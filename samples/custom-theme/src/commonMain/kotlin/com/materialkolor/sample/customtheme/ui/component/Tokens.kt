package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Each style sets size, weight and line height only, and the color comes from `AppColors` where the text is drawn.
 */
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
    /** Pieces that sit inside another control, like a segmented option, a chip or an icon button. */
    val Inner: RoundedCornerShape = RoundedCornerShape(8.dp)

    val Control: RoundedCornerShape = RoundedCornerShape(10.dp)

    val Card: RoundedCornerShape = RoundedCornerShape(12.dp)
}

/** The height every control shares, so a field, a picker and a button line up in one row. */
internal val ControlHeight: Dp = 36.dp

@Immutable
internal data class Accent(
    val container: Color,
    val content: Color,
)
