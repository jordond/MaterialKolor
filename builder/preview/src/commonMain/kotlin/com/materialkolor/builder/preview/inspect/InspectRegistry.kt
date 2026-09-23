package com.materialkolor.builder.preview.inspect

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.preview.split.PaneSide

/**
 * Where each element that declared its roles sits in the window, per pane.
 *
 * Elements only record themselves while a registry is provided, which is while Inspect is on. The
 * overlay picks the pane under the pointer and asks [hit] for the element there. The registry is
 * read and written on the UI thread only.
 */
@Stable
public class InspectRegistry {
    private val entries = LinkedHashMap<Any, InspectEntry>()

    /** How many elements are recorded right now, across every pane. */
    public val size: Int
        get() = entries.size

    /**
     * The smallest element on [side] whose window bounds hold [point], or null when none does.
     *
     * The smallest is the most specific, a button rather than the card around it. Of two elements
     * with the same area, the one recorded last wins.
     */
    public fun hit(
        side: PaneSide,
        point: Offset,
    ): InspectEntry? {
        var best: InspectEntry? = null
        for (entry in entries.values) {
            if (entry.side != side || !entry.bounds.contains(point)) continue
            if (best == null || entry.bounds.area <= best.bounds.area) best = entry
        }
        return best
    }

    /** Record or move the element [owner], which makes it the one recorded last. */
    internal fun record(
        owner: Any,
        entry: InspectEntry,
    ) {
        // A put on a key already there keeps its old place, so take it out first.
        entries.remove(owner)
        entries[owner] = entry
    }

    /** Forget the element [owner], which has left the screen. */
    internal fun remove(owner: Any) {
        entries.remove(owner)
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
