package com.materialkolor.builder.feature.canvas

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role

/**
 * The color Show on ramp asks the Palettes tab to pick out, in the mode it was picked in.
 *
 * It lives only as long as the tab it points at. Nothing saves it.
 */
@Immutable
internal sealed interface RampTarget {
    /** Whether the color was picked from the dark column. */
    val isDark: Boolean

    /**
     * A role, found on its ramp by the tone it picked.
     *
     * @property[role] The role.
     */
    data class OfRole(
        val role: Role,
        override val isDark: Boolean,
    ) : RampTarget

    /**
     * One color of an accent family, on the accent's own ramp.
     *
     * @property[slot] Which accent and which of its four colors.
     */
    data class OfAccent(
        val slot: AccentSlot,
        override val isDark: Boolean,
    ) : RampTarget

    /**
     * The key color a palette is built around.
     *
     * @property[palette] The palette.
     */
    data class OfKeyColor(
        val palette: KeyColor,
        override val isDark: Boolean,
    ) : RampTarget
}
