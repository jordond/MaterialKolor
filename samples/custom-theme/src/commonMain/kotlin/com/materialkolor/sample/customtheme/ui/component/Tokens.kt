package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AppType {
    val Title: TextStyle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 34.sp,
    )

    val Heading: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    )

    val Body: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val BodyStrong: TextStyle = Body.copy(fontWeight = FontWeight.Medium)

    val Label: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
    )

    val Caption: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    )
}

internal object AppShapes {
    val Inner: RoundedCornerShape = RoundedCornerShape(8.dp)
    val Control: RoundedCornerShape = RoundedCornerShape(10.dp)
    val Card: RoundedCornerShape = RoundedCornerShape(12.dp)
}

internal val ControlHeight: Dp = 36.dp

@Immutable
internal data class Accent(
    val container: Color,
    val content: Color,
)
