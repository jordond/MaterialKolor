package com.materialkolor.builder.domain.color

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Three sRGB channels on a 0.0 to 1.0 scale, gamma encoded or linear depending on where they came from.
 *
 * A channel can land outside 0.0 to 1.0 partway through a conversion, and that is how a color outside sRGB shows up.
 */
internal class Rgb(
    val red: Double,
    val green: Double,
    val blue: Double,
)

/**
 * A color in OKLab, where straight line distance follows how different two colors look.
 *
 * @property[l] Lightness, 0.0 for black and 1.0 for white.
 * @property[a] Green to red.
 * @property[b] Blue to yellow.
 */
internal data class Oklab(
    val l: Double,
    val a: Double,
    val b: Double,
) {
    /** The squared distance to [other], which is enough to find the nearest of several colors. */
    fun distanceSquaredTo(other: Oklab): Double {
        val dl = l - other.l
        val da = a - other.a
        val db = b - other.b
        return dl * dl + da * da + db * db
    }

    fun toOklch(): Oklch {
        val hue = atan2(b, a) * DegreesPerRadian
        return Oklch(l = l, c = hypot(a, b), h = if (hue < 0) hue + FullTurn else hue)
    }
}

/**
 * OKLab in polar form, the space behind CSS `oklch()`.
 *
 * @property[l] Lightness, 0.0 for black and 1.0 for white.
 * @property[c] Chroma, 0.0 for grey and rarely past 0.37 inside sRGB.
 * @property[h] Hue in degrees, 0 up to 360.
 */
internal data class Oklch(
    val l: Double,
    val c: Double,
    val h: Double,
) {
    fun toOklab(): Oklab {
        val radians = h / DegreesPerRadian
        return Oklab(l = l, a = c * cos(radians), b = c * sin(radians))
    }
}

/**
 * A color brought inside sRGB, and whether that took any change beyond rounding to eight bits.
 */
internal class GamutMapped(
    val argb: Argb,
    val clamped: Boolean,
)

/**
 * Conversions between sRGB, linear sRGB, OKLab, OKLCH and HSL.
 *
 * The OKLab matrices are Björn Ottosson's, the same ones CSS Color 4 uses.
 */
internal object ColorSpaces {
    /** Undo the sRGB transfer curve for one channel. */
    fun srgbToLinear(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    /**
     * Apply the sRGB transfer curve to one channel, mirrored around zero so a negative channel stays negative
     * and can still be told apart from an in gamut one.
     */
    fun linearToSrgb(channel: Double): Double =
        when {
            channel < 0 -> -linearToSrgb(-channel)
            channel <= 0.0031308 -> channel * 12.92
            else -> 1.055 * channel.pow(1 / 2.4) - 0.055
        }

    fun linearToOklab(rgb: Rgb): Oklab {
        val l = cbrt(0.4122214708 * rgb.red + 0.5363325363 * rgb.green + 0.0514459929 * rgb.blue)
        val m = cbrt(0.2119034982 * rgb.red + 0.6806995451 * rgb.green + 0.1073969566 * rgb.blue)
        val s = cbrt(0.0883024619 * rgb.red + 0.2817188376 * rgb.green + 0.6299787005 * rgb.blue)
        return Oklab(
            l = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    fun oklabToLinear(lab: Oklab): Rgb {
        val l = (lab.l + 0.3963377774 * lab.a + 0.2158037573 * lab.b).pow(3)
        val m = (lab.l - 0.1055613458 * lab.a - 0.0638541728 * lab.b).pow(3)
        val s = (lab.l - 0.0894841775 * lab.a - 1.2914855480 * lab.b).pow(3)
        return Rgb(
            red = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            green = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            blue = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    /**
     * Turn HSL into gamma encoded sRGB with the CSS Color 4 formula.
     *
     * @param[hue] Degrees, 0 up to 360.
     * @param[saturation] 0.0 to 1.0.
     * @param[lightness] 0.0 to 1.0.
     */
    fun hslToSrgb(
        hue: Double,
        saturation: Double,
        lightness: Double,
    ): Rgb {
        val spread = saturation * min(lightness, 1 - lightness)

        fun channel(offset: Int): Double {
            val k = (offset + hue / 30) % 12
            return lightness - spread * max(-1.0, min(min(k - 3, 9 - k), 1.0))
        }

        return Rgb(red = channel(0), green = channel(8), blue = channel(4))
    }

    /**
     * Bring an OKLCH color inside sRGB without moving its lightness or hue.
     *
     * A color that does not fit loses chroma, found by a binary search, until it does. That is the idea behind the
     * CSS Color 4 gamut mapping, kept simple. Clipping each channel on its own would be cheaper but can turn a
     * saturated green yellow, and the hue is the part of a seed people care about most.
     *
     * [color] should have a lightness from 0.0 to 1.0. At chroma zero every such lightness is a grey that fits.
     */
    fun oklchToArgb(color: Oklch): GamutMapped {
        val exact = oklchToSrgb(color)
        if (exact.fitsEightBits()) return GamutMapped(srgbToArgb(exact), clamped = false)
        var fits = 0.0
        var overflows = min(color.c, MaxSearchChroma)
        repeat(GamutSearchSteps) {
            val chroma = (fits + overflows) / 2
            if (oklchToSrgb(color.copy(c = chroma)).fitsEightBits()) fits = chroma else overflows = chroma
        }
        return GamutMapped(srgbToArgb(oklchToSrgb(color.copy(c = fits))), clamped = true)
    }

    /** Round gamma encoded channels to eight bits, clipping whatever rounding error is left. */
    fun srgbToArgb(rgb: Rgb): Argb {
        val red = rgb.red.toByteChannel()
        val green = rgb.green.toByteChannel()
        val blue = rgb.blue.toByteChannel()
        return Argb((red shl 16) or (green shl 8) or blue)
    }

    private fun oklchToSrgb(color: Oklch): Rgb {
        val linear = oklabToLinear(color.toOklab())
        return Rgb(linearToSrgb(linear.red), linearToSrgb(linear.green), linearToSrgb(linear.blue))
    }

    /** True when clipping would not change any channel once it is rounded to eight bits. */
    private fun Rgb.fitsEightBits(): Boolean = red.fitsEightBits() && green.fitsEightBits() && blue.fitsEightBits()

    private fun Double.fitsEightBits(): Boolean = this * ChannelMax >= -0.5 && this * ChannelMax < ChannelMax + 0.5

    private fun Double.toByteChannel(): Int = (this * ChannelMax).roundToInt().coerceIn(0, ChannelMax.toInt())
}

/** This color as gamma encoded sRGB channels from 0.0 to 1.0. */
internal fun Argb.toSrgb(): Rgb = Rgb(red / ChannelMax, green / ChannelMax, blue / ChannelMax)

/** This color in OKLab, the space [ColorNames] measures distance in. */
internal fun Argb.toOklab(): Oklab {
    val srgb = toSrgb()
    return ColorSpaces.linearToOklab(
        Rgb(
            red = ColorSpaces.srgbToLinear(srgb.red),
            green = ColorSpaces.srgbToLinear(srgb.green),
            blue = ColorSpaces.srgbToLinear(srgb.blue),
        ),
    )
}

private const val ChannelMax: Double = 255.0

private const val FullTurn: Double = 360.0

private const val DegreesPerRadian: Double = 180.0 / PI

/** Past any chroma sRGB can show, so the search starts from a sane upper bound however large the input. */
private const val MaxSearchChroma: Double = 0.5

/** Enough halvings of [MaxSearchChroma] to land well under one eight bit step. */
private const val GamutSearchSteps: Int = 24
