package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.control.progressLabel
import com.materialkolor.builder.kit.control.rememberSweepPhase
import com.materialkolor.builder.kit.control.sweepSpan
import com.materialkolor.builder.kit.skin.headless.ActionSweep

/**
 * Material's linear bar. B-402 decides whether the expressive flavour gets the loading indicator.
 *
 * Material's own indeterminate bar runs a clock of its own, which ignores frozen motion, reduced
 * motion and a hidden tab (MO-10). So the indeterminate bar is Material's determinate one held at
 * zero, which draws the whole track, with the sweep drawn from [rememberSweepPhase] in the slot
 * Material leaves for its stop indicator.
 */
@Composable
internal fun MaterialProgress(
    label: String,
    modifier: Modifier,
    progress: Float?,
) {
    val labelled = modifier
        .fillMaxWidth()
        .progressLabel(label, progress)
    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = labelled)
        return
    }
    val phase = rememberSweepPhase(ActionSweep)
    val color = ProgressIndicatorDefaults.linearColor
    val cap = ProgressIndicatorDefaults.LinearStrokeCap
    LinearProgressIndicator(
        progress = { 0f },
        modifier = labelled.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        color = color,
        strokeCap = cap,
        drawStopIndicator = { drawMaterialSweep(phase.value, color, cap) },
    )
}

/** Draws the sweep at [phase] the way Material draws its own bar, a line as thick as the track. */
private fun DrawScope.drawMaterialSweep(
    phase: Float,
    color: Color,
    cap: StrokeCap,
) {
    val span = sweepSpan(phase, ActionSweep.fraction)
    val start = span.start.coerceIn(0f, 1f)
    val end = span.endInclusive.coerceIn(0f, 1f)
    if (end <= start) return
    val ltr = layoutDirection == LayoutDirection.Ltr
    val from = (if (ltr) start else 1f - end) * size.width
    val to = (if (ltr) end else 1f - start) * size.width
    // A round cap reaches half the stroke past each end, so pull the ends in to keep it on the track.
    val inset = if (cap == StrokeCap.Butt) 0f else size.height / 2
    val y = size.height / 2
    drawLine(
        color = color,
        start = Offset(from.coerceIn(inset, size.width - inset), y),
        end = Offset(to.coerceIn(inset, size.width - inset), y),
        strokeWidth = size.height,
        cap = cap,
    )
}
