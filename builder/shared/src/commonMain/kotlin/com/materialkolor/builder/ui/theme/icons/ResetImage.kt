package com.materialkolor.builder.ui.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.theme.AppIcons

val AppIcons.ResetImage: ImageVector
    get() {
        if (resetImage != null) {
            return resetImage!!
        }
        resetImage = ImageVector.Builder(
            name = "Reset_image",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(120f, 360f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(134f)
                quadToRelative(50f, -62f, 122.5f, -98f)
                reflectiveQuadTo(480f, 120f)
                quadToRelative(118f, 0f, 210.5f, 67f)
                reflectiveQuadTo(820f, 360f)
                horizontalLineToRelative(-87f)
                quadToRelative(-34f, -72f, -101f, -116f)
                reflectiveQuadToRelative(-152f, -44f)
                quadToRelative(-57f, 0f, -107.5f, 21f)
                reflectiveQuadTo(284f, 280f)
                horizontalLineToRelative(76f)
                verticalLineToRelative(80f)
                close()
                moveToRelative(120f, 360f)
                horizontalLineToRelative(480f)
                lineTo(570f, 520f)
                lineTo(450f, 680f)
                lineToRelative(-90f, -120f)
                close()
                moveTo(200f, 880f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 800f)
                verticalLineToRelative(-320f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(320f)
                horizontalLineToRelative(560f)
                verticalLineToRelative(-320f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(320f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(760f, 880f)
                close()
            }
        }.build()
        return resetImage!!
    }

private var resetImage: ImageVector? = null
