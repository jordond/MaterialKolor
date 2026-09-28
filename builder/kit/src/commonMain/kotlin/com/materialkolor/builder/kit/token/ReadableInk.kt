package com.materialkolor.builder.kit.token

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct

/**
 * The ink that reads on [color], black on a light color and white on a dark one.
 *
 * The line falls at HCT tone 50, so black from tone 50 up and white under it. It is how the builder
 * writes on a color the user picked, such as a swatch or a button filled with the picked color.
 */
public fun readableInk(color: Color): Color =
    if (Hct.fromInt(color.toArgb()).tone >= BlackInkTone) Color.Black else Color.White

/**
 * The lowest tone that takes black ink.
 */
private const val BlackInkTone: Double = 50.0
