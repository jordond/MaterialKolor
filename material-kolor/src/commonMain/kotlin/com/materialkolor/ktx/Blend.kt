package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct

/**
 * Blend the design color's HCT hue towards the key color's HCT hue, in a way that leaves the
 * original color recognizable and recognizably shifted towards the key color.
 *
 * @param[fromColor] [Color] to blend from.
 * @param[toColor] [Color] to blend towards.
 */
public fun Blend.harmonize(
    fromColor: Color,
    toColor: Color,
): Color {
    val fromHct = Hct.from(fromColor)
    val toHct = Hct.from(toColor)
    return harmonize(fromHct, toHct).toColor()
}