package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Immutable
internal data class ControlState(
    val isHovered: Boolean,
    val isPressed: Boolean,
    val isFocused: Boolean,
)

@Composable
internal fun InteractionSource.collectControlState(): ControlState {
    val isHovered by collectIsHoveredAsState()
    val isPressed by collectIsPressedAsState()
    val isFocused by collectIsFocusedAsState()
    return ControlState(isHovered = isHovered, isPressed = isPressed, isFocused = isFocused)
}

/**
 * How far a control's second ink pass sits from its first. Hovering knocks it further off register, and pressing
 * squeezes the passes back into line, the way a print press would.
 */
@Composable
internal fun ControlState.animateShift(rest: DpOffset): DpOffset {
    val scale = when {
        isPressed -> 0f
        isHovered -> HOVER_SHIFT_SCALE
        else -> 1f
    }
    val x by animateDpAsState(targetValue = rest.x * scale, animationSpec = spring())
    val y by animateDpAsState(targetValue = rest.y * scale, animationSpec = spring())
    return DpOffset(x, y)
}

internal fun Modifier.focusRing(
    state: ControlState,
    color: Color,
    shape: Shape,
): Modifier = if (state.isFocused) border(Rule, color, shape) else this

internal val ControlShift: DpOffset = DpOffset(5.dp, 5.dp)

private const val HOVER_SHIFT_SCALE = 1.6f
