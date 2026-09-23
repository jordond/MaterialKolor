package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.composeunstyled.Indicator
import com.composeunstyled.UnstyledProgress
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.rememberLoopPhase
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentProgress
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.ProgressStyle
import com.materialkolor.builder.kit.skin.headless.ProgressSweep
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.material.MaterialProgress
import kotlin.math.roundToInt

/**
 * A bar that shows how far along some work is, or only that it is running.
 *
 * It reads out as a progress bar named [label], with the percentage as its state when the amount
 * is known.
 *
 * @param[label] What is running, read out as the bar's name.
 * @param[modifier] Applied to the bar, which fills the width it is given.
 * @param[progress] How much is done from zero to one, or null while nobody can tell. Anything past
 * either end counts as that end, and NaN counts as nothing done.
 */
@Composable
public fun BuilderProgress(
    label: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val amount = progress?.let { value -> if (value.isNaN()) 0f else value.coerceIn(0f, 1f) }
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialProgress(label, modifier, amount)
        Library.Unstyled -> HeadlessProgress(label, UnstyledActionStyles.progress, modifier, amount)
        Library.Fluent -> FluentProgress(label, modifier, amount) // fluent-placeholder
        Library.Custom -> HeadlessProgress(label, CustomActionStyles.progress, modifier, amount)
    }
}

/** Names a progress bar and, when [progress] is known, states it as a whole percentage. */
internal fun SemanticsPropertyReceiver.progressLabel(
    label: String,
    progress: Float?,
) {
    contentDescription = label
    if (progress != null) stateDescription = "${(progress * 100).roundToInt()}%"
}

/** Where the sweep rests whenever it may not move, across the middle of the track. */
private const val StillPhase = 0.5f

/**
 * The phase of an indeterminate sweep, zero as it enters the track and one as it leaves.
 *
 * It runs on [rememberLoopPhase], so it holds still while motion is frozen or the tab is hidden.
 * Reduced motion holds it still as well (MO-10), resting on a partial bar so the work never looks
 * finished.
 */
@Composable
internal fun rememberSweepPhase(sweep: ProgressSweep): State<Float> =
    if (LocalReducedMotion.current) {
        rememberUpdatedState(StillPhase)
    } else {
        rememberLoopPhase(periodMillis = sweep.periodMillis, frozenPhase = StillPhase)
    }

/**
 * The stretch of the track the sweep covers at [phase], as fractions from the reading start. Either
 * end may hang off the track while the sweep enters or leaves.
 */
internal fun sweepSpan(
    phase: Float,
    fraction: Float,
): ClosedFloatingPointRange<Float> {
    val start = -fraction + (1f + fraction) * phase
    return start..(start + fraction)
}

/** A progress bar drawn from [style] over Compose Unstyled's progress. */
@Composable
internal fun HeadlessProgress(
    label: String,
    style: ProgressStyle,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val track = modifier
        .fillMaxWidth()
        .height(style.height)
        .semantics { progressLabel(label, progress) }
        .clip(style.shape)
        .background(style.track)
    if (progress != null) {
        UnstyledProgress(progress = progress, modifier = track) {
            Indicator(Modifier.background(style.indicator, style.shape))
        }
        return
    }
    val phase = rememberSweepPhase(style.sweep)
    UnstyledProgress(modifier = track) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val span = sweepSpan(phase.value, style.sweep.fraction)
                    val from = if (layoutDirection == LayoutDirection.Ltr) span.start else 1f - span.endInclusive
                    drawRect(
                        color = style.indicator,
                        topLeft = Offset(from * size.width, 0f),
                        size = Size(style.sweep.fraction * size.width, size.height),
                    )
                },
        )
    }
}
