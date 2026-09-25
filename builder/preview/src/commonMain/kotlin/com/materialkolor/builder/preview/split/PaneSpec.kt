package com.materialkolor.builder.preview.split

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ColorMatrix
import com.materialkolor.builder.engine.resolve.ThemeResult

/**
 * What one preview pane draws.
 *
 * [result] has to be resolved from `document.forTarget(target)`, never from the raw document, so a
 * setting the target turns off never shows in its preview (D35). The canvas does that before it
 * builds a spec. A pane never resolves anything itself, it only reads [result].
 *
 * @property[result] The resolved theme for the target the preview shows.
 * @property[isDark] Which mode of [result] the pane is drawn in.
 * @property[label] What the pane is called, read out by the split handle, such as "Light".
 * @property[filter] A color matrix the whole pane is drawn through, for vision simulation and the
 * held grayscale peek, or null to draw it as it is. A `ColorMatrix` wraps a mutable array and
 * compares by that array, so pass a fresh matrix for a new filter rather than editing the old one,
 * or the spec looks unchanged and the pane keeps the filter it already drew with.
 */
@Immutable
public class PaneSpec(
    public val result: ThemeResult,
    public val isDark: Boolean,
    public val label: String,
    public val filter: ColorMatrix? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaneSpec) return false
        return result === other.result && isDark == other.isDark && label == other.label && filter == other.filter
    }

    override fun hashCode(): Int {
        var hash = result.hashCode()
        hash = 31 * hash + isDark.hashCode()
        hash = 31 * hash + label.hashCode()
        hash = 31 * hash + filter.hashCode()
        return hash
    }

    override fun toString(): String = "PaneSpec(label=$label, isDark=$isDark, filter=${filter != null})"
}

/**
 * Which copy of a split a pane is.
 *
 * A single pane is always the start copy.
 */
public enum class PaneSide {
    /**
     * The copy assistive tech and the keyboard reach, drawn from the start edge to the handle.
     */
    Start,

    /**
     * The clipped copy past the handle, only there for the eye and the pointer.
     */
    End,
}

/**
 * The side of the split the surrounding pane is on.
 */
internal val LocalPaneSide: ProvidableCompositionLocal<PaneSide> = staticCompositionLocalOf { PaneSide.Start }
