package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Whether overlays draw inside the page rather than in a popup or dialog window of their own (D40).
 *
 * The web accessibility mirror in CMP 1.12.1 follows a single semantics owner. Every `Popup` and
 * `Dialog` brings an owner of its own and takes the whole mirror over, and once it closes the mirror
 * stays frozen on it for the rest of the session. So on wasm every kit overlay renders into the
 * [OverlayHost] at the root of the builder. The switch stays so the windows can come back once CMP
 * fixes the listener.
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

    /** The toasts. They sit in the host's top slot over every other layer, and no modal hides them. */
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

/**
 * The bounds of the layout an overlay opens from, in the host's coordinates.
 *
 * A closed anchor only keeps its layout, so scrolling past it writes no state. The bounds follow
 * the layout while an overlay is open on it.
 */
internal class OverlayAnchor {
    var bounds: IntRect by mutableStateOf(IntRect.Zero)
        private set

    private var layout: LayoutCoordinates? = null
    private var host: OverlayHostState? = null

    /** Keeps [layout], and moves [bounds] with it while an overlay follows the anchor. */
    fun moved(layout: LayoutCoordinates) {
        this.layout = layout
        host?.let { measure(it) }
    }

    /** Starts following the anchor in [host] as an overlay opens on it, and stops with null. */
    fun follow(host: OverlayHostState?) {
        this.host = host
        host?.let { measure(it) }
    }

    private fun measure(host: OverlayHostState) {
        val attached = layout?.takeIf { it.isAttached } ?: return
        bounds = host.boundsOf(attached)
    }
}

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
}

/** The overlays open over the page, in the order they opened, the last one on top. */
internal class OverlayHostState {
    val layers: SnapshotStateList<OverlayLayer> = mutableStateListOf()

    /** The top slot over every layer, where the toasts go. */
    val top: SnapshotStateList<OverlayLayer> = mutableStateListOf()

    /** The host's own layout, which anchored overlays measure their anchors against. */
    var coordinates: LayoutCoordinates? = null

    /** Whether focus is in the page. */
    var pageHasFocus: Boolean = false

    /** Whether an open modal sits over the layer at [index], or over the page for [Page]. */
    fun isUnderModal(index: Int): Boolean =
        layers.withIndex().any { (i, layer) -> i > index && layer.open && layer.kind == OverlayKind.Modal }

    /** Whether an open modal or popover above the layer at [index] keeps the keyboard to itself. */
    fun isUnderFocusTrap(index: Int): Boolean =
        layers.withIndex().any { (i, layer) -> i > index && layer.open && layer.kind != OverlayKind.Passive }

