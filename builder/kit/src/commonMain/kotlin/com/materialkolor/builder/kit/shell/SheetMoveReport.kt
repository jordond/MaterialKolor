package com.materialkolor.builder.kit.shell

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.materialkolor.builder.kit.control.BottomSheetState

// b-406g

/**
 * Reports the poster sheet's rise under a drag on its content as scrolled, so the web keeps the
 * finger rather than handing the drag to the browser.
 *
 * The sheet rises before its content scrolls, taking each step of a drag as it comes in, and what
 * the sheet takes then never shows as scrolled to what lies above it. On the web the page reads a
 * drag that scrolled nothing as one for the browser, which takes the finger over at the first step
 * and cancels the drag, so the sheet neither rose nor scrolled. While a step moves the sheet and
 * scrolls nothing else, this reports the sheet's move as scrolled. It goes on the sheet, above the
 * sheet's own share of its content's drags, so it sees where the sheet stood before each step.
 *
 * @param[sheet] The sheet whose moves it reports.
 */
internal class SheetMoveReport(
    private val sheet: BottomSheetState,
) : NestedScrollConnection {
    /**
     * Where the sheet stood as the step in flight began, NaN between steps.
     */
    private var before = Float.NaN

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        before = sheet.draggable.offset
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val moved = sheet.draggable.offset - before
        before = Float.NaN
        val unseen = source == NestedScrollSource.UserInput && consumed == Offset.Zero && !moved.isNaN() && moved != 0f
        return if (unseen) Offset(0f, moved) else Offset.Zero
    }
}
