package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.rememberLoopPhase
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlin.math.abs

/** How long a skeleton chip takes to pulse out to the strong border tone and back. */
private const val SkeletonPulseMillis = 1_200

/** Where the pulse rests whenever it may not move, halfway between its two tones. */
private const val SkeletonStillPhase = 0.25f

/**
 * How much room a [SchemeChip] takes, its circle with the ring and the focus outline around it.
 * Anything set beside the chips, such as the picture they came from, can take the same room so
 * nothing moves when the chips come in.
 */
public val SchemeChipFootprint: Dp =
    SchemeChipDiameter + (SchemeChipRingGap + SchemeChipRingWidth + WidgetFocusWidth) * 2

/**
 * Stands where a [SchemeChip] will be while its colors are still on their way.
 *
 * It takes the chip's room and draws the chip's circle pulsing between the two border tones, so
 * nothing moves when the real chip replaces it. The pulse runs on [rememberLoopPhase], so it holds
 * still while motion is frozen or the tab is hidden, and reduced motion holds it still too (MO-10).
 *
 * It takes no focus and reads as nothing. Whatever waits on it says what is loading, once for the
 * whole group.
 *
 * @param[modifier] Applied to the skeleton.
 */
@Composable
public fun SchemeChipSkeleton(modifier: Modifier = Modifier) {
    val tokens = LocalBuilderTokens.current
    val phase = rememberSkeletonPhase()
    val inset = (SchemeChipFootprint - SchemeChipDiameter) / 2
    Box(
        modifier
            .size(SchemeChipFootprint)
            .padding(inset)
            .drawBehind {
                val pulse = 1f - abs(phase.value * 2f - 1f)
                drawCircle(lerp(tokens.border, tokens.borderStrong, pulse))
            },
    )
}

/** The phase of the skeleton's pulse, still under reduced motion. */
@Composable
private fun rememberSkeletonPhase(): State<Float> =
    if (LocalReducedMotion.current) {
        rememberUpdatedState(SkeletonStillPhase)
    } else {
        rememberLoopPhase(periodMillis = SkeletonPulseMillis, frozenPhase = SkeletonStillPhase)
    }
