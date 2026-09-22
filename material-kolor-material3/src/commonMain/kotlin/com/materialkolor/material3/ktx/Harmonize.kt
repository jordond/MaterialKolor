package com.materialkolor.material3.ktx

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.blend.Blend
import com.materialkolor.ktx.harmonize

/**
 * Returns the [Color] of the given [color] harmonized with the [ColorScheme.primary].
 *
 * If you want to adjust the saturation of [color] to match the saturation of [ColorScheme.primary],
 * set [matchSaturation] to `true`.
 *
 * @param[color] [Color] to harmonize with the primary color.
 * @return [Color] harmonized color
 */
@Stable
public fun ColorScheme.harmonizeWithPrimary(
    color: Color,
    matchSaturation: Boolean = false,
): Color = Blend.harmonize(color, primary, matchSaturation)
