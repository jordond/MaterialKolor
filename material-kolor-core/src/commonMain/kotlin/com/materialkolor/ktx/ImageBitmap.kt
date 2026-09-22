package com.materialkolor.ktx

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_QUANTIZE_MAX_COLORS = 128
private const val DEFAULT_DESIRED_COLORS = 4

/**
 * Quantize the colors in a [ImageBitmap] to a maximum of [maxColors] colors.
 *
 * The image is sampled down to [sampleArea] pixels first, so a photo costs the same as a
 * thumbnail. See [samplePixels] if you want the sampled pixels yourself.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[maxColors] The number of colors to divide the image into. A lower number of colors may be
 * returned.
 * @param[sampleArea] The most pixels to read from the image, or zero and below to read them all.
 * @return A map of colors to their frequency in the image.
 */
public fun QuantizerCelebi.quantize(
    image: ImageBitmap,
    maxColors: Int,
    sampleArea: Int = DEFAULT_SAMPLE_AREA,
): Map<Int, Int> = quantize(image.samplePixels(sampleArea).pixels, maxColors)

/**
 * Rank the colors in a [ImageBitmap] by their suitability for being used for a UI theme.
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available.
 * @param[maxColors] The number of colors to divide the image into.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[desired] The number of colors to return.
 * @param[sampleArea] The most pixels to read from the image, or zero and below to read them all.
 * @return Colors sorted by suitability for a UI theme. The most suitable color is the first item,
 * the least suitable is the last. There will always be at least one color returned. If none of
 * the input colors suit a theme, the list holds only [fallback].
 */
@Stable
public fun ImageBitmap.themeColors(
    fallback: Color,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    filter: Boolean = true,
    desired: Int = DEFAULT_DESIRED_COLORS,
    sampleArea: Int = DEFAULT_SAMPLE_AREA,
): List<Color> {
    val quantized = QuantizerCelebi.quantize(image = this, maxColors, sampleArea)
    return Score
        .score(
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
 * @param[sampleArea] The most pixels to read from the image, or zero and below to read them all.
 * @return The most suitable color for a UI theme.
 */
@Stable
public fun ImageBitmap.themeColor(
    fallback: Color,
    filter: Boolean = true,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    sampleArea: Int = DEFAULT_SAMPLE_AREA,
): Color = themeColors(fallback, maxColors, filter, sampleArea = sampleArea).first()

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme or `null`
 *
 * @receiver the [ImageBitmap] to extract colors from.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[maxColors] The number of colors to divide the image into.
 * @param[sampleArea] The most pixels to read from the image, or zero and below to read them all.
 * @return The most suitable color for a UI theme or `null` if no suitable color found.
 */
@Stable
public fun ImageBitmap.themeColorOrNull(
    filter: Boolean = true,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    sampleArea: Int = DEFAULT_SAMPLE_AREA,
): Color? {
    val quantized = QuantizerCelebi.quantize(
        image = this,
        maxColors = maxColors,
        sampleArea = sampleArea,
    )
    return Score
        .score(
            colorsToPopulation = quantized,
            desired = 1,
            fallbackColorArgb = null,
            filter = filter,
        ).firstOrNull()
        ?.let { colorInt -> Color(colorInt) }
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * The work happens on [Dispatchers.Default], so the first frame gets [fallback] and the extracted
 * colors arrive once they are ready.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available.
 * @param[maxColors] The number of colors to divide the image into.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[desired] The number of colors to return.
 * @param[sampleArea] The most pixels to read from the image, or zero and below to read them all.
 * @return The most suitable colors for a UI theme.
 */
@Stable
@Composable
public fun rememberThemeColors(
    image: ImageBitmap,
    fallback: Color,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    filter: Boolean = true,
    desired: Int = DEFAULT_DESIRED_COLORS,
    sampleArea: Int = DEFAULT_SAMPLE_AREA,
): List<Color> {
    var themeColors by remember { mutableStateOf(listOf(fallback)) }
    LaunchedEffect(image, fallback, filter, maxColors, sampleArea) {
        themeColors = withContext(Dispatchers.Default) {
            image.themeColors(fallback, maxColors, filter, desired, sampleArea)
        }
    }

    return themeColors
}

/**
 * Determine the most suitable color in a [ImageBitmap] for a UI theme.
 *
 * The work happens on [Dispatchers.Default], so the first frame gets [fallback] and the extracted
 * color arrives once it is ready.
 *
 * @param[image] the [ImageBitmap] to extract colors from.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[maxColors] The number of colors to divide the image into.
 * @param[sampleArea] The most pixels to read from the image, or zero and below to read them all.
 * @return The most suitable color for a UI theme.
 */
@Stable
@Composable
public fun rememberThemeColor(
    image: ImageBitmap,
    fallback: Color,
    filter: Boolean = true,
    maxColors: Int = DEFAULT_QUANTIZE_MAX_COLORS,
    sampleArea: Int = DEFAULT_SAMPLE_AREA,
): Color {
    var themeColor by remember { mutableStateOf(fallback) }
    LaunchedEffect(image, fallback, filter, maxColors, sampleArea) {
        themeColor = withContext(Dispatchers.Default) {
            image.themeColor(fallback, filter, maxColors, sampleArea)
        }
    }

    return themeColor
}
