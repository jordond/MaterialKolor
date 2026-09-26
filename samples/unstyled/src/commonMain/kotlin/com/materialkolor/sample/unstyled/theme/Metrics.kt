package com.materialkolor.sample.unstyled.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object Spacing {
    val XSmall: Dp = 4.dp
    val Tight: Dp = 6.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val XLarge: Dp = 24.dp
    val XXLarge: Dp = 32.dp
}

internal object Shapes {
    val Small: Shape = RoundedCornerShape(6.dp)
    val Control: Shape = RoundedCornerShape(8.dp)
    val Tab: Shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val Card: Shape = RoundedCornerShape(12.dp)
    val Pill: Shape = RoundedCornerShape(percent = 50)
    val Round: Shape = CircleShape
}

internal val ContentMaxWidth: Dp = 720.dp
internal val ControlHeight: Dp = 40.dp
internal val IconSize: Dp = 18.dp
