package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

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

    /**
     * Keeps [layout], and moves [bounds] with it while an overlay follows the anchor.
     */
    fun moved(layout: LayoutCoordinates) {
        this.layout = layout
        host?.let { measure(it) }
    }

    /**
     * Starts following the anchor in [host] as an overlay opens on it, and stops with null.
     */
    fun follow(host: OverlayHostState?) {
        this.host = host
        host?.let { measure(it) }
    }

    private fun measure(host: OverlayHostState) {
        val attached = layout?.takeIf { it.isAttached } ?: return
        bounds = host.boundsOf(attached)
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
 * Lays [content] out where the placement puts it. A provider picks the spot from the anchor and
 * flips at the host's edges, and without one [content] covers the anchor's bounds exactly.
 */
@Composable
internal fun AnchoredOverlay(
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
