package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.composeunstyled.Indicator
import com.composeunstyled.UnstyledProgress
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.rememberLoopPhase
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentProgress
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.ProgressStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.material.MaterialProgress
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A bar that shows how far along some work is, or only that it is running.
 *
 * It reads out as a progress bar named [label], with the percentage as its state when the amount
 * is known.
 *
 * @param[label] What is running, read out as the bar's name.
 * @param[modifier] Applied to the bar, which fills the width it is given.
 * @param[progress] How much is done from zero to one, or null while nobody can tell.
 */
@Composable
public fun BuilderProgress(
    label: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val amount = progress?.coerceIn(0f, 1f)
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

/** How long the indeterminate sweep takes to cross the track once. */
private const val SweepMillis = 1400

/** How much of the track the indeterminate sweep covers. */
private const val SweepFraction = 0.4f

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
    val phase = rememberLoopPhase(periodMillis = SweepMillis, frozenPhase = 0.5f)
    val reducedMotion = LocalReducedMotion.current
    UnstyledProgress(modifier = track) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val now = phase.value
                    if (reducedMotion) {
                        // Nothing slides under reduced motion, the whole bar breathes instead.
                        drawRect(style.indicator, alpha = 0.4f + 0.6f * (1f - abs(2f * now - 1f)))
                    } else {
                        val sweep = size.width * SweepFraction
                        val start = -sweep + (size.width + sweep) * now
                        drawRect(style.indicator, topLeft = Offset(start, 0f), size = Size(sweep, size.height))
                    }
                },
        )
    }
}
