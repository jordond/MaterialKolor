package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.headless.menusOpenAsWindows
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.BuilderType
import com.materialkolor.builder.kit.token.LocalBuilderType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// b-406

/**
 * The top bar buttons a Medium window moves into the overflow menu when the library dropdown needs
 * their room, in the order they go. History goes first, since its list opens under the overflow
 * button just as well, and Undo is the last to leave, since it gets the most use.
 */
internal val MediumOverflowOrder: List<TopBarControl> =
    listOf(TopBarControl.History, TopBarControl.Commands, TopBarControl.Redo, TopBarControl.Undo) // b-509

/**
 * How many of [MediumOverflowOrder] the Medium top bar has moved into its overflow menu, so the
 * library dropdown shows the longest library name whole.
 *
 * The dropdown tells it what it needs and what room it got each time it measures. Short of room,
 * the next button goes, and the room that frees is remembered, so a button only comes back once
 * the dropdown would still fit with it there. That keeps a bar at the edge from swapping a button
 * in and out on every frame.
 */
@Stable
internal class MediumBarFit {
    /** How many buttons sit in the overflow, from the start of [MediumOverflowOrder]. */
    var moved: Int by mutableIntStateOf(0)
        private set

    /** The room each move freed, measured once the bar had caught up with it. Plain, nothing draws from it. */
    private val freed = IntArray(MediumOverflowOrder.size)

    /** The room the dropdown had just before the last move, until the next measure prices the move. */
    private var roomBeforeMove: Int? = null

    /** The buttons in the overflow right now. */
    val overflowed: Set<TopBarControl>
        get() = MediumOverflowOrder.take(moved).toSet()

    /**
     * Moves a button out or back for a dropdown that needs [needed] and got [room], both in pixels,
     * measured in a bar composed with [shownMoved] buttons moved. A measure from a bar that has not
     * caught up with the last move yet changes nothing.
     */
    fun refit(
        needed: Int,
        room: Int,
        shownMoved: Int,
    ) {
        if (shownMoved != moved) return
        roomBeforeMove?.let { before ->
            freed[moved - 1] = (room - before).coerceAtLeast(1)
            roomBeforeMove = null
        }
        if (needed > room && moved < MediumOverflowOrder.size) {
            roomBeforeMove = room
            moved++
        } else if (moved > 0 && room - needed >= freed[moved - 1]) {
            moved--
        }
    }
}

/**
 * The library switcher on a Medium window, a dropdown whose trigger shows the library's name whole.
 *
 * It measures every library's trigger off screen and out of the accessibility tree, and asks [fit]
 * for room for the widest, so a switch never moves the bar's buttons. Which one is widest is only
 * worked out again when the skin, the type or the names change. The form goes to [LocalSwitcherForm]
 * for the command registry. A pick reaches [onSwitch] at once where the menu opens in the page, and
 * once the menu has left where it opens as a window of its own.
 *
 * @param[selected] The library the document is on.
 * @param[fit] Which top bar buttons have made room for it.
 * @param[modifier] Applied to the room the dropdown gets, which it fills.
 * @param[switcherModifier] Applied to the dropdown itself.
 */
