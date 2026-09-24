package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.focusRing
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

internal const val DISABLED_ALPHA = 0.38f

private const val PRESSED_SCALE = 0.96f

internal fun enabledAlpha(enabled: Boolean): Float = if (enabled) 1f else DISABLED_ALPHA

@Composable
internal fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) PRESSED_SCALE else 1f, label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A focus ring for keyboard focus only. A click leaves none, since the app sits in a `FocusVisibilityProvider`.
 *
 * @param[offset] How far the ring stands off the control, so it never sits on the control's own outline.
 */
@Composable
internal fun Modifier.controlFocusRing(
    interactionSource: InteractionSource,
    shape: Shape,
    offset: Dp = 2.dp,
): Modifier =
    focusRing(
        interactionSource = interactionSource,
        width = 2.dp,
        color = MaterialKolorTokens.primary.color,
        shape = shape,
        offset = offset,
    )
