package com.materialkolor.builder.domain.color

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The ways the color picker can write a color out, one per option of its format switch (F-06).
 *
 * Every format writes text that [ColorInput.parse] reads back to the same color, so a field can show
 * a color in any of them and take typed edits in the same form.
 */
public enum class ColorFormat {
    /** Red as `#FF0000`. */
    Hex,

    /** Red as `rgb(255 0 0)`, each channel from 0 to 255. */
    Rgb,

    /** Red as `hsl(0 100% 50%)`, the hue in degrees, then saturation and lightness as percentages. */
    Hsl,

    /** Red as about `oklch(0.62796 0.25768 29.234)`, lightness from 0 to 1, then chroma, then the hue in degrees. */
    Oklch,
}

/**
 * The three numbers [color] is written with in this format, in the order the text lists them.
 *
 * Hex and RGB give red, green and blue from 0 to 255. HSL gives the hue in degrees, then saturation
 * and lightness from 0 to 100. OKLCH gives lightness from 0 to 1, chroma, then the hue in degrees. A
 * grey has no hue of its own, so it reads as 0.
 */
internal fun ColorFormat.channelsOf(color: Argb): List<Double> =
    when (this) {
        ColorFormat.Hex, ColorFormat.Rgb -> {
            listOf(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())
        }
        ColorFormat.Hsl -> {
            val hsl = color.toHsl()
            listOf(hsl.hue, hsl.saturation * Percent, hsl.lightness * Percent)
        }
        ColorFormat.Oklch -> {
            val oklch = color.toOklab().toOklch()
            val grey = oklch.c.roundTo(OklchChromaDecimals) == 0.0
            listOf(oklch.l, if (grey) 0.0 else oklch.c, if (grey) 0.0 else oklch.h)
        }
    }

/**
 * [color] written in this format, with enough decimals that [ColorInput.parse] reads it back to the
 * same color and no more.
 */
public fun ColorFormat.textOf(color: Argb): String {
    val channels = channelsOf(color)
    return when (this) {
        ColorFormat.Hex -> {
            color.toHex()
        }
        ColorFormat.Rgb -> {
            "rgb(${channels.joinToString(" ") { channel -> channel.toDecimalText(0) }})"
        }
        ColorFormat.Hsl -> {
            val (hue, saturation, lightness) = channels.map { channel -> channel.toDecimalText(HslDecimals) }
            "hsl($hue $saturation% $lightness%)"
        }
        ColorFormat.Oklch -> {
            val lightness = channels[0].toDecimalText(OklchLightnessDecimals)
            val chroma = channels[1].toDecimalText(OklchChromaDecimals)
            val hue = channels[2].toDecimalText(OklchHueDecimals)
            "oklch($lightness $chroma $hue)"
        }
    }
}

/**
 * A color in HSL.
 *
 * @property[hue] Degrees, 0 up to 360, and 0 for a grey.
 * @property[saturation] 0.0 to 1.0.
 * @property[lightness] 0.0 to 1.0.
 */
internal class Hsl(
    val hue: Double,
    val saturation: Double,
    val lightness: Double,
)

/**
 * Turn gamma encoded sRGB into HSL with the CSS Color 4 formula, the way back from
 * [ColorSpaces.hslToSrgb].
 */
internal fun srgbToHsl(rgb: Rgb): Hsl {
    val brightest = max(rgb.red, max(rgb.green, rgb.blue))
    val dimmest = min(rgb.red, min(rgb.green, rgb.blue))
    val lightness = (brightest + dimmest) / 2
    val spread = brightest - dimmest
    if (spread == 0.0) return Hsl(hue = 0.0, saturation = 0.0, lightness = lightness)
    val sixths = when (brightest) {
        rgb.red -> (rgb.green - rgb.blue) / spread + if (rgb.green < rgb.blue) 6 else 0
        rgb.green -> (rgb.blue - rgb.red) / spread + 2
        else -> (rgb.red - rgb.green) / spread + 4
    }
    val saturation = (brightest - lightness) / min(lightness, 1 - lightness)
    return Hsl(hue = sixths * DegreesPerSixth, saturation = saturation, lightness = lightness)
}

/** This color in HSL. */
internal fun Argb.toHsl(): Hsl = srgbToHsl(toSrgb())

/** This number rounded to [decimals] places, with trailing zeros and a bare point left off. */
private fun Double.toDecimalText(decimals: Int): String {
    val scale = 10.0.pow(decimals).toLong()
    val scaled = (this * scale).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val magnitude = abs(scaled)
    val fraction = (magnitude % scale).toString().padStart(decimals, '0').trimEnd('0')
    val whole = magnitude / scale
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}

private fun Double.roundTo(decimals: Int): Double {
    val scale = 10.0.pow(decimals)
    return (this * scale).roundToLong() / scale
}

private const val Percent: Double = 100.0

private const val DegreesPerSixth: Double = 60.0

/** Two decimals keep every channel within a fraction of an eight bit step, so HSL text round trips. */
private const val HslDecimals: Int = 2

/**
 * OKLCH needs more than HSL. A color on the sRGB edge sits a hair from leaving it, and four decimals
 * can push it far enough out that reading it back reports a clamp.
 */
private const val OklchLightnessDecimals: Int = 5

private const val OklchChromaDecimals: Int = 5

private const val OklchHueDecimals: Int = 3
