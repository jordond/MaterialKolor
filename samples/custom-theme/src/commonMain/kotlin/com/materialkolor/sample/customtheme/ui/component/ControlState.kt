package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp

/**
 * What the pointer and the keyboard are doing to one control right now.
 *
 * The components pass `indication = null` and draw their own feedback from this, so hover, press and focus look the
 * same everywhere and come from the theme.
 *
 * @property[isHovered] The pointer is over the control.
 * @property[isPressed] The control is held down.
 * @property[isFocused] The control has keyboard focus.
 */
@Immutable
internal data class ControlState(
    val isHovered: Boolean,
    val isPressed: Boolean,
    val isFocused: Boolean,
) {
    /** How strongly to tint the control, more when pressed than when hovered. */
    val veilAlpha: Float
        get() = when {
            isPressed -> PRESSED_ALPHA
            isHovered -> HOVERED_ALPHA
            else -> 0f
        }
}

/**
 * Collects the [ControlState] of whatever emits into this source.
 */
@Composable
internal fun InteractionSource.collectControlState(): ControlState {
    val isHovered by collectIsHoveredAsState()
    val isPressed by collectIsPressedAsState()
    val isFocused by collectIsFocusedAsState()
    return ControlState(isHovered = isHovered, isPressed = isPressed, isFocused = isFocused)
}

/**
 * Paints a see-through layer of [tint] over the background while [state] is hovered or pressed.
 */
internal fun Modifier.veil(
    tint: Color,
    state: ControlState,
): Modifier = if (state.veilAlpha > 0f) background(tint.copy(alpha = state.veilAlpha)) else this

/**
 * Outlines the control in [color] while [state] has keyboard focus.
 */
internal fun Modifier.focusRing(
    state: ControlState,
    color: Color,
    shape: Shape,
): Modifier = if (state.isFocused) border(2.dp, color, shape) else this

private const val HOVERED_ALPHA = 0.08f
private const val PRESSED_ALPHA = 0.14f
