package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.ajalt.colormath.model.HSL
import com.materialkolor.internal.toColormathColor
import com.materialkolor.internal.toComposeColor
import com.materialkolor.utils.ColorUtils

/**
 * Check if the color is light.
 *
 * @receiver[Color] to check.
 * @return [Boolean] true if the color is light, false otherwise.
 */
public fun Color.isLight(): Boolean {
    return this != Color.Transparent && ColorUtils.calculateLuminance(toArgb()) > 0.5;
}

/**
 * Create a [Color] with the same hue as this color, but with the saturation and lightness of [other].
 *
 * @receiver[Color] to match hue.
 * @param[other] color to match saturation and lightness.
 * @return [Color] with the same hue as this color, but with the saturation and lightness of [other].
 */
internal fun Color.matchSaturation(other: Color): Color {
    val hsl = toColormathColor().toHSL()
    val otherHsl = other.toColormathColor().toHSL()
    return HSL(hsl.h, otherHsl.s, otherHsl.l).toComposeColor()
}