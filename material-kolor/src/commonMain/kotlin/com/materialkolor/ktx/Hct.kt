package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct

/**
 * Convert a Compose [Color] to [Hct].
 *
 * @param[color] The color to convert.
 * @return The HCT representation of the color.
 */
public fun Hct.Companion.from(color: Color): Hct {
    return fromInt(color.toArgb())
}

/**
 * Convert HCT to a Compose [Color].
 *
 * @receiver The HCT to convert.
 * @return The Compose [Color] representation of the HCT.
 */
public fun Hct.toColor(): Color {
    return Color(toInt())
}

/**
 * Convert a Compose [Color] to [Hct].
 *
 * @receiver The color to convert.
 * @return The HCT representation of the color.
 */
public fun Color.toHct(): Hct {
    return Hct.from(this)
}