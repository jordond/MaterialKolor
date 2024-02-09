package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.scheme.DynamicScheme

/**
 * Returns an [Color] that represents the color of this [DynamicColor] in the given [scheme].
 *
 * @param[scheme] Defines the conditions of the user interface, for example, whether or not it is
 * dark mode or light mode, and what the desired contrast level is.
 */
public fun DynamicColor.getColor(scheme: DynamicScheme): Color {
    return Color(getArgb(scheme))
}