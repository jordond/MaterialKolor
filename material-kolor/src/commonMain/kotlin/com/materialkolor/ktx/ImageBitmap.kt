package com.materialkolor.ktx

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score

private const val DEFAULT_QUANTIZE_MAX_COLORS = 128
private const val DEFAULT_DESIRED_COLORS = 4

/**
 * Quantize the colors in a [ImageBitmap] to a maximum of [maxColors] colors.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[maxColors] The number of colors to divide the image into. A lower number of colors may be
 * returned.
 * @return A map of colors to their frequency in the image.
 */
public fun QuantizerCelebi.quantize(image: ImageBitmap, maxColors: Int): Map<Int, Int> {
    val pixels = IntArray(image.width * image.height)
    image.readPixels(
        buffer = pixels,
        startX = 0,
        startY = 0,
    )

    return quantize(pixels, maxColors)
}

/**
 * Rank the colors in a [ImageBitmap] by their suitability for being used for a UI theme.
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[maxColors] The number of colors to divide the image into.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[desired] The number of colors to return.
 * @return Colors sorted by suitability for a UI theme. The most suitable color is the first item,
 * the least suitable is the last. There will always be at least one color returned. If all
 * the input colors were not suitable for a theme, a default fallback color will be provided,
 * Google Blue.
 */
@Stable
public fun ImageBitmap.themeColors(
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    fallback: Color = Color(-0xbd7a0c),
    filter: Boolean = true,
    desired: Int = DEFAULT_DESIRED_COLORS,
): List<Color> {
    val quantized = QuantizerCelebi.quantize(image = this, maxColors)
    return Score.score(
        colorsToPopulation = quantized,
        desired = desired,
        fallbackColorArgb = fallback.toArgb(),
        filter = filter,
    ).map { Color(it) }
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[maxColors] The number of colors to divide the image into.
 * @return The most suitable color for a UI theme.
 */
@Stable
public fun ImageBitmap.themeColor(
    fallback: Color,
    filter: Boolean = true,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
): Color {
    return themeColors(maxColors = maxColors, fallback, filter).first()
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme or `null`
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[maxColors] The number of colors to divide the image into.
 * @return The most suitable color for a UI theme or `null` if no suitable color found.
 */
@Stable
public fun ImageBitmap.themeColorOrNull(
    filter: Boolean = true,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
): Color? {
    val quantized = QuantizerCelebi.quantize(image = this, maxColors = maxColors)
    return Score.score(
        colorsToPopulation = quantized,
        desired = 1,
        fallbackColorArgb = null,
        filter = filter,
    ).firstOrNull()?.let { colorInt -> Color(colorInt) }
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available. Defaults to primary color.
 * @param[maxColors] The number of colors to divide the image into.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[desired] The number of colors to return.
 * @return The most suitable colors for a UI theme.
 */
@Stable
@Composable
public fun rememberThemeColors(
    image: ImageBitmap,
    fallback: Color = MaterialTheme.colorScheme.primary,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    filter: Boolean = true,
    desired: Int = DEFAULT_DESIRED_COLORS,
): List<Color> {
    var themeColors by remember { mutableStateOf(listOf(fallback)) }
    LaunchedEffect(image, fallback, filter, maxColors) {
        themeColors = image.themeColors(maxColors, fallback, filter, desired)
    }

    return themeColors
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available. Defaults to primary color.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[maxColors] The number of colors to divide the image into.
 * @return The most suitable color for a UI theme.
 */
@Stable
@Composable
public fun rememberThemeColor(
    image: ImageBitmap,
    fallback: Color = MaterialTheme.colorScheme.primary,
    filter: Boolean = true,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
): Color {
    var themeColor by remember { mutableStateOf(fallback) }
    LaunchedEffect(image, fallback, filter, maxColors) {
        themeColor = image.themeColor(fallback, filter, maxColors)
    }

    return themeColor
}