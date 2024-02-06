package com.materialkolor.ktx

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.ajalt.colormath.model.HSL
import com.materialkolor.contrast.Contrast
import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.internal.toColormathColor
import com.materialkolor.internal.toComposeColor
import com.materialkolor.utils.ColorUtils

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
 * Contrast ratio is a measure of legibility, its used to compare the lightness of two colors.
 * This method is used commonly in industry due to its use by WCAG.
 *
 * To compare lightness, the colors are expressed in the XYZ color space, where Y is lightness,
 * also known as relative luminance.
 *
 * The equation is ratio = lighter Y + 5 / darker Y + 5.
 *
 * WCAG 2.0 level AA requires a contrast ratio of at least 4.5:1 for normal text
 * and 3:1 for large text. WCAG 2.1 requires a contrast ratio of at least 3:1 for graphics and
 * user interface components (such as form input borders). WCAG Level AAA requires a
 * contrast ratio of at least 7:1 for normal text and 4.5:1 for large text.
 *
 * @receiver The first [Color] to compare.
 * @param[other] The second [Color] to compare.
 * @return Contrast ratio of two colors.
 */
public fun Color.contrastRatio(other: Color): Double {
    val firstXyz = ColorUtils.xyzFromArgb(toArgb())
    val secondXyz = ColorUtils.xyzFromArgb(other.toArgb())
    return Contrast.ratioOfYs(firstXyz[1], secondXyz[1])
}

/**
 * Contrast ratio of two tones. T in HCT, L* in L*a*b*. Also known as luminance or perpetual
 * luminance.
 *
 * Contrast ratio is defined using Y in XYZ, relative luminance. However, relative luminance is
 * linear to number of photons, not to perception of lightness. Perceptual luminance, L* in
 * L*a*b*, T in HCT, is. Designers prefer color spaces with perceptual luminance since they're
 * accurate to the eye.
 *
 * Y and L* are pure functions of each other, so it possible to use perceptually accurate color
 * spaces, and measure contrast, and measure contrast in a much more understandable way: instead
 * of a ratio, a linear difference. This allows a designer to determine what they need to adjust a
 * color's lightness to in order to reach their desired contrast, instead of guessing & checking
 * with hex codes.
 *
 * @receiver The first [Color] to compare.
 * @param[other] The second [Color] to compare.
 * @return Contrast ratio of two tones. T in HCT, L* in L*a*b*.
 */
public fun Color.tonalContrastRatio(other: Color): Double {
    val firstHct = toHct()
    val secondHct = other.toHct()
    return Contrast.ratioOfTones(firstHct.tone, secondHct.tone)
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