@Composable
internal fun LibraryDropdown(
    selected: LibraryChoice,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    fit: MediumBarFit,
    modifier: Modifier = Modifier,
    switcherModifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    val type = LocalBuilderType.current
    val labels = LibraryChoice.entries.map { choice -> libraryName(choice) }
    val report = LocalSwitcherForm.current
    val shownMoved = fit.moved
    val widest = remember { WidestTrigger() }
    if (report != null) SideEffect { report.segmented = false }
    // b-406g
    // Where the menu is a window of its own, on the desktop, the pick waits for it to finish leaving,
    // since a skin switch that closes the window while the bar is being measured crashes the scene.
    // In the page, on the web, the pick switches at once.
    val scope = rememberCoroutineScope()
    val switch by rememberUpdatedState(onSwitch)
    val windows = menusOpenAsWindows
    val pick: (LibraryChoice, Offset) -> Unit = remember(scope, windows) {
        { choice, origin ->
            if (windows) {
                scope.launch {
                    awaitMenuExit()
                    switch(choice, origin)
                }
            } else {
                switch(choice, origin)
            }
        }
    }
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val key = TriggerKey(skin, type, labels, density, fontScale)
        val widestChoice = widest.of(key) {
            LibraryChoice.entries.maxBy { choice -> naturalWidth(TriggerSlot.Probe(choice), choice) }
        }
        // The widest is measured on every pass, so a font that loads after the pick above still counts.
        val needed = naturalWidth(TriggerSlot.Widest, widestChoice)
        if (constraints.hasBoundedWidth) fit.refit(needed, constraints.maxWidth, shownMoved)
        val shown = subcompose(TriggerSlot.Shown) {
            LibrarySwitcher(
                selected = selected,
                onSwitch = pick, // b-406g
                modifier = switcherModifier,
                segmented = false,
            )
        }.map { measurable -> measurable.measure(loose) }
        val natural = shown.maxOfOrNull { placeable -> placeable.width } ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else natural
        val height = maxOf(constraints.minHeight, shown.maxOfOrNull { placeable -> placeable.height } ?: 0)
        layout(width, height) {
            shown.forEach { placeable -> placeable.placeRelative(0, (height - placeable.height) / 2) }
        }
    }
}

/**
 * How wide the trigger for [choice] is when nothing holds it in, composed in [slot] off screen and
 * out of the accessibility tree, so only the trigger that shows can be reached.
 */
private fun SubcomposeMeasureScope.naturalWidth(
    slot: TriggerSlot,
    choice: LibraryChoice,
): Int {
    val probe = subcompose(slot) {
        LibrarySwitcher(
            selected = choice,
            onSwitch = { _, _ -> },
            modifier = Modifier.clearAndSetSemantics {},
            segmented = false,
        )
    }
    // Bounded, since a row gives a weighted label no room at all when its own room has no end.
    val room = Constraints(maxWidth = PROBE_MAX_WIDTH)
    return probe.maxOfOrNull { measurable -> measurable.measure(room).width } ?: 0
}

// b-512

/**
 * Draws what it holds scaled down from its top start corner, just enough to stand no taller than
 * [max], and takes up only the scaled size. Something that fits already is left as it is. Presses
 * and the menu anchored to it follow the scale.
 *
 * It keeps Material's outlined dropdown, whose label floats over its top edge, whole inside the top
 * bar, where the field and its label together stand taller than the bar.
 */
internal fun Modifier.shrinkToHeight(max: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val limit = max.roundToPx()
        if (placeable.height <= limit) {
            return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        val scale = limit.toFloat() / placeable.height
        layout((placeable.width * scale).roundToInt(), limit) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }

// b-406g

/** How long a menu window takes to leave, in frame time, with room to spare for Material's springs. */
private const val MENU_EXIT_NANOS: Long = 250_000_000L

/** Waits out [MENU_EXIT_NANOS] of frames, so the menu a pick came from is gone. */
private suspend fun awaitMenuExit() {
    val start = withFrameNanos { frame -> frame }
    var now = start
    while (now - start < MENU_EXIT_NANOS) now = withFrameNanos { frame -> frame }
}

/** The room a trigger is measured in to find its own width, far more than any window gives it. */
private const val PROBE_MAX_WIDTH: Int = 32_767

/** Everything the widest trigger hangs on, the skin and type it draws in and the names it shows. */
private data class TriggerKey(
    val skin: Skin,
    val type: BuilderType,
    val labels: List<String>,
    val density: Float,
    val fontScale: Float,
)

/** Which library's trigger came out widest last. Plain fields, since only the dropdown's measure reads them. */
private class WidestTrigger {
    private var key: TriggerKey? = null
    private var widest: LibraryChoice = LibraryChoice.M3

    fun of(
        key: TriggerKey,
        pick: () -> LibraryChoice,
    ): LibraryChoice {
        if (key != this.key) {
            widest = pick()
            this.key = key
        }
        return widest
    }
}

/**
 * The dropdown's slots, the one that shows, the widest measured each pass, and one off screen
 * trigger for each library to find the widest.
 */
private sealed interface TriggerSlot {
    data object Shown : TriggerSlot

    data object Widest : TriggerSlot

    data class Probe(
        val choice: LibraryChoice,
    ) : TriggerSlot
}
