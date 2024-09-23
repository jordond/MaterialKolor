package com.materialkolor.ktx

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.ajalt.colormath.model.HSL
import com.materialkolor.contrast.Contrast
import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.internal.toColormathColor
import com.materialkolor.internal.toComposeColor
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.utils.ColorUtils
import kotlin.math.roundToInt

/**
 * Check if the color is light.
 *
 * @receiver[Color] to check.
 * @return [Boolean] true if the color is light, false otherwise.
 */
@Stable
public fun Color.isLight(): Boolean {
    return this != Color.Transparent && ColorUtils.calculateLuminance(toArgb()) > 0.5;
}

/**
 * Lighten the color by the given ratio.
 *
 * @receiver[Color] to lighten.
 * @param[ratio] the ratio to lighten the color by.
 * @return [Color] The lightened color or the original color if the lightened color is not valid.
 */
@Stable
public fun Color.lighten(ratio: Float = 1.0f): Color {
    val hct = toHct()
    val tone = Contrast.lighter(hct.tone, ratio.toDouble()).takeIf { it > -1 }
    return if (tone == null) this else hct.withTone(tone).toColor()
}

/**
 * Darken the color by the given ratio.
 *
 * @receiver[Color] to darken.
 * @param[ratio] the ratio to darken the color by.
 * @return [Color] The darkened color or the original color if the darkened color is not valid.
 */
@Stable
public fun Color.darken(ratio: Float = 1.1f): Color {
    val hct = toHct()
    val tone = Contrast.darker(hct.tone, ratio.toDouble()).takeIf { it > -1 }
    return if (tone == null) this else hct.withTone(tone).toColor()
}

/**
 * Returns true if color is disliked.
 *
 * Disliked is defined as a dark yellow-green that is not neutral.
 *
 * @receiver The [Color] to check.
 * @return true if color is disliked
 */
@Stable
public fun Color.isDisliked(): Boolean {
    return DislikeAnalyzer.isDisliked(toHct())
}

/**
 * If color is disliked, lighten it to make it likable.
 *
 * @receiver The [Color] to check.
 * @return Lightened [Color] that is not disliked
 */
@Stable
public fun Color.fixIfDisliked(): Color {
    return DislikeAnalyzer.fixIfDisliked(toHct()).toColor()
}

/**
 * Convert the color to a hex string.
 *
 * @receiver[Color] to convert.
 * @param[includePrefix] whether to include the '#' [prefix].
 * @return [String] hex representation of the color.
 */
public fun Color.toHex(
    includePrefix: Boolean = true,
    prefix: String = "#",
    alwaysIncludeAlpha: Boolean = false,
): String {
    val alpha = (alpha * 255).roundToInt()
    val red = (red * 255).roundToInt()
    val green = (green * 255).roundToInt()
    val blue = (blue * 255).roundToInt()

    return buildString {
        if (includePrefix) append(prefix)
        if (alwaysIncludeAlpha || alpha < 255) append(alpha.format())
        append(red.format())
        append(green.format())
        append(blue.format())
    }.uppercase()
}

/**
 * Format the integer as a hex string.
 *
 * @receiver[Int] to format.
 * @return [String] hex representation of the integer.
 */
internal fun Int.format(): String {
    return toString(16).padStart(2, '0')
}

/**
 * Create a [Color] with the same hue as this color, but with the saturation and lightness of [other].
 *
 * @receiver[Color] to match hue.
 * @param[other] color to match saturation and lightness.
 * @return [Color] with the same hue as this color, but with the saturation and lightness of [other].
 */
@Stable
internal fun Color.matchSaturation(other: Color): Color {
    val hsl = toColormathColor().toHSL()
    val otherHsl = other.toColormathColor().toHSL()
    return HSL(hsl.h, otherHsl.s, otherHsl.l).toComposeColor()
}

/**
 * Convert the color to a [TonalPalette].
 *
 * @receiver[Color] to convert.
 * @return [TonalPalette] representation of the color.
 */
@Stable
internal fun Color.toTonalPalette(): TonalPalette {
    return TonalPalette.from(this)
}