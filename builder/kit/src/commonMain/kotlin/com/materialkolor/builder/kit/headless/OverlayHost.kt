package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
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
}

/** Where an anchored overlay goes, worked out by [provider] from the bounds [anchor] reports. */
internal class OverlayPlacement(
    val anchor: OverlayAnchor,
    val provider: PopupPositionProvider,
)

/** The bounds of the layout an overlay opens from, in the host's coordinates. */
internal class OverlayAnchor {
    var bounds: IntRect by mutableStateOf(IntRect.Zero)
}

/** One overlay in the host, drawn with the locals of the place it was opened from. */
internal class OverlayLayer(
    val kind: OverlayKind,
) {
    var context: CompositionLocalContext? by mutableStateOf(null)
    var content: @Composable () -> Unit by mutableStateOf({})
    var placement: OverlayPlacement? by mutableStateOf(null)
    var onDismissRequest: (() -> Unit)? by mutableStateOf(null)
}

/** The overlays open over the page, in the order they opened, the last one on top. */
internal class OverlayHostState {
    val layers: SnapshotStateList<OverlayLayer> = mutableStateListOf()

    /** The host's own layout, which anchored overlays measure their anchors against. */
    var coordinates: LayoutCoordinates? = null

    /** Whether a modal sits over the layer at [index], or over the page for [Page]. */
    fun isUnderModal(index: Int): Boolean =
        layers.withIndex().any { (i, layer) -> i > index && layer.kind == OverlayKind.Modal }

    /** Whether a modal or a popover above the layer at [index] keeps the keyboard to itself. */
    fun isUnderFocusTrap(index: Int): Boolean =
        layers.withIndex().any { (i, layer) -> i > index && layer.kind != OverlayKind.Passive }

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
 * The host overlays open into when they render in the page, or null when they open windows.
 *
 * Both the switch and a host are needed, so an overlay outside `BuilderTheme` still opens.
 */
@Composable
internal fun inTreeOverlayHost(): OverlayHostState? =
    if (LocalOverlaysInTree.current) LocalOverlayHost.current else null

/**
 * Hosts the overlays over [content] when they render in the page.
 *
 * It sits once at the root, inside `BuilderTheme`. A nested theme reuses the host it is already under,
 * and with the switch off the host steps aside and [content] is laid out as it would be without it.
 * While a modal is open the page and every layer under it leave the Tab order and the semantics
 * tree (AR-11), and they come back once it closes. A popover keeps Tab to itself the same way but
 * leaves the page readable.
 */
@Composable
internal fun OverlayHost(content: @Composable () -> Unit) {
    if (!LocalOverlaysInTree.current || LocalOverlayHost.current != null) {
        content()
        return
    }
    val host = remember { OverlayHostState() }
    CompositionLocalProvider(LocalOverlayHost provides host) {
        Box(
            modifier = Modifier.onPlaced { coordinates -> host.coordinates = coordinates },
            propagateMinConstraints = true,
        ) {
            Box(
                modifier = Modifier.shutOut(host, OverlayHostState.Page),
                propagateMinConstraints = true,
            ) { content() }
            OverlayLayers(host, Modifier.matchParentSize())
        }
    }
}

/**
 * Renders [content] into the host as an overlay of [kind] for as long as this stays composed.
 *
 * The locals at the call site travel with it, so an overlay opened inside a poster, a preview pane
 * or a right to left subtree keeps its tokens, skin, layout direction and density. With a
 * [placement] the host lays [content] out against the anchor, and a popover closes through
 * [onDismissRequest] on Esc and on a press outside it.
 */
@Composable
internal fun OverlayPortal(
    host: OverlayHostState,
    kind: OverlayKind,
    placement: OverlayPlacement? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = currentCompositionLocalContext
    val layer = remember(host, kind) { OverlayLayer(kind) }
    layer.context = context
    layer.content = content
    layer.placement = placement
    layer.onDismissRequest = onDismissRequest
    DisposableEffect(host, layer) {
        host.layers.add(layer)
        onDispose { host.layers.remove(layer) }
    }
}

/**
 * Reports the bounds of the layout this is composed in, for an overlay anchored to it.
 *
 * It lays out as nothing, so the anchor keeps its size, the way a `Popup` does.
 */
@Composable
internal fun rememberOverlayAnchor(host: OverlayHostState): OverlayAnchor {
    val anchor = remember { OverlayAnchor() }
    Layout(
        modifier = Modifier.onGloballyPositioned { probe ->
            anchor.bounds = host.boundsOf(probe.parentLayoutCoordinates ?: probe)
        },
    ) { _, _ -> layout(0, 0) {} }
    return anchor
}

@Composable
private fun OverlayLayers(
    host: OverlayHostState,
    modifier: Modifier,
) {
    host.layers.forEachIndexed { index, layer ->
        key(layer) { OverlayLayerContent(host, layer, index, modifier) }
    }
}

@Composable
private fun OverlayLayerContent(
    host: OverlayHostState,
    layer: OverlayLayer,
    index: Int,
    modifier: Modifier,
) {
    val context = layer.context ?: return
    val dismiss = layer.onDismissRequest
    Box(
        modifier = modifier
            .shutOut(host, index)
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

/** Keeps focus and assistive technology out of the page or the layer at [index] while it is covered. */
private fun Modifier.shutOut(
    host: OverlayHostState,
    index: Int,
): Modifier =
    focusProperties { onEnter = { if (host.isUnderFocusTrap(index)) cancelFocusChange() } }
        .focusGroup()
        .then(if (host.isUnderModal(index)) Modifier.clearAndSetSemantics {} else Modifier)

/** Lays [content] out at the spot the placement's provider picks, flipping at the host's edges. */
@Composable
private fun AnchoredOverlay(
    placement: OverlayPlacement,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val window = IntSize(constraints.maxWidth, constraints.maxHeight)
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable -> measurable.measure(loose) }
        layout(window.width, window.height) {
            for (placeable in placeables) {
                val size = IntSize(placeable.width, placeable.height)
                val anchor = placement.anchor.bounds
                placeable.place(placement.provider.calculatePosition(anchor, window, layoutDirection, size))
            }
        }
    }
}
