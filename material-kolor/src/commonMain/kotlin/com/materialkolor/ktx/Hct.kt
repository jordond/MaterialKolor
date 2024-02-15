package com.materialkolor.ktx

import androidx.compose.runtime.Stable
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
@Stable
public fun Hct.toColor(): Color {
    return Color(toInt())
}

/**
 * Convert a list of HCT to a list of Compose [Color].
 *
 * @receiver The list of HCT to convert.
 * @return The list of Compose [Color] representation of the HCT.
 */
@Stable
public fun List<Hct>.toColors(): List<Color> {
    return map { it.toColor() }
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

/**
 * Convert a list of Compose [Color] to a list of HCT.
 *
 * @receiver The list of colors to convert.
 * @return The list of HCT representation of the colors.
 */
public fun List<Color>.toHcts(): List<Hct> {
    return map { it.toHct() }
}