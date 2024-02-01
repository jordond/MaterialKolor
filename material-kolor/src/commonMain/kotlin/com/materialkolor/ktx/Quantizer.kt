package com.materialkolor.ktx

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score

/**
 * Quantize the colors in a [ImageBitmap] to a maximum of [maxColors] colors.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[maxColors] max count of colors to be returned in the list.
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
 * @param[maxColors]max count of colors to be returned in the list.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @return Colors sorted by suitability for a UI theme. The most suitable color is the first item,
 * the least suitable is the last. There will always be at least one color returned. If all
 * the input colors were not suitable for a theme, a default fallback color will be provided,
 * Google Blue.
 */
public fun ImageBitmap.dominantColors(
    maxColors: Int = 4,
    fallback: Color = Color(-0xbd7a0c),
    filter: Boolean = true,
): List<Color> {
    val quantized = QuantizerCelebi.quantize(image = this, maxColors)
    return Score.score(quantized, maxColors, fallback.toArgb(), filter).map { Color(it) }
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme.
 */
public fun ImageBitmap.dominantColor(
    fallback: Color,
    filter: Boolean = true,
): Color {
    return dominantColors(maxColors = 1, fallback, filter).first()
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme or `null`
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme or `null` if no suitable color found.
 */
public fun ImageBitmap.dominantColorOrNull(filter: Boolean = true): Color? {
    val quantized = QuantizerCelebi.quantize(image = this, maxColors = 1)
    return Score
        .score(quantized, desired = 1, fallbackColorArgb = null, filter)
        .firstOrNull()
        ?.let { Color(it) }
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available. Defaults to primary color.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable colors for a UI theme.
 */
@Composable
public fun calculateDominantColors(
    image: ImageBitmap,
    fallback: Color = MaterialTheme.colorScheme.primary,
    maxColors: Int = 4,
    filter: Boolean = true,
): State<List<Color>> {
    return remember(image, fallback, maxColors, filter) {
        mutableStateOf(image.dominantColors(maxColors, fallback, filter))
    }
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available. Defaults to primary color.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme.
 */
@Composable
public fun calculateDominantColor(
    image: ImageBitmap,
    fallback: Color = MaterialTheme.colorScheme.primary,
    filter: Boolean = true,
): State<Color> {
    return remember(image, fallback, filter) {
        mutableStateOf(image.dominantColor(fallback, filter))
    }
}