package com.materialkolor.palette

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.hct.Hct
import com.materialkolor.score.Score

internal const val DEFAULT_DESIRED_COLORS: Int = 4

/**
 * Rank the swatches of this [Palette] by how well they would seed a UI theme, using the same
 * scoring Android applies to wallpapers.
 *
 * kmpalette found the swatches, this puts them through Material Color Utilities' [Score], which
 * drops the washed out and barely used ones and spreads the survivors across the hue circle.
 *
 * @receiver the [Palette] whose swatches are scored.
 * @param[fallback] color to be returned if no other options available.
 * @param[desired] The number of colors to return.
 * @param[filter] whether to filter out undesirable combinations.
 * @return Colors sorted by suitability for a UI theme. The most suitable color is the first item,
 * the least suitable is the last. There will always be at least one color returned. If none of
 * the swatches suit a theme, the list holds only [fallback].
 */
public fun Palette.themeColors(
    fallback: Color,
    desired: Int = DEFAULT_DESIRED_COLORS,
    filter: Boolean = true,
): List<Color> =
    Score
        .score(
            colorsToPopulation = populationByColor(),
            desired = desired,
            fallbackColorArgb = fallback.toArgb(),
            filter = filter,
        ).map { argb -> Color(argb) }

/**
 * Determine the most suitable swatch of this [Palette] for a UI theme.
 *
 * @receiver the [Palette] whose swatches are scored.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme, or [fallback] when no swatch suits one.
 */
public fun Palette.themeColor(
    fallback: Color,
    filter: Boolean = true,
): Color = themeColors(fallback = fallback, desired = 1, filter = filter).first()

/**
 * Determine the most suitable swatch of this [Palette] for a UI theme, or `null`.
 *
 * Use this when you would rather show nothing than show a color the image did not really contain.
 *
 * @receiver the [Palette] whose swatches are scored.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme, or `null` if no suitable color was found.
 */
public fun Palette.themeColorOrNull(filter: Boolean = true): Color? =
    Score
        .score(
            colorsToPopulation = populationByColor(),
            desired = 1,
            fallbackColorArgb = null,
            filter = filter,
        ).firstOrNull()
        ?.let { argb -> Color(argb) }

/**
 * The vibrant swatch of this [Palette] when there is one, otherwise the dominant one.
 *
 * This is kmpalette's own pick rather than a scored one, so it is the cheap answer when you want
 * a color that is in the image rather than the color a theme would be built from.
 *
 * @receiver the [Palette] to pick a swatch from.
 * @return The color of the vibrant or dominant swatch, or `null` when the palette has no swatches.
 */
public fun Palette.seedColorOrNull(): Color? {
    val swatch = vibrantSwatch ?: dominantSwatch
    return swatch?.let { found -> Color(found.rgb) }
}

/**
 * Convert this [Palette.Swatch] to its [Hct] representation.
 *
 * @receiver the [Palette.Swatch] to convert.
 * @return The [Hct] of the swatch color.
 */
public fun Palette.Swatch.toHct(): Hct = Hct.fromInt(rgb)

/**
 * The swatches of this palette as the color to population map [Score] expects.
 */
private fun Palette.populationByColor(): Map<Int, Int> =
    swatches.associate { swatch -> swatch.rgb to swatch.population }
