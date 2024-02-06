package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.blend.Blend

/**
 * Blend two colors together. The hue, chroma, and tone of the original color will change.
 *
 * @param[from] The [Color] to blend from.
 * @param[to] The [Color] to blend to.
 * @param[amount] how much blending to perform; 0.0 >= and <= 1.0.
 * @return [from], blended towards [to]. Hue, chroma, and tone will change.
 */
public fun Blend.blend(from: Color, to: Color, amount: Float): Color {
    val blended = cam16Ucs(from.toArgb(), to.toArgb(), amount.toDouble())
    return Color(blended)
}

/**
 * Blend two colors together. The hue, chroma, and tone of the original color will change.
 *
 * @receiver The [Color] to blend from.
 * @param[to] The [Color] to blend to.
 * @param[amount] how much blending to perform; 0.0 >= and <= 1.0.
 * @return [this], blended towards [to]. Hue, chroma, and tone will change.
 */
public fun Color.blend(
    to: Color,
    amount: Float = 0.5f,
): Color = Blend.blend(this, to, amount)

/**
 * Blends hue from one [Color] into another. The chroma and tone of the original color are
 * maintained.
 *
 * @param[from] The [Color] to blend the hue from.
 * @param[to] The [Color] to blend the hue to.
 * @param[amount] how much blending to perform; 0.0 >= and <= 1.0.
 * @return [from], with a hue blended towards [to]. Chroma and tone are constant.
 */
public fun Blend.blendHue(from: Color, to: Color, amount: Float): Color {
    val blended = hctHue(from.toArgb(), to.toArgb(), amount.toDouble())
    return Color(blended)
}

/**
 * Blends this [Color] into another. The chroma and tone of the original color are maintained.
 *
 * @receiver The [Color] to blend the hue from.
 * @param[to] The [Color] to blend the hue to.
 * @param[amount] how much blending to perform; 0.0 >= and <= 1.0.
 * @return [this], with a hue blended towards [to]. Chroma and tone are constant.
 */
public fun Color.blendHue(
    to: Color,
    amount: Float = 0.5f,
): Color = Blend.blendHue(this, to, amount)