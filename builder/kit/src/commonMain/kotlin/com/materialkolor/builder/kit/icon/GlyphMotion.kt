package com.materialkolor.builder.kit.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.rememberLoopPhase

/**
 * How long [IconId.Progress] takes to go round once.
 */
private const val ProgressTurnMillis = 1_200

private const val FullTurn = 360f

/**
 * Turns the [IconId.Progress] glyph round its centre, so it reads as work under way. Every other
 * glyph stands still.
 *
 * Under reduced motion it stands still too and asks for no frames. It turns on the builder's loop
 * clock, so it also holds still when motion is frozen and stops while the tab is hidden.
 */
@Composable
internal fun Modifier.glyphMotion(id: IconId): Modifier {
    if (id != IconId.Progress || LocalReducedMotion.current) return this
    val phase = rememberLoopPhase(periodMillis = ProgressTurnMillis)
    return graphicsLayer { rotationZ = phase.value * FullTurn }
}
