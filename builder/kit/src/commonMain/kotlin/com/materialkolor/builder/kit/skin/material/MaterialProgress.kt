package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.progressLabel
import com.materialkolor.builder.kit.control.rememberSweepPhase
import com.materialkolor.builder.kit.control.sweepSpan
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.ActionSweep

/**
 * Material's linear bar, or in the expressive flavour Material's wavy one ([ExpressiveProgress]).
 *
 * Material's own indeterminate bar runs a clock of its own, which ignores frozen motion, reduced
 * motion and a hidden tab. So the indeterminate bar is Material's determinate one held at
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
    if (LocalSkin.current.expressive) {
        ExpressiveProgress(labelled, progress)
        return
    }
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

/**
 * Material's expressive bar, the wavy one, whose wave rises while the work is under way.
 *
 * The wave stands still. A moving wave runs a clock of Material's own, which ignores frozen motion,
 * reduced motion and a hidden tab like the linear bar's. Material's bar also reads its amount
 * inside its semantics, so an amount that moved every frame would rebuild them every frame. While
 * nobody can tell how far along the work is, the bar is drawn here instead, the sweep from
 * [rememberSweepPhase] between two stretches of track, in Material's strokes and gaps, under
 * semantics that stay fixed as a bar with no amount.
 */
@Composable
private fun ExpressiveProgress(
    labelled: Modifier,
    progress: Float?,
) {
    if (progress != null) {
        LinearWavyProgressIndicator(progress = { progress }, modifier = labelled, waveSpeed = 0.dp)
        return
    }
    val phase = rememberSweepPhase(ActionSweep)
    val sweep = ExpressiveSweep(
        color = WavyProgressIndicatorDefaults.indicatorColor,
        trackColor = WavyProgressIndicatorDefaults.trackColor,
        stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
        trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
        gap = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    )
    Spacer(
        labelled
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .height(WavyProgressIndicatorDefaults.LinearContainerHeight)
            .drawBehind { drawExpressiveSweep(phase.value, sweep) },
    )
}

/**
 * How Material's expressive bar draws, its line and its track and the gap between them.
 */
private class ExpressiveSweep(
    val color: Color,
    val trackColor: Color,
    val stroke: Stroke,
    val trackStroke: Stroke,
    val gap: Dp,
)

/**
 * Draws the sweep at [phase] the way Material draws its expressive bar with no wave, the line with a
 * gap on either side and the track over the rest of the width.
 */
private fun DrawScope.drawExpressiveSweep(
    phase: Float,
    sweep: ExpressiveSweep,
) {
    val span = sweepSpan(phase, ActionSweep.fraction)
    val start = span.start.coerceIn(0f, 1f) * size.width
    val end = span.endInclusive.coerceIn(0f, 1f) * size.width
    if (end <= start) {
        drawStretch(0f, size.width, sweep.trackColor, sweep.trackStroke)
        return
    }
    val gap = sweep.gap.toPx()
    drawStretch(0f, start - gap, sweep.trackColor, sweep.trackStroke)
    drawStretch(start, end, sweep.color, sweep.stroke)
    drawStretch(end + gap, size.width, sweep.trackColor, sweep.trackStroke)
}

/**
 * Draws a line across the middle from [from] to [to], measured from the reading start, with its
 * caps inside that stretch. A stretch too short for its caps is left out.
 */
private fun DrawScope.drawStretch(
    from: Float,
    to: Float,
    color: Color,
    stroke: Stroke,
) {
    // A round cap reaches half the stroke past each end, so pull the ends in to keep it in the stretch.
    val cap = if (stroke.cap == StrokeCap.Butt) 0f else stroke.width / 2
    val first = from + cap
    val last = to - cap
    if (last < first) return
    val ltr = layoutDirection == LayoutDirection.Ltr
    val y = size.height / 2
    drawLine(
        color = color,
        start = Offset(if (ltr) first else size.width - first, y),
        end = Offset(if (ltr) last else size.width - last, y),
        strokeWidth = stroke.width,
        cap = stroke.cap,
    )
}

/**
 * Draws the sweep at [phase] the way Material draws its own bar, a line as thick as the track.
 */
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
