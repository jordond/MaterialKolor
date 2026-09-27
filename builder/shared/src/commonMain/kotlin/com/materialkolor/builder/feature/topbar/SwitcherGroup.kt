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
import androidx.compose.ui.layout.Placeable
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
 *
 * @param[probe] Whether this group is only measured for its width and never placed. A probe skips
 *   the frame lines and stands the chip as tall as the switcher, which leaves its width as it was.
 *   Reading a line that a control reports from deep inside, as Material's outlined dropdown does,
 *   lays out that control's insides for the line. In a group that is never placed, Compose then
 *   marks those insides as placed under parents it never placed, and the next measure after a
 *   recomposition crashes the page on web with a LayoutNode that is not in the RectList.
 */
@Composable
internal fun SwitcherGroup(
    modifier: Modifier = Modifier,
    probe: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = if (probe) ProbeGroupPolicy else FramedGroupPolicy,
    )
}

/**
 * The policy for a group that shows, which stands the chip on the switcher's frame.
 */
private val FramedGroupPolicy: MeasurePolicy = SwitcherGroupPolicy(framed = true)

/**
 * The policy for a group that is only measured for its width, which never reads a frame line.
 */
private val ProbeGroupPolicy: MeasurePolicy = SwitcherGroupPolicy(framed = false)

private class SwitcherGroupPolicy(
    private val framed: Boolean,
) : MeasurePolicy {
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
        val frameTop = if (framed) switcher.lineOrNull(ControlFrameTop) ?: 0 else 0
        val frameBottom = if (framed) switcher.lineOrNull(ControlFrameBottom) ?: switcher.height else switcher.height
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

/**
 * Where this placeable reports [line], or null when it does not report it.
 */
private fun Placeable.lineOrNull(line: AlignmentLine): Int? =
    get(line).takeUnless { position -> position == AlignmentLine.Unspecified }
