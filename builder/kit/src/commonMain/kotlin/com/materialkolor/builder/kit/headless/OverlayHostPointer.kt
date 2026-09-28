package com.materialkolor.builder.kit.headless

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize

/**
 * Lets the pointer reach what lies under a tooltip's layer as well as the tooltip, so a wheel turned
 * over the label still scrolls the page.
 *
 * Compose asks only the layer's own modifiers whether it shares, never the label deep inside it, so
 * the flag sits on the layer and does nothing else. The label keeps a press to itself on its own.
 */
internal fun Modifier.shareThePointer(): Modifier = this then SharePointerElement

private data object SharePointerElement : ModifierNodeElement<SharePointerNode>() {
    override fun create(): SharePointerNode = SharePointerNode()

    override fun update(node: SharePointerNode) = Unit
}

private class SharePointerNode :
    Modifier.Node(),
    PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) = Unit

    override fun onCancelPointerInput() = Unit

    override fun sharePointerInputWithSiblings(): Boolean = true
}
