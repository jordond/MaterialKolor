package com.materialkolor.builder.kit.headless

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf

/**
 * Where focus lands as an overlay opens, so it is always inside an open modal or popover.
 *
 * It goes to the first thing in the panel that takes it. When nothing does, a menu of disabled rows
 * or a dialog of text alone, the panel takes it itself, so Tab and Esc reach the overlay rather than
 * the page under it. When something inside has already taken focus, a field that asks for it as it
 * opens, focus stays where it is.
 *
 * In the overlay host the panel offers itself to the layer it is drawn in, so the host can lead focus
 * back in the same way once a toast that held it has gone.
 */
internal class OverlayFocus {
    private val panel = FocusRequester()
    private val inside = FocusRequester()
    private var panelTakesFocus by mutableStateOf(false)

    /** Whether focus rests on the panel or on anything inside it. */
    private var hasFocus = false

    /** Goes on the panel, around what it holds. */
    val modifier: Modifier = Modifier
        .then(OfferToLayerElement(this))
        .focusRequester(panel)
        .onFocusChanged { state -> hasFocus = state.hasFocus }
        .focusProperties { canFocus = panelTakesFocus }
        .focusTarget()
        .focusRequester(inside)

    /**
     * Focuses [first], or else the first thing in the panel in reading order, or else the panel.
     * It does nothing while focus is already inside.
     */
    fun enter(first: FocusRequester? = null) {
        if (hasFocus) return
        if (first?.requestFocus() == true || inside.requestFocus()) return
        panelTakesFocus = true
        panel.requestFocus()
    }
}

/** Offers [focus] to the host layer the panel is drawn in, for as long as the panel is there. */
private class OfferToLayerElement(
    private val focus: OverlayFocus,
) : ModifierNodeElement<OfferToLayerNode>() {
    override fun create(): OfferToLayerNode = OfferToLayerNode(focus)

    override fun update(node: OfferToLayerNode) {
        node.offer(focus)
    }

    override fun equals(other: Any?): Boolean = other is OfferToLayerElement && other.focus === focus

    override fun hashCode(): Int = focus.hashCode()
}

private class OfferToLayerNode(
    private var focus: OverlayFocus,
) : Modifier.Node(),
    CompositionLocalConsumerModifierNode {
    private var layer: OverlayLayer? = null

    fun offer(next: OverlayFocus) {
        withdraw()
        focus = next
        if (isAttached) offer()
    }

    override fun onAttach() {
        offer()
    }

    override fun onDetach() {
        withdraw()
    }

    private fun offer() {
        layer = currentValueOf(LocalOverlayLayer)?.also { it.focus = focus }
    }

    private fun withdraw() {
        layer?.let { if (it.focus === focus) it.focus = null }
        layer = null
    }
}
