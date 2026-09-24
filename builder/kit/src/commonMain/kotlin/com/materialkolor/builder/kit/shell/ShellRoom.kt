package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect

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
 * and puts it back once the sheet sinks. Focus asks [covered] each time it would enter, so nothing
 * recomposes as the sheet moves. The semantics stay as they are.
 */
internal fun Modifier.skipTabWhile(covered: () -> Boolean): Modifier =
    focusProperties { onEnter = { if (covered()) cancelFocusChange() } }.focusGroup()
