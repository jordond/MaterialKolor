package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.BottomSheetState

/**
 * The room the shell leaves its overlays, noted as the shell lays out.
 *
 * The shell writes it while it measures and [ToastRegion] reads it while it measures, so the rail
 * animating only lays the toasts out again and never recomposes them.
 */
@Stable
internal class ShellRoom {
    /**
     * Where toasts stack, in pixels from the shell's top start corner. It is the canvas frame past
     * the poster as drawn and above the dock, or on a phone the width of the shell above the poster
     * peek and the dock.
     */
    var toasts: IntRect by mutableStateOf(IntRect.Zero)
}

/** The room of the shell its overlays are drawn over, null outside a `WorkspaceShell`. */
internal val LocalShellRoom: ProvidableCompositionLocal<ShellRoom?> =
    staticCompositionLocalOf { null }

/**
 * How far the toast room stands in from the shell's edges.
 *
 * @property[start] From the start edge, read at layout time so a rail on the move is followed.
 * @property[end] From the end edge.
 * @property[bottom] From the bottom edge, which clears the dock.
 */
internal class ToastInsets(
    val start: () -> Dp,
    val end: Dp,
    val bottom: Dp,
)

/**
 * Lays the content out in [room]'s toast room. The region this lands on has to fill the shell, and
 * it keeps the whole size it is given.
 */
internal fun Modifier.inToastRoom(room: ShellRoom): Modifier =
    layout { measurable, constraints ->
        val rect = room.toasts
        val placeable = measurable.measure(Constraints.fixed(rect.width.coerceAtLeast(0), rect.height.coerceAtLeast(0)))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(rect.left, rect.top) }
    }

/**
 * Takes everything inside out of the Tab order while [covered] says the poster sheet lies over it,
 * and puts it back once the sheet sinks. Focus asks [covered] each time Tab or Shift+Tab would
 * enter, so nothing recomposes as the sheet moves. A click or a focus request still gets in, since
 * what shows of the region stays usable. The semantics stay as they are.
 *
 * @param[covered] Whether the sheet lies over the region right now.
 * @param[focused] Told whether focus is inside the region each time that changes.
 */
internal fun Modifier.skipTabWhile(
    covered: () -> Boolean,
    focused: (inside: Boolean) -> Unit,
): Modifier =
    onFocusChanged { state -> focused(state.hasFocus) }
        .focusProperties {
            onEnter = {
                val direction = requestedFocusDirection
                val tab = direction == FocusDirection.Next || direction == FocusDirection.Previous
                if (tab && covered()) cancelFocusChange()
            }
        }.focusGroup()

/**
 * What the phone layout notes about the regions its poster sheet can lie over. The fields are
 * plain, since only focus and the sheet's detent read them, so nothing recomposes as they change.
 */
internal class SheetCover {
    /** Whether focus is in the top bar. */
    var topBarFocused: Boolean = false

    /** Whether focus is in the canvas. */
    var canvasFocused: Boolean = false

    /** Whether focus is in the dock. */
    var dockFocused: Boolean = false

    /** The dock's top edge in root pixels as last placed, NaN before that. */
    var dockTop: Float = Float.NaN

    /** The top edge of the frame the sheet slides in, in root pixels as last placed, NaN before that. */
    var sheetTop: Float = Float.NaN

    /** Whether [sheet] resting at [detent] lies over the whole dock, as the two were last placed. */
    fun dockUnder(
        sheet: BottomSheetState,
        detent: BottomSheetDetent,
    ): Boolean {
        val top = sheetTop + sheet.draggable.anchors.positionOf(detent)
        return !top.isNaN() && !dockTop.isNaN() && dockTop >= top
    }

    /** Whether focus is in a region the sheet lies over at [detent], the dock from [dockFrom] up. */
    fun focusUnder(
        detent: BottomSheetDetent,
        dockFrom: BottomSheetDetent,
    ): Boolean =
        (topBarFocused && detent >= BottomSheetDetent.Full) ||
            (canvasFocused && detent >= BottomSheetDetent.Half) ||
            (dockFocused && detent >= dockFrom)
}
