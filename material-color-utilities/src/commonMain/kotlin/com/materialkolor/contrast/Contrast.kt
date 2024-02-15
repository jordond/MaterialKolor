/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.materialkolor.contrast

import com.materialkolor.utils.ColorUtils.lstarFromY
import com.materialkolor.utils.ColorUtils.yFromLstar
import kotlin.math.abs
import kotlin.math.max

/**
 * Color science for contrast utilities.
 *
 *
 * Utility methods for calculating contrast given two colors, or calculating a color given one
 * color and a contrast ratio.
 *
 *
 * Contrast ratio is calculated using XYZ's Y. When linearized to match human perception, Y
 * becomes HCT's tone and L*a*b*'s' L*.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
public object Contrast {

    // The minimum contrast ratio of two colors.
    // Contrast ratio equation = lighter + 5 / darker + 5, if lighter == darker, ratio == 1.
    private const val RATIO_MIN = 1.0

    // The maximum contrast ratio of two colors.
    // Contrast ratio equation = lighter + 5 / darker + 5. Lighter and darker scale from 0 to 100.
    // If lighter == 100, darker = 0, ratio == 21.
    private const val RATIO_MAX = 21.0
    private const val RATIO_30 = 3.0
    private const val RATIO_45 = 4.5
    private const val RATIO_70 = 7.0

    // Given a color and a contrast ratio to reach, the luminance of a color that reaches that ratio
    // with the color can be calculated. However, that luminance may not contrast as desired, i.e. the
    // contrast ratio of the input color and the returned luminance may not reach the contrast ratio
    // asked for.
    //
    // When the desired contrast ratio and the result contrast ratio differ by more than this amount,
    // an error value should be returned, or the method should be documented as 'unsafe', meaning,
    // it will return a valid luminance but that luminance may not meet the requested contrast ratio.
    //
    // 0.04 selected because it ensures the resulting ratio rounds to the same tenth.
    private const val CONTRAST_RATIO_EPSILON = 0.04

    // Color spaces that measure luminance, such as Y in XYZ, L* in L*a*b*, or T in HCT, are known as
    // perceptually accurate color spaces.
    //
    // To be displayed, they must gamut map to a "display space", one that has a defined limit on the
    // number of colors. Display spaces include sRGB, more commonly understood  as RGB/HSL/HSV/HSB.
    // Gamut mapping is undefined and not defined by the color space. Any gamut mapping algorithm must
    // choose how to sacrifice accuracy in hue, saturation, and/or lightness.
    //
    // A principled solution is to maintain lightness, thus maintaining contrast/a11y, maintain hue,
    // thus maintaining aesthetic intent, and reduce chroma until the color is in gamut.
    //
    // HCT chooses this solution, but, that doesn't mean it will _exactly_ matched desired lightness,
    // if only because RGB is quantized: RGB is expressed as a set of integers: there may be an RGB
    // color with, for example, 47.892 lightness, but not 47.891.
    //
    // To allow for this inherent incompatibility between perceptually accurate color spaces and
    // display color spaces, methods that take a contrast ratio and luminance, and return a luminance
    // that reaches that contrast ratio for the input luminance, purposefully darken/lighten their
    // result such that the desired contrast ratio will be reached even if inaccuracy is introduced.
    //
    // 0.4 is generous, ex. HCT requires much less delta. It was chosen because it provides a rough
    // guarantee that as long as a perceptual color space gamut maps lightness such that the resulting
    // lightness rounds to the same as the requested, the desired contrast ratio will be reached.
    private const val LUMINANCE_GAMUT_MAP_TOLERANCE = 0.4

    /**
     * Contrast ratio is a measure of legibility, its used to compare the lightness of two colors.
     * This method is used commonly in industry due to its use by WCAG.
     *
     * To compare lightness, the colors are expressed in the XYZ color space, where Y is lightness,
     * also known as relative luminance.
     *
     * The equation is ratio = lighter Y + 5 / darker Y + 5.
     *
     * @param[y1] Y in XYZ, relative luminance.
     * @param[y2] Y in XYZ, relative luminance.
     * @return Contrast ratio of two colors.
     */
    public fun ratioOfYs(y1: Double, y2: Double): Double {
        val lighter: Double = max(y1, y2)
        val darker = if (lighter == y2) y1 else y2
        return (lighter + 5.0) / (darker + 5.0)
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
     * @param[tone1] T in HCT, L* in L*a*b*.
     * @param[tone2] T in HCT, L* in L*a*b*.
     * @return Contrast ratio of two tones. T in HCT, L* in L*a*b*.
     */
    public fun ratioOfTones(tone1: Double, tone2: Double): Double {
        return ratioOfYs(yFromLstar(tone1), yFromLstar(tone2))
    }

    /**
     * Returns T in HCT, L* in L*a*b* >= tone parameter that ensures ratio with input T/L*.
     * Returns -1 if ratio cannot be achieved.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     * @return T in HCT, L* in L*a*b* >= tone parameter that ensures ratio with input T/L*.
     */
    public fun lighter(tone: Double, ratio: Double): Double {
        if (tone < 0.0 || tone > 100.0) {
            return -1.0
        }
        // Invert the contrast ratio equation to determine lighter Y given a ratio and darker Y.
        val darkY = yFromLstar(tone)
        val lightY = ratio * (darkY + 5.0) - 5.0
        if (lightY < 0.0 || lightY > 100.0) {
            return -1.0
        }
        val realContrast = ratioOfYs(lightY, darkY)
        val delta: Double = abs(realContrast - ratio)
        if (realContrast < ratio && delta > CONTRAST_RATIO_EPSILON) {
            return -1.0
        }
        val returnValue = lstarFromY(lightY) + LUMINANCE_GAMUT_MAP_TOLERANCE
        // NOMUTANTS--important validation step; functions it is calling may change implementation.
        return if (returnValue < 0 || returnValue > 100) {
            -1.0
        } else returnValue
    }

    /**
     * Returns T in HCT, L* in L*a*b* >= tone parameter that ensures ratio with input T/L*.
     * Returns -1 if ratio cannot be achieved.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     * @return T in HCT, L* in L*a*b* >= tone parameter that ensures ratio with input T/L*.
     */
    public fun lighter(tone: Double, ratio: Float): Float {
        return lighter(tone, ratio.toDouble()).toFloat()
    }

    /**
     * Tone >= tone parameter that ensures ratio. 100 if ratio cannot be achieved.
     *
     * This method is unsafe because the returned value is guaranteed to be in bounds, but, the in
     * bounds return value may not reach the desired ratio.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     */
    public fun lighterUnsafe(tone: Double, ratio: Double): Double {
        val lighterSafe = lighter(tone, ratio)
        return if (lighterSafe < 0.0) 100.0 else lighterSafe
    }

    /**
     * Tone >= tone parameter that ensures ratio. 100 if ratio cannot be achieved.
     *
     * This method is unsafe because the returned value is guaranteed to be in bounds, but, the in
     * bounds return value may not reach the desired ratio.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     */
    public fun lighterUnsafe(tone: Double, ratio: Float): Float {
        return lighterUnsafe(tone, ratio.toDouble()).toFloat()
    }

    /**
     * Returns T in HCT, L* in L*a*b* <= tone parameter that ensures ratio with input T/L*. Returns -1
     * if ratio cannot be achieved.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     * @return T in HCT, L* in L*a*b* <= tone parameter that ensures ratio with input T/L*.
     */
    public fun darker(tone: Double, ratio: Double): Double {
        if (tone < 0.0 || tone > 100.0) {
            return -1.0
        }
        // Invert the contrast ratio equation to determine darker Y given a ratio and lighter Y.
        val lightY = yFromLstar(tone)
        val darkY = (lightY + 5.0) / ratio - 5.0
        if (darkY < 0.0 || darkY > 100.0) {
            return -1.0
        }
        val realContrast = ratioOfYs(lightY, darkY)
        val delta: Double = abs(realContrast - ratio)
        if (realContrast < ratio && delta > CONTRAST_RATIO_EPSILON) {
            return -1.0
        }

        // For information on 0.4 constant, see comment in lighter(tone, ratio).
        val returnValue = lstarFromY(darkY) - LUMINANCE_GAMUT_MAP_TOLERANCE
        // NOMUTANTS--important validation step; functions it is calling may change implementation.
        return if (returnValue < 0 || returnValue > 100) {
            -1.0
        } else returnValue
    }

    /**
     * Returns T in HCT, L* in L*a*b* <= tone parameter that ensures ratio with input T/L*. Returns -1
     * if ratio cannot be achieved.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     * @return T in HCT, L* in L*a*b* <= tone parameter that ensures ratio with input T/L*.
     */
    public fun darker(tone: Double, ratio: Float): Float {
        return darker(tone, ratio.toDouble()).toFloat()
    }

    /**
     * Tone <= tone parameter that ensures ratio. 0 if ratio cannot be achieved.
     *
     * This method is unsafe because the returned value is guaranteed to be in bounds, but, the in
     * bounds return value may not reach the desired ratio.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     */
    public fun darkerUnsafe(tone: Double, ratio: Double): Double {
        val darkerSafe = darker(tone, ratio)
        return max(0.0, darkerSafe)
    }

    /**
     * Tone <= tone parameter that ensures ratio. 0 if ratio cannot be achieved.
     *
     * This method is unsafe because the returned value is guaranteed to be in bounds, but, the in
     * bounds return value may not reach the desired ratio.
     *
     * @param[tone] Tone return value must contrast with.
     * @param[ratio] Desired contrast ratio of return value and tone parameter.
     */
    public fun darkerUnsafe(tone: Double, ratio: Float): Float {
        return darkerUnsafe(tone, ratio.toDouble()).toFloat()
    }
}
