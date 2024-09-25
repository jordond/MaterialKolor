package com.materialkolor.internal

import com.materialkolor.PaletteStyle
import com.materialkolor.scheme.Variant

internal val PaletteStyle.asVariant: Variant
    get() = when (this) {
        PaletteStyle.TonalSpot -> Variant.TONAL_SPOT
        PaletteStyle.Neutral -> Variant.NEUTRAL
        PaletteStyle.Vibrant -> Variant.VIBRANT
        PaletteStyle.Expressive -> Variant.EXPRESSIVE
        PaletteStyle.Rainbow -> Variant.RAINBOW
        PaletteStyle.FruitSalad -> Variant.FRUIT_SALAD
        PaletteStyle.Monochrome -> Variant.MONOCHROME
        PaletteStyle.Fidelity -> Variant.FIDELITY
        PaletteStyle.Content -> Variant.CONTENT
    }