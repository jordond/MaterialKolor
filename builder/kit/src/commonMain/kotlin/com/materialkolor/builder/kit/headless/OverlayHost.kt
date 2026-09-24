package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.LocalSkin
import kotlinx.coroutines.flow.drop

/**
 * Whether overlays draw inside the page rather than in a popup or dialog window of their own (D40).
 *
 * The web accessibility mirror in CMP 1.12.1 follows a single semantics owner. Every `Popup` and
 * `Dialog` brings an owner of its own and takes the whole mirror over, and once it closes the mirror
 * stays frozen on it for the rest of the session. So on wasm every kit overlay renders into the
 * nearest [OverlayHost] above it, the builder's at the root or a preview pane's own. The switch stays
 * so the windows can come back once CMP fixes the listener.
 */
internal expect val overlaysInTree: Boolean

/** [overlaysInTree] as a local, so a test can run every overlay both ways on the JVM. */
internal val LocalOverlaysInTree: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { overlaysInTree }

/** The host the overlays under it render into, null where overlays open windows of their own. */
internal val LocalOverlayHost: ProvidableCompositionLocal<OverlayHostState?> =
    staticCompositionLocalOf { null }

/** How an overlay treats what lies under it. */
internal enum class OverlayKind {
    /** A dialog, sheet or side panel. What lies under it leaves the Tab order and the semantics tree. */
    Modal,

    /** A menu or the list of a select. Tab stays inside it, and what lies under it can still be read. */
    Popover,

    /** A tooltip. It never takes focus and leaves what lies under it as it is. */
    Passive,

    /**
     * The toasts. They sit in the host's top slot over every other layer, and no modal hides them.
     * Tab reaches them from an open modal, so a toast's action joins its cycle, and only an open
     * popover keeps Tab away from them.
     */
    Top,
}

/**
 * Where an anchored overlay goes. [provider] places it from the bounds [anchor] reports, flipping at
 * the host's edges, and without a provider it covers the anchor's own bounds.
 */
internal class OverlayPlacement(
    val anchor: OverlayAnchor,
    val provider: PopupPositionProvider?,
)

/** One overlay in the host, drawn with the locals of the place it was opened from. */
internal class OverlayLayer(
    val kind: OverlayKind,
) {
    var context: CompositionLocalContext? by mutableStateOf(null)
    var content: @Composable () -> Unit by mutableStateOf({})
    var placement: OverlayPlacement? by mutableStateOf(null)
    var onDismissRequest: (() -> Unit)? by mutableStateOf(null)

    /**
     * False once the overlay has been asked to close. It lets go of the keyboard and of what lies
     * under it then, while it may still be animating out.
     */
    var open: Boolean by mutableStateOf(true)

    /** Whether focus is inside the layer. */
    var hasFocus: Boolean = false

    /** The panel drawn in the layer that takes focus as it opens, which the host can lead focus back into. */
    var focus: OverlayFocus? = null
}

/** The layer the overlay drawn here sits in, so its [OverlayFocus] can offer itself to the host. */
internal val LocalOverlayLayer: ProvidableCompositionLocal<OverlayLayer?> =
    staticCompositionLocalOf { null }

/** The overlays open over the page, in the order they opened, the last one on top. */
internal class OverlayHostState {
    val layers: SnapshotStateList<OverlayLayer> = mutableStateListOf()

    /** The top slot over every layer, where the toasts go. */
    val top: SnapshotStateList<OverlayLayer> = mutableStateListOf()

    /** The host's own layout, which anchored overlays measure their anchors against. */
    var coordinates: LayoutCoordinates? = null

    /** The page's focus group, which notes the child that held focus each time focus leaves the page. */
    val page: FocusRequester = FocusRequester()

    /**
     * Every focus target one level into the page. A scroll container or a lazy list is one of them,
     * since each brings a focus group of its own, and each notes the child in it that held focus.
     */
    val pageChildren: FocusRequester = FocusRequester()

    /** Whether focus is in the page. */
    var pageHasFocus: Boolean = false

