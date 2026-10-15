package com.materialkolor.palette

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.kmpalette.DEFAULT_CACHE_SIZE
import com.kmpalette.loader.ImageBitmapLoader
import com.kmpalette.loader.rememberPainterLoader
import com.kmpalette.palette.graphics.Palette
import com.kmpalette.rememberPaletteState
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Create and remember the most suitable color for a UI theme in the image [loader] loads for [input].
 *
 * The image is loaded and quantized off the main thread, so the first composition sees [fallback]
 * and later ones see the scored color. Results are cached by [input], which means going back to an
 * image you already used costs nothing.
 *
 * kmpalette ships loaders for byte arrays, painters and drawable resources, and its extension
 * artifacts add base64 strings, network URLs and files.
 *
 * @param[T] The type of the input to load with [loader].
 * @param[loader] The [ImageBitmapLoader] that turns [input] into an image.
 * @param[input] The input to load, and the cache key of its result.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[cacheSize] The maximum number of palettes to cache. If 0, no caching will be done.
 * @param[coroutineContext] The [CoroutineContext] to generate the palette on.
 * @param[builder] A lambda applied to the [Palette.Builder] to customize the generation.
 * @return The most suitable color for a UI theme, or [fallback] until there is one.
 */
@Composable
public fun <T : Any> rememberThemeColor(
    loader: ImageBitmapLoader<T>,
    input: T,
    fallback: Color,
    filter: Boolean = true,
    cacheSize: Int = DEFAULT_CACHE_SIZE,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    builder: Palette.Builder.() -> Unit = {},
): Color {
    val state = rememberPaletteState(loader, cacheSize, coroutineContext, builder)

    LaunchedEffect(state, input) {
        state.generate(input)
    }

    return state.themeColor(fallback, filter)
}

/**
 * Create and remember the colors of the image [loader] loads for [input], ranked by how well they
 * would seed a UI theme.
 *
 * The image is loaded and quantized off the main thread, so the first composition sees only
 * [fallback] and later ones see the scored colors. Results are cached by [input], which means
 * going back to an image you already used costs nothing.
 *
 * @param[T] The type of the input to load with [loader].
 * @param[loader] The [ImageBitmapLoader] that turns [input] into an image.
 * @param[input] The input to load, and the cache key of its result.
 * @param[fallback] color to be returned if no other options available.
 * @param[desired] The number of colors to return.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[cacheSize] The maximum number of palettes to cache. If 0, no caching will be done.
 * @param[coroutineContext] The [CoroutineContext] to generate the palette on.
 * @param[builder] A lambda applied to the [Palette.Builder] to customize the generation.
 * @return Colors sorted by suitability for a UI theme, or a list holding only [fallback] until
 * there are any.
 */
@Composable
public fun <T : Any> rememberThemeColors(
    loader: ImageBitmapLoader<T>,
    input: T,
    fallback: Color,
    desired: Int = DEFAULT_DESIRED_COLORS,
    filter: Boolean = true,
    cacheSize: Int = DEFAULT_CACHE_SIZE,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    builder: Palette.Builder.() -> Unit = {},
): List<Color> {
    val state = rememberPaletteState(loader, cacheSize, coroutineContext, builder)

    LaunchedEffect(state, input) {
        state.generate(input)
    }

    return state.palette?.themeColors(fallback, desired, filter) ?: listOf(fallback)
}

/**
 * Create and remember the most suitable color for a UI theme in what [painter] draws.
 *
 * The painter is drawn into an image at its intrinsic size, so give it one. Everything else
 * matches [rememberThemeColor].
 *
 * @param[painter] The [Painter] to draw and read colors from.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @param[cacheSize] The maximum number of palettes to cache. If 0, no caching will be done.
 * @param[coroutineContext] The [CoroutineContext] to generate the palette on.
 * @param[builder] A lambda applied to the [Palette.Builder] to customize the generation.
 * @return The most suitable color for a UI theme, or [fallback] until there is one.
 */
@Composable
public fun rememberPainterThemeColor(
    painter: Painter,
    fallback: Color,
    filter: Boolean = true,
    cacheSize: Int = DEFAULT_CACHE_SIZE,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    builder: Palette.Builder.() -> Unit = {},
): Color =
    rememberThemeColor(
        loader = rememberPainterLoader(),
        input = painter,
        fallback = fallback,
        filter = filter,
        cacheSize = cacheSize,
        coroutineContext = coroutineContext,
        builder = builder,
    )
