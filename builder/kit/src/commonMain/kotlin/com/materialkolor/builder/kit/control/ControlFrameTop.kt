package com.materialkolor.builder.kit.control

import androidx.compose.ui.layout.HorizontalAlignmentLine
import kotlin.math.min

/**
 * How far below its own top edge a control's frame starts.
 *
 * A field that floats its label over its outline gives the label half its line above the frame, so
 * its outline starts lower than its bounds. Something drawn to line up with that outline reads the
 * line to find it. A control that sets no line starts its frame at its top.
 */
public val ControlFrameTop: HorizontalAlignmentLine = HorizontalAlignmentLine(::min)