    /** Whether an open modal sits over the layer at [index], or over the page for [Page]. */
    fun isUnderModal(index: Int): Boolean =
        layers.withIndex().any { (i, layer) -> i > index && layer.open && layer.kind == OverlayKind.Modal }

    /** Whether an open modal or popover above the layer at [index] keeps the keyboard to itself. */
    fun isUnderFocusTrap(index: Int): Boolean =
        layers.withIndex().any { (i, layer) -> i > index && layer.open && layer.kind != OverlayKind.Passive }

    /**
     * Whether an open popover keeps the keyboard away from the top slot. A modal does not, so Undo on
     * a toast raised from a dialog stays one Tab away.
     */
    fun isTopUnderFocusTrap(): Boolean = layers.any { layer -> layer.open && layer.kind == OverlayKind.Popover }

    /** Whether focus rests in the page or in a layer, rather than nowhere after a layer left with it. */
    fun holdsFocus(): Boolean = pageHasFocus || layers.any { it.hasFocus } || top.any { it.hasFocus }

    /** Goes up each time focus leaves the top slot, so the host can look where it went once it settles. */
    var topFocusLosses: Int by mutableIntStateOf(0)
        private set

    /** Notes that focus left the top slot, a toast's Undo that closed the toast under it included. */
    fun topLostFocus() {
        topFocusLosses++
    }

    /** Notes the control in the page that holds focus, two focus groups deep, as focus leaves the page. */
    fun savePageFocus() {
        page.saveFocusedChild()
        pageChildren.saveFocusedChild()
    }

    /**
     * Leads focus back when it rests nowhere after a toast that held it left. With a modal open it
     * goes into the top one, the way the modal took it as it opened, so the keyboard stays inside
     * the modal.
     *
     * Otherwise it goes back to the control in the page that had it before the toast took it. That
     * reaches a control that sits straight in the page or straight in a scroll container, a lazy list
     * or another focus group in the page. Compose only notes the focused child where a focus
     * requester or a focus restorer sits, while `ComposeUiFlags.isFocusRestorationEnabled` is off, so
     * a control nested a group deeper is not found and focus stays where the toast left it.
     */
    fun refocus() {
        if (holdsFocus()) return
        val modal = layers.lastOrNull { layer -> layer.open && layer.kind == OverlayKind.Modal }
        if (modal != null) modal.focus?.enter() else page.restoreFocusedChild()
    }

    /** [layout]'s bounds in the host, or in the root before the host has been placed. */
    fun boundsOf(layout: LayoutCoordinates): IntRect {
        val host = coordinates?.takeIf { it.isAttached }
        val topLeft = host?.localPositionOf(layout, Offset.Zero) ?: layout.positionInRoot()
        return IntRect(topLeft.round(), layout.size)
    }

    companion object {
        /** The index [isUnderModal] and [isUnderFocusTrap] take for the page itself. */
        const val Page: Int = -1
    }
}

/**
 * The host an overlay opening here renders into, or null where overlays open windows of their own.
 *
 * Ask only once the overlay opens. With the switch on and no host it fails, the way a missing
 * `LocalLayout` does, since a window would take the web mirror over (D40). A closed anchor never
 * asks, so offscreen and preview compositions without a host still compose.
 */
@Composable
internal fun inTreeOverlayHost(): OverlayHostState? {
    if (!LocalOverlaysInTree.current) return null
    return LocalOverlayHost.current ?: error("No OverlayHost provided, open overlays inside BuilderTheme")
}

/** The host overlays render into here, or null, without failing when there is none. */
@Composable
internal fun currentOverlayHost(): OverlayHostState? =
    if (LocalOverlaysInTree.current) LocalOverlayHost.current else null

/**
 * The library a control that may hold a popup or dialog window draws itself in.
 *
 * `BuilderTheme` carries its content across a skin switch, so a `BoxWithConstraints` in it composes
 * its content again while it is being measured. A window closed there crashes the desktop scene,
 * which is still laying the window out. So where overlays open windows the library
 * follows the skin one composition late, and the old window closes in a composition of its own.
 * On desktop that means the control draws one frame in the old library after a switch. In the page
 * the library follows the skin at once.
 */
