package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.a11y.foldsValueIntoName
import com.materialkolor.builder.kit.a11y.progressRoleWord
import com.materialkolor.builder.kit.a11y.valueNodeName
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.rememberLoopPhase
import kotlin.math.abs
import kotlin.math.roundToInt

// The samples of the Expressive Feedback cards of ExpressiveCards in ExpressiveGallery.kt. None of
// them runs a clock of Material's own, which would ignore frozen motion, reduced motion and a hidden
// tab (MO-10). Material gives progress no disabled look.

/**
 * How far along the determinate samples are.
 */
private const val ExpressiveAmount = 0.6f

/**
 * How long the looping loading indicator takes to morph through its shapes and back.
 */
private const val LoadingLoopMillis = 2800

/**
 * Where the loop rests whenever it may not move, halfway through the shapes.
 */
private const val StillPhase = 0.25f

/**
 * A loading indicator that loops and one on its container that holds still.
 *
 * Material's own looping indicator runs a clock of its own. So the looping one here is Material's
 * determinate indicator, walked through its shapes and back on the builder's loop, which rests
 * while motion is frozen or reduced and while the tab is hidden. Its semantics are set once, a
 * progress bar with no amount, so a moving amount never rebuilds them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoadingIndicators() {
    val folds = foldsValueIntoName
    val roleWord = progressRoleWord
    val phase = rememberLoadingPhase()
    ProgressRow {
        Box(
            Modifier
                .previewRoles(ExpressiveComponent.LoadingIndicator)
                .clearAndSetSemantics {
                    valueNodeName("Loading trips", "", folds, roleWord)
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                },
        ) {
            LoadingIndicator(progress = { loadingShapes(phase.value) })
        }
        ContainedLoadingIndicator(
            progress = { ExpressiveAmount },
            modifier = Modifier
                .previewRoles(ExpressiveComponent.ContainedLoadingIndicator)
                .semantics { valueNodeName("Saving", percentOf(ExpressiveAmount), folds, roleWord) },
        )
    }
}

/**
 * A circular and a linear wavy indicator, their waves holding still.
 */
@Composable
internal fun WavyProgressIndicators() {
    val folds = foldsValueIntoName
    val roleWord = progressRoleWord
    val done = percentOf(ExpressiveAmount)
    ProgressRow {
        CircularWavyProgressIndicator(
            progress = { ExpressiveAmount },
            modifier = Modifier
                .previewRoles(ExpressiveComponent.CircularWavyProgressIndicator)
                .semantics { valueNodeName("Uploading", done, folds, roleWord) },
            waveSpeed = 0.dp,
        )
        LinearWavyProgressIndicator(
            progress = { ExpressiveAmount },
            modifier = Modifier
                .weight(1f)
                .previewRoles(ExpressiveComponent.LinearWavyProgressIndicator)
                .semantics { valueNodeName("Syncing", done, folds, roleWord) },
            waveSpeed = 0.dp,
        )
    }
}

@Composable
private fun ProgressRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SectionGap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * The phase of the looping loading indicator. It runs on the builder's loop, which rests while
 * motion is frozen or the tab is hidden, and reduced motion holds it still as well.
 */
@Composable
private fun rememberLoadingPhase(): State<Float> =
    if (LocalReducedMotion.current) {
        rememberUpdatedState(StillPhase)
    } else {
        rememberLoopPhase(periodMillis = LoadingLoopMillis, frozenPhase = StillPhase)
    }

/**
 * How far through its shapes the looping indicator is at [phase], out to the last and back.
 */
internal fun loadingShapes(phase: Float): Float = 1f - abs(2f * phase - 1f)

private fun percentOf(amount: Float): String = "${(amount * 100).roundToInt()}%"
