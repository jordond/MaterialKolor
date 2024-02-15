package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.contrast.Contrast
import com.materialkolor.utils.ColorUtils

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
 * Check if the contrast ratio between two colors is greater than or equal to a given threshold.
 *
 * Learn more about WCAG [here](https://www.w3.org/WAI/standards-guidelines/wcag).
 *
 * @receiver The first [Color] to compare.
 * @param[other] The second [Color] to compare.
 * @param[threshold] The threshold to compare the contrast ratio against.
 * @return `true` if the contrast ratio is greater than or equal to the threshold, `false` otherwise.
 */
public fun Color.hasEnoughContrast(
    other: Color,
    threshold: ContrastThreshold = ContrastThreshold.WCAG_AA_NORMAL_TEXT,
): Boolean = contrastRatio(other) >= threshold

/**
 * Define the threshold for a contrast ratio.
 *
 * Learn more about WCAG [here](https://www.w3.org/WAI/standards-guidelines/wcag).
 */
public enum class ContrastThreshold(public val threshold: Double) {
    WCAG_AA_NORMAL_TEXT(4.5),
    WCAG_AA_LARGE_TEXT(3.0),
    WCAG_AAA_NORMAL_TEXT(7.0),
    WCAG_AAA_LARGE_TEXT(4.5);

    public operator fun compareTo(value: Double): Int = threshold.compareTo(value)
}

private operator fun Double.compareTo(threshold: ContrastThreshold): Int {
    return compareTo(threshold.threshold)
}