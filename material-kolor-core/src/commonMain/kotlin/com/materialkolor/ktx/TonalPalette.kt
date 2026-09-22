package com.materialkolor.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Generates a [TonalPalette] from the given [color].
 *
 * @param[color] The color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.from(color: Color): TonalPalette = from(color.toArgb())

/**
 * Generates a [TonalPalette] from the given [argb] color int.
 *
 * @param[argb] The ARGB representation of a color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.from(argb: Int): TonalPalette = fromInt(argb)

/**
 * Generates a [TonalPalette] from the given [Hct].
 *
 * @param[hct] The color to generate the [TonalPalette] from.
 * @return The generated [TonalPalette].
 */
public fun TonalPalette.Companion.from(hct: Hct): TonalPalette = fromHct(hct)

/**
 * Get a tonal [Color] from the [TonalPalette] with the given [tone].
 *
 * @param[tone] HCT tone, measured from 0 to 100.
 */
public fun TonalPalette.toneColor(tone: Int): Color = Color(tone(tone))

/**
 * Pick the tone from this palette that is readable on top of [toneColor] at [tone].
 *
 * Roles like `onPrimary` give you a content color for the tone Material happened to pick. When you
 * pick the tone yourself, this gives you the matching content color from the same ramp, so a
 * hand-built theme never needs a hand-written background to content lookup table.
 *
 * The search moves away from the middle of the ramp, darker for a light [tone] and lighter for a
 * dark one, and takes the first tone that clears [threshold]. When the ramp runs out before the
 * ratio is met, the nearer end of the ramp is returned, which is the most readable color the
 * palette has to offer.
 *
 * @receiver The [TonalPalette] to take both the background and the content tone from.
 * @param[tone] HCT tone of the background, measured from 0 to 100.
 * @param[threshold] The contrast ratio the returned color should reach.
 * @return A [Color] from this palette that contrasts with `toneColor(tone)`.
 */
public fun TonalPalette.onTone(
    tone: Int,
    threshold: ContrastThreshold = ContrastThreshold.WCAG_AA_NORMAL_TEXT,
): Color {
    val background = toneColor(tone)
    val ratio = threshold.threshold

    // A light background reads best with dark content, so try that direction first.
    val preferDarker = tone >= MIDDLE_TONE
    val directions = if (preferDarker) intArrayOf(-1, 1) else intArrayOf(1, -1)

    for (direction in directions) {
        val target =
            if (direction < 0) {
                Contrast.darker(tone.toDouble(), ratio)
            } else {
                Contrast.lighter(tone.toDouble(), ratio)
            }

        val start = target?.roundAwayFrom(tone) ?: continue
        var candidate = start.coerceIn(MIN_TONE, MAX_TONE)

        // Rounding to a whole tone and mapping HCT back into sRGB can cost a sliver of contrast,
        // so walk outwards until the colors themselves clear the threshold.
        while (candidate in MIN_TONE..MAX_TONE) {
            val color = toneColor(candidate)
            if (color.hasEnoughContrast(background, threshold)) return color
            candidate += direction
        }
    }

    return toneColor(if (preferDarker) MIN_TONE else MAX_TONE)
}

/**
 * Create and remember a [TonalPalette] for a seed color the scheme does not cover.
 *
 * A [com.materialkolor.dynamiccolor.DynamicScheme] carries six ramps. Themes that own more accents
 * than that, a status color or a decorative one per category, build the rest from their own seeds.
 * Pass [harmonizeWith] to pull the seed towards another color, usually the scheme's source, so the
 * extra accents look like they belong to the same theme.
 *
 * @param[seed] The color to generate the [TonalPalette] from.
 * @param[harmonizeWith] The color to shift [seed] towards before generating, or `null` to use
 * [seed] as it is.
 * @return The remembered [TonalPalette].
 */
@Composable
public fun rememberTonalPalette(
    seed: Color,
    harmonizeWith: Color? = null,
): TonalPalette =
    remember(seed, harmonizeWith) {
        val source = harmonizeWith?.let { seed.harmonize(it) } ?: seed
        TonalPalette.from(source)
    }

private const val MIN_TONE = 0
private const val MAX_TONE = 100
private const val MIDDLE_TONE = 50

/**
 * Round to a whole tone in the direction that moves away from [origin], so rounding never eats
 * into the contrast the caller asked for.
 */
private fun Double.roundAwayFrom(origin: Int): Int = if (this < origin) floor(this).toInt() else ceil(this).toInt()