@Composable
internal fun overlayLibrary(): Library {
    val library = LocalSkin.current.library
    if (LocalOverlaysInTree.current) return library
    val settled = remember { mutableStateOf(library) }
    SideEffect { settled.value = library }
    return settled.value
}

/**
 * Hosts the overlays over [content] when they render in the page.
 *
 * It sits once at the root, inside `BuilderTheme`. A nested theme reuses the host it is already under,
 * and with the switch off the host steps aside and [content] is laid out as it would be without it.
 * While a modal is open the page and every layer under it leave the Tab order and the semantics
 * tree (AR-11), and they come back once it closes. A popover keeps Tab to itself the same way but
 * leaves the page readable. The toasts in the top slot stay readable under both, and Tab reaches
 * them from a modal but not from a popover.
 *
 * A [nested] host is a pane's own. It never reuses the host above it, and it clips its overlays to
 * the bounds of [content], the pane's, so they stay inside the pane's clip and filters, and a modal
 * in it clears only the pane.
 *
 * Text fields get no context menu here, since foundation draws it in a popup and its rows are not
 * public. A right press never reaches them. A touch selection gets the page's own text toolbar
 * instead, which the root host provides and draws last, over every layer and the top slot. A
 * [nested] host passes the toolbar of the host above it through, and one with no host above it
 * shows none.
 */
@Composable
internal fun OverlayHost(
    nested: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!LocalOverlaysInTree.current || (!nested && LocalOverlayHost.current != null)) {
        content()
        return
    }
    val parent = LocalOverlayHost.current
    val host = remember { OverlayHostState() }
    val toolbar = if (nested) null else remember { PageTextToolbar() }
    LaunchedEffect(host) {
        snapshotFlow { host.topFocusLosses }.drop(1).collect { host.refocus() }
    }
    CompositionLocalProvider(
        LocalOverlayHost provides host,
        LocalTextContextMenuDropdownProvider provides NoTextContextMenu,
        LocalTextContextMenuToolbarProvider provides NoTextContextMenu,
        LocalTextToolbar provides when {
            toolbar != null -> toolbar
            parent != null -> LocalTextToolbar.current
            else -> NoTextToolbar
        },
    ) {
        Box(
            modifier = Modifier
                .then(if (nested) Modifier.clipToBounds() else Modifier)
                .onPlaced { coordinates -> host.coordinates = coordinates }
                .pointerInput(Unit) { swallowSecondaryPresses() }
                .then(if (toolbar != null) Modifier.pageTextToolbarScroll(toolbar) else Modifier),
            propagateMinConstraints = true,
        ) {
            Box(
                modifier = Modifier
                    .onFocusChanged { state -> host.pageHasFocus = state.hasFocus }
                    .focusRequester(host.page)
                    .focusProperties { onExit = { host.savePageFocus() } }
                    .trapFocus { host.isUnderFocusTrap(OverlayHostState.Page) }
                    .focusRequester(host.pageChildren)
                    .hideUnderModal(host, OverlayHostState.Page),
                propagateMinConstraints = true,
            ) { content() }
            OverlayLayers(host, Modifier.matchParentSize())
            if (toolbar != null) PageTextToolbarLayer(toolbar, host, Modifier.matchParentSize())
        }
    }
}

/**
 * Renders [content] into the host as [layer] for as long as this stays composed.
 *
 * The locals at the call site travel with it, so an overlay opened inside a poster, a preview pane
 * or a right to left subtree keeps its tokens, skin, layout direction and density. With a
 * [placement] the host lays [content] out against the anchor, and a popover closes through
 * [onDismissRequest] on Esc and on a press outside it. Once [open] goes false the layer lets go of
 * the keyboard and of what lies under it, while [content] animates out.
 */