    /** Whether focus rests in the page or in a layer, rather than nowhere after a layer left with it. */
    fun holdsFocus(): Boolean = pageHasFocus || layers.any { it.hasFocus } || top.any { it.hasFocus }

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
 * Hosts the overlays over [content] when they render in the page.
 *
 * It sits once at the root, inside `BuilderTheme`. A nested theme reuses the host it is already under,
 * and with the switch off the host steps aside and [content] is laid out as it would be without it.
 * While a modal is open the page and every layer under it leave the Tab order and the semantics
 * tree (AR-11), and they come back once it closes. A popover keeps Tab to itself the same way but
 * leaves the page readable. The toasts in the top slot stay readable under both.
 *
 * Text fields get no context menu or selection toolbar here. Foundation draws both in a popup, and
 * the rows it offers are not public, so they cannot be drawn in the page instead.
 */
@Composable
internal fun OverlayHost(content: @Composable () -> Unit) {
    if (!LocalOverlaysInTree.current || LocalOverlayHost.current != null) {
        content()
        return
    }
    val host = remember { OverlayHostState() }
    CompositionLocalProvider(
        LocalOverlayHost provides host,
        LocalTextContextMenuDropdownProvider provides NoTextContextMenu,
        LocalTextContextMenuToolbarProvider provides NoTextContextMenu,
        LocalTextToolbar provides NoTextToolbar,
    ) {
        Box(
            modifier = Modifier
                .onPlaced { coordinates -> host.coordinates = coordinates }
                .pointerInput(Unit) { swallowSecondaryPresses() },
            propagateMinConstraints = true,
        ) {
            Box(
                modifier = Modifier
                    .onFocusChanged { state -> host.pageHasFocus = state.hasFocus }
                    .trapFocus(host, OverlayHostState.Page)
                    .hideUnderModal(host, OverlayHostState.Page),
                propagateMinConstraints = true,
            ) { content() }
            OverlayLayers(host, Modifier.matchParentSize())
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
        onDispose { stack.remove(layer) }
    }
    val anchor = placement?.anchor
    DisposableEffect(host, anchor) {
        anchor?.follow(host)
        onDispose { anchor?.follow(null) }
    }
}

/**
 * Keeps the layout this is composed in as an anchor for an overlay.
 *
 * It lays out as nothing, so the anchor keeps its size, the way a `Popup` does.
 */
@Composable
internal fun rememberOverlayAnchor(): OverlayAnchor {
    val anchor = remember { OverlayAnchor() }
    Layout(
        modifier = Modifier.onGloballyPositioned { probe -> anchor.moved(probe.parentLayoutCoordinates ?: probe) },
    ) { _, _ -> layout(0, 0) {} }
    return anchor
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

/**
 * Where focus lands as an overlay opens, so it is always inside an open modal or popover.
 *
 * It goes to the first thing in the panel that takes it. When nothing does, a menu of disabled rows
 * or a dialog of text alone, the panel takes it itself, so Tab and Esc reach the overlay rather than
 * the page under it.
 */
internal class OverlayFocus {
    private val panel = FocusRequester()
    private val inside = FocusRequester()
    private var panelTakesFocus by mutableStateOf(false)

    /** Goes on the panel, around what it holds. */
    val modifier: Modifier = Modifier
        .focusRequester(panel)
        .focusProperties { canFocus = panelTakesFocus }
        .focusTarget()
        .focusRequester(inside)

    /** Focuses [first], or else the first thing in the panel in reading order, or else the panel. */
    fun enter(first: FocusRequester? = null) {
        if (first?.requestFocus() == true || inside.requestFocus()) return
        panelTakesFocus = true
        panel.requestFocus()
    }
}

@Composable
private fun OverlayLayers(
    host: OverlayHostState,
    modifier: Modifier,
) {
    host.layers.forEachIndexed { index, layer ->
        key(layer) {
            OverlayLayerContent(layer, modifier.trapFocus(host, index).hideUnderModal(host, index))
        }
    }
    for (layer in host.top) {
        key(layer) { OverlayLayerContent(layer, modifier.trapFocus(host, OverlayHostState.Page)) }
    }
}

@Composable
private fun OverlayLayerContent(
    layer: OverlayLayer,
    modifier: Modifier,
) {
    val context = layer.context ?: return
    val dismiss = layer.onDismissRequest
    Box(
        modifier = Modifier
            .onFocusChanged { state -> layer.hasFocus = state.hasFocus }
            .then(modifier)
            .then(if (dismiss != null) Modifier.dismissOnEscape(layer) else Modifier),
    ) {
        if (dismiss != null) {
            Box(Modifier.fillMaxSize().pointerInput(layer) { awaitEachGesture { dismissOnPress(layer) } })
        }
        CompositionLocalProvider(context) {
            val placement = layer.placement
            if (placement == null) layer.content() else AnchoredOverlay(placement, layer.content)
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

/** Keeps focus out of the page or the layer at [index] while an open modal or popover covers it. */
private fun Modifier.trapFocus(
    host: OverlayHostState,
    index: Int,
): Modifier = focusProperties { onEnter = { if (host.isUnderFocusTrap(index)) cancelFocusChange() } }.focusGroup()

/**
 * Hides the page or the layer at [index] from assistive technology while an open modal covers it.
 * Built during composition, so the host recomposes as modals open and close.
 */
private fun Modifier.hideUnderModal(
    host: OverlayHostState,
    index: Int,
): Modifier = then(if (host.isUnderModal(index)) Modifier.clearAndSetSemantics {} else Modifier)

/**
 * Swallows a secondary press before a text field sees it, since foundation's text context menu is
 * a popup. Nothing else in the builder answers one.
 */
private suspend fun PointerInputScope.swallowSecondaryPresses() {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.buttons.isSecondaryPressed) {
                event.changes.forEach { change -> if (change.changedToDownIgnoreConsumed()) change.consume() }
            }
        }
    }
}

/** A text context menu that shows nothing, standing in for foundation's popup. */
private object NoTextContextMenu : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) = Unit
}

/** A text selection toolbar that shows nothing, standing in for the web's popup. */
private object NoTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}

/**
 * Lays [content] out where the placement puts it. A provider picks the spot from the anchor and
 * flips at the host's edges, and without one [content] covers the anchor's bounds exactly.
 */
@Composable
private fun AnchoredOverlay(
    placement: OverlayPlacement,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val window = IntSize(constraints.maxWidth, constraints.maxHeight)
        val anchor = placement.anchor.bounds
        val provider = placement.provider
        val inner = if (provider == null) {
            Constraints.fixed(anchor.width, anchor.height)
        } else {
            constraints.copy(minWidth = 0, minHeight = 0)
        }
        val placeables = measurables.map { measurable -> measurable.measure(inner) }
        layout(window.width, window.height) {
            for (placeable in placeables) {
                val size = IntSize(placeable.width, placeable.height)
                placeable.place(provider?.calculatePosition(anchor, window, layoutDirection, size) ?: anchor.topLeft)
            }
        }
    }
}
