package com.materialkolor.builder.preview.inspect

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.preview.split.PaneSide

/**
 * Where each element that declared its roles sits in the window, per pane, and which of them hold
 * keyboard focus.
 *
 * Elements only record themselves while a registry is provided, which is while Inspect is on. The
 * overlay picks the pane under the pointer and asks [hit] for the element there, and reads
 * [focused] for the card keyboard focus shows. The registry is read and written on the UI thread
 * only.
 */
@Stable
public class InspectRegistry {
    private val entries = LinkedHashMap<Any, InspectEntry>()

    /**
     * The same map again, in snapshot state that takes every write for a change. It is set each time
     * an element arrives, moves or leaves, so a draw or placement that reads [entryOf] runs again then
     * without recomposing anything.
     */
    private val live = mutableStateOf(entries, neverEqualPolicy())

    /** The elements that hold focus or contain what does, whether or not they are recorded yet. */
    private val focusedOwners = HashSet<Any>()

    /** The recorded elements among [focusedOwners], kept in snapshot state so a reader sees focus move. */
    private val focusedEntries = mutableStateMapOf<Any, InspectEntry>()

    /** How many elements are recorded right now, across every pane. */
    public val size: Int
        get() = entries.size

    /**
     * The smallest recorded element that holds focus or contains what does, or null when none does.
     *
     * An element holding focus sits inside every declared element around it, so the smallest is
     * the one focus is really on. Reading it in composition recomposes when focus moves.
     */
    public val focused: InspectEntry?
        get() = focusedOwner()?.let { owner -> focusedEntries[owner] }

    /** The element behind [focused], so a reader can tell focus moving from the same element moving. */
    internal fun focusedOwner(): Any? = focusedEntries.entries.minByOrNull { (_, entry) -> entry.bounds.area }?.key

    /**
     * The smallest element on [side] whose window bounds hold [point], or null when none does.
     *
     * The smallest is the most specific, a button rather than the card around it. Of two elements
     * with the same area, the one recorded last wins.
     */
    public fun hit(
        side: PaneSide,
        point: Offset,
    ): InspectEntry? = ownerAt(side, point)?.let { owner -> entries[owner] }

    /** The element [hit] finds, as its key in the registry. */
    internal fun ownerAt(
        side: PaneSide,
        point: Offset,
    ): Any? {
        var best: Any? = null
        var bestArea = Float.POSITIVE_INFINITY
        for ((owner, entry) in entries) {
            if (entry.side != side || !entry.bounds.contains(point)) continue
            if (entry.bounds.area <= bestArea) {
                best = owner
                bestArea = entry.bounds.area
            }
        }
        return best
    }

    /**
     * The element [owner] as last recorded, or null once it has left. A draw or placement block that
     * reads it runs again each time the element moves, as it does while the preview scrolls.
     */
    internal fun entryOf(owner: Any): InspectEntry? = live.value[owner]

    /** Record or move the element [owner], which makes it the one recorded last. */
    internal fun record(
        owner: Any,
        entry: InspectEntry,
    ) {
        // A put on a key already there keeps its old place, so take it out first.
        val old = entries.remove(owner)
        entries[owner] = entry
        if (old != entry) live.value = entries
        if (owner in focusedOwners && focusedEntries[owner] != entry) focusedEntries[owner] = entry
    }

    /** Note whether the element [owner] holds focus or contains what does. */
    internal fun focus(
        owner: Any,
        focused: Boolean,
    ) {
        if (focused) {
            focusedOwners += owner
            val entry = entries[owner]
            if (entry != null && focusedEntries[owner] != entry) focusedEntries[owner] = entry
        } else {
            focusedOwners -= owner
            focusedEntries -= owner
        }
    }

    /** Forget the element [owner], which has left the screen. */
    internal fun remove(owner: Any) {
        if (entries.remove(owner) != null) live.value = entries
        focus(owner, focused = false)
    }
}

/**
 * One element that declared its roles, as Inspect sees it.
 *
 * @property[side] The copy of the split it is drawn in.
 * @property[roles] The colors it reads, in the order it declared them.
 * @property[bounds] Where it sits in the window.
 */
public data class InspectEntry(
    public val side: PaneSide,
    public val roles: List<ColorRef>,
    public val bounds: Rect,
)

/**
 * The registry elements record themselves in, or null while Inspect is off.
 *
 * It is dynamic on purpose. Turning Inspect on swaps the value in place, and every element that
 * declared its roles notices and records where it already is, with no relayout.
 */
public val LocalInspectRegistry: ProvidableCompositionLocal<InspectRegistry?> = compositionLocalOf { null }

private val Rect.area: Float
    get() = width * height
