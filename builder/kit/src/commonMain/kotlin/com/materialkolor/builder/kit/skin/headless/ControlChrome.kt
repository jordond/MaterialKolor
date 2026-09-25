package com.materialkolor.builder.kit.skin.headless

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.composeunstyled.outline
import com.materialkolor.builder.kit.a11y.collectIsFocusVisibleAsState
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/** How faint a disabled control draws, container and ink together. */
internal const val DisabledAlpha: Float = 0.38f

/** Full opacity, or [DisabledAlpha] when the control is disabled. */
internal fun enabledAlpha(enabled: Boolean): Float = if (enabled) 1f else DisabledAlpha

/** How thick the keyboard focus ring is in every skin. */
internal val FocusRingWidth: Dp = 2.dp

/** How far the focus ring stands off the control, so it never sits on the control's own outline. */
internal val FocusRingOffset: Dp = 2.dp

/**
 * Shrinks the control while it is pressed, by the skin's press scale over the skin's press timing
 * (MO-06). Reduced motion sets the scale to one, so nothing moves. A disabled control keeps its size.
 */
@Composable
internal fun Modifier.controlPress(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier {
    val motion = LocalBuilderMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed && enabled) motion.pressScale else 1f,
        animationSpec = motion.press(),
        label = "press",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Draws the focus ring in [color] around [shape] while the control has keyboard focus (AR-01). It
 * stands [offset] off the control, [FocusRingOffset] unless the control asks for less. A negative
 * offset draws the ring inside the control, for one whose parent clips at its edge.
 *
 * Focus a pointer press leaves behind draws no ring, the way a browser's `:focus-visible` works,
 * and the ring comes back with the next key press (D58).
 */
@Composable
internal fun Modifier.controlRing(
    interactionSource: InteractionSource,
    shape: Shape,
    color: Color = LocalBuilderTokens.current.focus,
    offset: Dp = FocusRingOffset,
): Modifier {
    // b-513
    val shown by interactionSource.collectIsFocusVisibleAsState()
    return if (shown) outline(width = FocusRingWidth, color = color, shape = shape, offset = offset) else this
}

/**
 * Grows the space the control takes to at least [size] on each side and centres the control in it.
 *
 * Put it after the modifier that takes the input, so a press anywhere in the grown box counts, and
 * before the modifiers that draw, so the control keeps its own size on screen (AR-04).
 */
internal fun Modifier.controlTouchTarget(size: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val least = size.roundToPx()
        val width = constraints.constrainWidth(maxOf(placeable.width, least))
        val height = constraints.constrainHeight(maxOf(placeable.height, least))
        layout(width, height) {
            placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
        }
    }
