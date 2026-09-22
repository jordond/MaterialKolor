package com.materialkolor.palette

import androidx.compose.ui.graphics.Color
import com.kmpalette.DominantColorState
import com.kmpalette.PaletteState

/**
 * The scored seed color of the last successful generation of this [PaletteState].
 *
 * Reading this in a composition subscribes to the state, so the composition runs again when the
 * generation finishes.
 *
 * @receiver the [PaletteState] to read.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme, or `null` while loading, after an error, or when
 * no swatch suits a theme.
 */
public fun PaletteState<*>.themeColorOrNull(filter: Boolean = true): Color? = palette?.themeColorOrNull(filter)

/**
 * The scored seed color of the last successful generation of this [PaletteState], or [fallback].
 *
 * Reading this in a composition subscribes to the state, so the composition runs again when the
 * generation finishes.
 *
 * @receiver the [PaletteState] to read.
 * @param[fallback] color to be returned if no other options available.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme, or [fallback] while loading, after an error, or
 * when no swatch suits a theme.
 */
public fun PaletteState<*>.themeColor(
    fallback: Color,
    filter: Boolean = true,
): Color = palette?.themeColor(fallback, filter) ?: fallback

/**
 * The scored seed color of the last successful update of this [DominantColorState].
 *
 * [DominantColorState] already hands you `color` and `onColor`, which are the highest population
 * swatch it found. This is the scored view of the same result, so it is the color a theme would
 * be seeded with rather than the color that covers the most pixels.
 *
 * @receiver the [DominantColorState] to read.
 * @param[filter] whether to filter out undesirable combinations.
 * @return The most suitable color for a UI theme, or `null` while loading, after an error, or when
 * no swatch suits a theme.
 */
public fun DominantColorState<*>.themeColorOrNull(filter: Boolean = true): Color? =
    result?.paletteOrNull?.themeColorOrNull(filter)