@Composable
internal fun OverlayPortal(
    host: OverlayHostState,
    layer: OverlayLayer,
    open: Boolean,
    placement: OverlayPlacement? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = currentCompositionLocalContext
    layer.context = context
    layer.content = content
    layer.placement = placement
    layer.onDismissRequest = onDismissRequest
    layer.open = open
    DisposableEffect(host, layer) {
        val stack = if (layer.kind == OverlayKind.Top) host.top else host.layers
        stack.add(layer)
        onDispose {
            stack.remove(layer)
            // A backstop, since a layer that leaves along with its portal may not report the focus it held.
            if (layer.kind == OverlayKind.Top && layer.hasFocus) host.topLostFocus()
        }
    }
    val anchor = placement?.anchor
    DisposableEffect(host, anchor) {
        anchor?.follow(host)
        onDispose { anchor?.follow(null) }
    }
}

/**
 * Lays [content] out over the region [modifier] gives it.
 *
 * Where overlays render in the page it is drawn in the host's top slot over every layer, so an open
 * modal never hides it, and the region stays empty in the page. Anywhere else, the host missing
 * included, it is drawn in place.
 */
@Composable
internal fun OverlayTopSlot(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val host = currentOverlayHost()
    if (host == null) {
        Box(modifier, propagateMinConstraints = true) { content() }
        return
    }
    val anchor = remember { OverlayAnchor() }
    Box(modifier.onGloballyPositioned { region -> anchor.moved(region) })
    val layer = remember { OverlayLayer(OverlayKind.Top) }
    val placement = remember(anchor) { OverlayPlacement(anchor, provider = null) }
    OverlayPortal(host, layer, open = true, placement = placement, content = content)
}

@Composable
private fun OverlayLayers(
    host: OverlayHostState,
    modifier: Modifier,
) {
    host.layers.forEachIndexed { index, layer ->
        key(layer) {
            OverlayLayerContent(
                host,
                layer,
                modifier.trapFocus { host.isUnderFocusTrap(index) }.hideUnderModal(host, index),
            )
        }
    }
    for (layer in host.top) {
        key(layer) { OverlayLayerContent(host, layer, modifier.trapFocus { host.isTopUnderFocusTrap() }) }
    }
}

@Composable
private fun OverlayLayerContent(
    host: OverlayHostState,
    layer: OverlayLayer,
    modifier: Modifier,
) {
    val context = layer.context ?: return
    val dismiss = layer.onDismissRequest
    Box(
        modifier = Modifier
            .onFocusChanged { state ->
                val letGo = layer.hasFocus && !state.hasFocus
                layer.hasFocus = state.hasFocus
                if (letGo && layer.kind == OverlayKind.Top) host.topLostFocus()
            }.then(modifier)
            .then(if (dismiss != null) Modifier.dismissOnEscape(layer) else Modifier),
    ) {
        if (dismiss != null) {
            Box(Modifier.fillMaxSize().pointerInput(layer) { awaitEachGesture { dismissOnPress(layer) } })
        }
        CompositionLocalProvider(context) {
            CompositionLocalProvider(LocalOverlayLayer provides layer) {
                val placement = layer.placement
                if (placement == null) layer.content() else AnchoredOverlay(placement, layer.content)
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.dismissOnPress(layer: OverlayLayer) {
    awaitFirstDown(requireUnconsumed = false)
    layer.onDismissRequest?.invoke()
}

private fun Modifier.dismissOnEscape(layer: OverlayLayer): Modifier =
    onKeyEvent { event ->
        val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
        if (escape) layer.onDismissRequest?.invoke()
        escape
    }

/** Keeps focus out of the page, a layer or the top slot while [trapped] says an overlay over it holds it. */
private fun Modifier.trapFocus(trapped: () -> Boolean): Modifier =
    focusProperties { onEnter = { if (trapped()) cancelFocusChange() } }.focusGroup()

/**
 * Hides the page or the layer at [index] from assistive technology while an open modal covers it.
 * Built during composition, so the host recomposes as modals open and close.
 */
private fun Modifier.hideUnderModal(
    host: OverlayHostState,
    index: Int,
): Modifier = then(if (host.isUnderModal(index)) Modifier.clearAndSetSemantics {} else Modifier)
