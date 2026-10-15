package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal object AppType {
    val Masthead: TextStyle = TextStyle(
        fontSize = 120.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 120.sp,
        letterSpacing = (-0.05).em,
    )

    val Display: TextStyle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    )

    val Heading: TextStyle = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).em,
    )

    val Body: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
    )

    val Label: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 16.sp,
        letterSpacing = 0.06.em,
    )

    val Caption: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 14.sp,
        letterSpacing = 0.14.em,
    )
}

internal val ControlHeight: Dp = 42.dp

internal val Rule: Dp = 2.dp

@Immutable
internal data class Accent(
    val container: Color,
    val content: Color,
)
