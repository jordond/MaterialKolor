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
