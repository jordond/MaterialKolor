package com.materialkolor.builder.ktx

import com.materialkolor.PaletteStyle

internal val ExpressivePaletteStyles = setOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Neutral,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
)

val PaletteStyle.isExpressive: Boolean
    get() = this in ExpressivePaletteStyles

/**
 * The stable name we write a style under, in saved URLs and in exported code.
 *
 * [PaletteStyle] used to be an enum, so these match the old entry names and keep links that people
 * have already shared working.
 */
internal val PaletteStyle.storageName: String
    get() = when (this) {
        PaletteStyle.TonalSpot -> "TonalSpot"
        PaletteStyle.Neutral -> "Neutral"
        PaletteStyle.Vibrant -> "Vibrant"
        PaletteStyle.Expressive -> "Expressive"
        PaletteStyle.Rainbow -> "Rainbow"
        PaletteStyle.FruitSalad -> "FruitSalad"
        PaletteStyle.Monochrome -> "Monochrome"
        PaletteStyle.Fidelity -> "Fidelity"
        PaletteStyle.Content -> "Content"
        is PaletteStyle.Cmf -> "Cmf"
    }

/**
 * Find the style a [storageName] belongs to, or null when the name is not one we know.
 *
 * A stored `Cmf` comes back as `PaletteStyle.Cmf()` with no tertiary seed color, since the
 * builder has no way to pick one yet.
 */
internal fun paletteStyleOfStorageName(name: String): PaletteStyle? =
    PaletteStyle.KnownStyles.firstOrNull { style -> style.storageName == name }
