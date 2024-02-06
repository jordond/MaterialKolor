package com.materialkolor.ktx

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct

/**
 * Blend the design color's HCT hue towards the key color's HCT hue, in a way that leaves the
 * original color recognizable and recognizably shifted towards the key color.
 *
 * If you want to adjust the saturation of [fromColor] to match the saturation of [toColor], set
 * [matchSaturation] to `true`.
 *
 * @param[fromColor] [Color] to blend from.
 * @param[toColor] [Color] to blend towards.
 * @param[matchSaturation] Whether to match the saturation of the [fromColor] to the [toColor].
 */
@Stable
public fun Blend.harmonize(
    fromColor: Color,
    toColor: Color,
    matchSaturation: Boolean = false,
): Color {
    val from =
        if (matchSaturation) fromColor.matchSaturation(toColor)
        else fromColor

    val fromHct = Hct.from(from)
    val toHct = Hct.from(toColor)
    return harmonize(fromHct, toHct).toColor()
}

/**
 * Blend the color's HCT hue towards the [other] color's HCT hue, in a way that leaves the
 * original color recognizable and recognizably shifted towards the key color.
 *
 * If you want to adjust the saturation of [Color] to match the saturation of [other], set
 * [matchSaturation] to `true`.
 *
 * @receiver[Color] to blend from.
 * @param[other] [Color] to blend towards.
 * @param[matchSaturation] Whether to match the saturation of the [from] to the [toColor].
 * @return [Color] blended color.
 */
@Stable
public fun Color.harmonize(other: Color, matchSaturation: Boolean = false): Color {
    return Blend.harmonize(fromColor = this, other, matchSaturation)
}

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
public fun ColorScheme.harmonizeWithPrimary(color: Color, matchSaturation: Boolean = false): Color {
    return Blend.harmonize(color, primary, matchSaturation)
}