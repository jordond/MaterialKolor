package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.palettes.TonalPalette

/**
 * Generates a [TonalPalette] from the given [color].
 *
 * @param[color] The color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.fromColor(color: Color): TonalPalette {
    return fromInt(color.toArgb())
}