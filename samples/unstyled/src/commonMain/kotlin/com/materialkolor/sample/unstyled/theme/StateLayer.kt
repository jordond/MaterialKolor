package com.materialkolor.sample.unstyled.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import com.composeunstyled.LocalContentColor
import kotlinx.coroutines.launch

private const val HOVERED_ALPHA = 0.08f
private const val PRESSED_ALPHA = 0.12f

/**
 * The theme's default indication. It lays the content color over a hovered or pressed control.
 */
internal data object StateLayer : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = StateLayerNode(interactionSource)
}

private class StateLayerNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var hovered = false
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            val hovers = mutableListOf<HoverInteraction.Enter>()
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> hovers += interaction
                    is HoverInteraction.Exit -> hovers -= interaction.enter
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                hovered = hovers.isNotEmpty()
                pressed = presses.isNotEmpty()
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = when {
            pressed -> PRESSED_ALPHA
            hovered -> HOVERED_ALPHA
            else -> return
        }
        val color = currentValueOf(LocalContentColor)
        if (color.isSpecified) drawRect(color.copy(alpha = alpha))
    }
}
