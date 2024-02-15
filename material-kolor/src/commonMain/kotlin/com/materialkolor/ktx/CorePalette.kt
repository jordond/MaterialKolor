package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.palettes.CorePalette

/**
 * Create key tones from a color.
 *
 * @param[color] The color to create the key tones from.
 */
public fun CorePalette.Companion.of(color: Color): CorePalette = of(color.toArgb())

/**
 * Create key tones from a color.
 *
 * @param[color] The color to create the key tones from.
 */
public fun CorePalette.Companion.from(color: Color): CorePalette = of(color)

/**
 * Create content key tones from a color.
 *
 * @param[color] The color to create the content key tones from.
 */
public fun CorePalette.Companion.contentOf(color: Color): CorePalette = contentOf(color.toArgb())
