package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * What a demo screen remembers, shared by both copies of a split so they always agree.
 *
 * The canvas owns one and hands it to both copies. Flip a switch on the dark side and it flips on
 * the light side too. Scroll is the exception, one list state cannot drive two lists, so each copy
 * asks [rememberListState] for its own and the copies mirror each other.
 */
@Stable
public class DemoAppState {
    /** The item the screen has open or highlighted. */
    public var selectedItem: Int by mutableIntStateOf(0)

    /** The tab the screen shows. */
    public var tabIndex: Int by mutableIntStateOf(0)

    /** What the screen's text field holds. */
    public var text: String by mutableStateOf("")

    private val switches = mutableStateMapOf<String, Boolean>()
    private val checkboxes = mutableStateMapOf<String, Boolean>()
    private val mirrors = mutableMapOf<String, ScrollMirror>()

    /** Whether the switch called [switch] is on, off until someone flips it. */
    public fun isOn(switch: String): Boolean = switches[switch] ?: false

    /** Turn the switch called [switch] on or off. */
    public fun setOn(
        switch: String,
        on: Boolean,
    ) {
        switches[switch] = on
    }

    /** Whether the checkbox called [checkbox] is checked, unchecked until someone ticks it. */
    public fun isChecked(checkbox: String): Boolean = checkboxes[checkbox] ?: false

    /** Tick or clear the checkbox called [checkbox]. */
    public fun setChecked(
        checkbox: String,
        checked: Boolean,
    ) {
        checkboxes[checkbox] = checked
    }

    /**
     * A list state for the list called [key] in this copy, kept in step with the same list in the
     * other copy.
     *
     * The copy that last scrolled on its own leads, and every other copy follows it with
     * `scrollToItem`. A copy that joins later opens where the leader is.
     */
    @Composable
    public fun rememberListState(key: String): LazyListState {
        val mirror = remember(key) { mirrors.getOrPut(key) { ScrollMirror() } }
        val list = remember(mirror) {
            val lead = mirror.lead
            if (lead == null) LazyListState() else LazyListState(lead.position.index, lead.position.offset)
        }
        LaunchedEffect(mirror, list) { mirror.follow(list) }
        return list
    }
}

/** Where a list is scrolled to. */
private data class ListPosition(
    val index: Int,
    val offset: Int,
)

/** The list that scrolled last and where it got to. */
private class ScrollLead(
    val source: LazyListState,
    val position: ListPosition,
)

/** Keeps every copy of one list at the position of the copy that scrolled last. */
@Stable
private class ScrollMirror {
    var lead: ScrollLead? by mutableStateOf(null)

    /**
     * Lead whenever [list] moves on its own, and follow whenever another copy leads.
     *
     * A move that lands where the mirror just sent [list] is its own echo, not a scroll, so it does
     * not take the lead back.
     */
    suspend fun follow(list: LazyListState) {
        var expected = list.position()
        coroutineScope {
            launch {
                snapshotFlow { list.position() }.collect { position ->
                    if (position != expected) {
                        expected = position
                        lead = ScrollLead(list, position)
                    }
                }
            }
            snapshotFlow { lead }.collect { current ->
                if (current != null && current.source !== list && current.position != expected) {
                    expected = current.position
                    list.scrollToItem(current.position.index, current.position.offset)
                }
            }
        }
    }

    private fun LazyListState.position(): ListPosition =
        ListPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)
}
