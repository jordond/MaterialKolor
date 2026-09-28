package com.materialkolor.builder.kit.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

private const val NanosPerMilli = 1_000_000L

/**
 * The one clock the builder is allowed to loop on.
 *
 * Returns a phase that walks from zero to one every [periodMillis] and starts over. Read it inside
 * a draw or `graphicsLayer` lambda so the loop costs a frame, not a recomposition.
 *
 * It holds still in two cases. Under [LocalMotionFrozen] it sits on [frozenPhase], which is what
 * makes a screenshot of a shimmering skeleton reproducible. While the tab is in the background it
 * keeps whatever phase it had and stops asking for frames, so a hidden builder burns nothing.
 *
 * @param[periodMillis] How long one trip from zero to one takes.
 * @param[frozenPhase] The phase to sit on when motion is frozen.
 */
@Composable
public fun rememberLoopPhase(
    periodMillis: Int,
    frozenPhase: Float = 0f,
): State<Float> {
    require(periodMillis > 0) { "A loop needs a period of at least one millisecond, got $periodMillis" }
    require(frozenPhase in 0f..1f) { "A phase runs from zero to one, got $frozenPhase" }

    val frozen = LocalMotionFrozen.current
    val visible = LocalTabVisible.current
    val phase = remember { mutableFloatStateOf(frozenPhase) }

    LaunchedEffect(frozen, visible, periodMillis, frozenPhase) {
        if (frozen) {
            phase.floatValue = frozenPhase
            return@LaunchedEffect
        }
        if (!visible) return@LaunchedEffect

        val period = periodMillis * NanosPerMilli
        // Null until the first frame. The start itself can be below zero, when the clock has run for
        // less than the phase the loop picks up from.
        var startNanos: Long? = null
        while (true) {
            withFrameNanos { frameNanos ->
                val start = startNanos ?: (frameNanos - (phase.floatValue * period).toLong()).also { first ->
                    startNanos = first
                }
                phase.floatValue = ((frameNanos - start) % period) / period.toFloat()
            }
        }
    }

    return phase
}
