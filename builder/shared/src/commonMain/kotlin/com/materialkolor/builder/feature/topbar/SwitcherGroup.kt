package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.materialkolor.builder.kit.control.ControlFrameBottom
import com.materialkolor.builder.kit.control.ControlFrameTop

/**
 * The library switcher and, when [content] holds a second control, the Expressive chip joined to
 * its end edge with no gap, so the two read as one group.
 *
 * The chip keeps its whole width and the switcher takes what is left, so a Medium bar short of room
 * narrows the dropdown rather than cutting the chip's name. The chip stands as tall as the
 * switcher's frame, from the [ControlFrameTop] to the [ControlFrameBottom] the switcher reports, so
 * it lines up with an outlined dropdown whose label floats above the outline as well as with a
 * segmented row whose buttons stand inside a larger touch target.
 */
@Composable
internal fun SwitcherGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier, measurePolicy = SwitcherGroupPolicy)
}

private object SwitcherGroupPolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val chip = measurables.getOrNull(1)
        val chipRoom = chip?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
        val chipWidth = if (loose.hasBoundedWidth) chipRoom.coerceAtMost(loose.maxWidth) else chipRoom
        val switcherRoom = if (loose.hasBoundedWidth) {
            loose.copy(maxWidth = (loose.maxWidth - chipWidth).coerceAtLeast(0))
        } else {
            loose
        }
        val switcher = measurables.first().measure(switcherRoom)
        val frameTop = switcher[ControlFrameTop].takeUnless { top -> top == AlignmentLine.Unspecified } ?: 0
        val frameBottom = switcher[ControlFrameBottom].takeUnless { bottom -> bottom == AlignmentLine.Unspecified }
            ?: switcher.height
        val frameHeight = (frameBottom - frameTop).coerceAtLeast(0)
        val joined = chip?.measure(
            Constraints(minWidth = 0, maxWidth = chipWidth, minHeight = frameHeight, maxHeight = frameHeight),
        )
        val width = constraints.constrainWidth(switcher.width + (joined?.width ?: 0))
        val height = constraints.constrainHeight(switcher.height)
        val top = (height - switcher.height) / 2
        return layout(width, height) {
            switcher.placeRelative(0, top)
            joined?.placeRelative(switcher.width, top + frameTop)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.sumOf { measurable -> measurable.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.sumOf { measurable -> measurable.minIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.firstOrNull()?.maxIntrinsicHeight(width) ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.firstOrNull()?.minIntrinsicHeight(width) ?: 0
}
