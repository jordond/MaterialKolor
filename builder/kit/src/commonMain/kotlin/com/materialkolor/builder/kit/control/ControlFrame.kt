package com.materialkolor.builder.kit.control

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import kotlin.math.max
import kotlin.math.min

/**
 * How far below its own top edge a control's frame starts.
 *
 * A field that floats its label over its outline gives the label half its line above the frame, so
 * its outline starts lower than its bounds, and a button grown to its touch target stands inside
 * room it does not draw. Something drawn to line up with the frame reads the line to find it. A
 * control that sets no line starts its frame at its top.
 */
public val ControlFrameTop: HorizontalAlignmentLine = HorizontalAlignmentLine(::min)

/**
 * How far below its own top edge a control's frame ends, the partner of [ControlFrameTop]. A
 * control that sets no line ends its frame at its bottom.
 */
public val ControlFrameBottom: HorizontalAlignmentLine = HorizontalAlignmentLine(::max)

/**
 * Reports what this wraps as the frame, from its top to its bottom. Put it inside the modifiers
 * that grow a control past what it draws, such as its touch target, so the lines land on the drawn
 * edges once the parent reads them.
 */
internal fun Modifier.reportControlFrame(): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val lines = mapOf<AlignmentLine, Int>(ControlFrameTop to 0, ControlFrameBottom to placeable.height)
        layout(placeable.width, placeable.height, lines) { placeable.place(0, 0) }
    }
