package com.materialkolor.builder.feature.picker

import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResult

/**
 * What the color picker is editing.
 */
internal sealed interface PickerTarget {
    /**
     * The theme's seed color.
     */
    data object Seed : PickerTarget

    /**
     * One palette's key color override.
     */
    data class KeyColorOverride(
        val slot: KeyColor,
    ) : PickerTarget

    /**
     * A role pinned to a color of its own, in the modes [mode] covers.
     */
    data class Pin(
        val role: Role,
        val mode: PinMode,
    ) : PickerTarget

    /**
     * The seed of the extra color family at [index].
     */
    data class Accent(
        val index: Int,
    ) : PickerTarget

    /**
     * The second seed the Cmf style takes for its tertiary.
     */
    data object CmfSeed : PickerTarget
}

// b-307

/**
 * The control that says whether [this] takes input on the document's target.
 */
internal val PickerTarget.control: Control
    get() = when (this) {
        PickerTarget.Seed -> {
            Control.SeedEntryPoints
        }
        is PickerTarget.KeyColorOverride -> {
            if (slot == KeyColor.Primary) Control.PrimaryOverride else Control.OtherOverrides
        }
        is PickerTarget.Pin -> {
            Control.RolePins
        }
        is PickerTarget.Accent -> {
            Control.ExtendedColors
        }
        PickerTarget.CmfSeed -> {
            Control.CmfSecondSeed
        }
    }

/**
 * The color [document] stores for [this], or null for none. A key color, a pin half or the Cmf
 * seed left to the scheme reads null, as does an accent the document lacks.
 */
internal fun PickerTarget.storedIn(document: ThemeDocument): Argb? =
    when (this) {
        PickerTarget.Seed -> {
            document.seed
        }
        is PickerTarget.KeyColorOverride -> {
            document.keyColors[slot]
        }
        is PickerTarget.Pin -> {
            val pin = document.pins[role]
            if (mode == PinMode.Light) pin?.light else pin?.dark
        }
        is PickerTarget.Accent -> {
            document.accents.getOrNull(index)?.seed
        }
        PickerTarget.CmfSeed -> {
            document.cmfTertiarySeed
        }
    }

/**
 * The color the picker starts on, what [document] stores or else what [result] worked out for
 * [this] from the seed.
 */
internal fun PickerTarget.shownIn(
    document: ThemeDocument,
    result: ThemeResult,
): Argb =
    storedIn(document) ?: when (this) {
        is PickerTarget.KeyColorOverride -> result.ramps[slot, false].keyColor
        is PickerTarget.Pin -> result.roles[role, mode == PinMode.Dark].argb
        PickerTarget.CmfSeed -> result.ramps[KeyColor.Tertiary, false].keyColor
        PickerTarget.Seed, is PickerTarget.Accent -> document.seed
    }
