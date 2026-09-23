package com.materialkolor.builder.feature.picker

import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role

/**
 * What the color picker is editing.
 */
internal sealed interface PickerTarget {
    /** The seed. */
    data object Seed : PickerTarget

    /** One palette's key color override. */
    data class KeyColorOverride(
        val slot: KeyColor,
    ) : PickerTarget

    /** A role pinned to a color of its own, in the modes [mode] covers. */
    data class Pin(
        val role: Role,
        val mode: PinMode,
    ) : PickerTarget

    /** The seed of the extra color family at [index]. */
    data class Accent(
        val index: Int,
    ) : PickerTarget

    /** The second seed the Cmf style takes for its tertiary. */
    data object CmfSeed : PickerTarget
}
