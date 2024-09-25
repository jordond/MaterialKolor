package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette

/**
 * Generates a [TonalPalette] from the given [color].
 *
 * @param[color] The color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.from(color: Color): TonalPalette {
    return from(color.toArgb())
}

/**
 * Generates a [TonalPalette] from the given [argb] color int.
 *
 * @param[argb] The ARGB representation of a color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.from(argb: Int): TonalPalette {
    return fromInt(argb)
}

/**
 * Generates a [TonalPalette] from the given [Hct].
 *
 * @param[hct] The color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.from(hct: Hct): TonalPalette {
    return fromHct(hct)
}

/**
 * Get a tonal [Color] from the [TonalPalette] with the given [tone].
 *
 * @param[tone] HCT tone, measured from 0 to 100.
 */
public fun TonalPalette.toneColor(tone: Int): Color {
    return Color(tone(tone))
}