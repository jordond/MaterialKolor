package com.materialkolor.sample.unstyled.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken

internal object ShapeTokens {
    val shapes: ThemeProperty<Shape> = ThemeProperty("shapes")

    val control: ThemeToken<Shape> = ThemeToken("control")
    val card: ThemeToken<Shape> = ThemeToken("card")
    val dialog: ThemeToken<Shape> = ThemeToken("dialog")
    val pill: ThemeToken<Shape> = ThemeToken("pill")
    val tag: ThemeToken<Shape> = ThemeToken("tag")
}

internal val TasksShapes: Map<ThemeToken<Shape>, Shape> = mapOf(
    ShapeTokens.control to RoundedCornerShape(14.dp),
    ShapeTokens.card to RoundedCornerShape(20.dp),
    ShapeTokens.dialog to RoundedCornerShape(28.dp),
    ShapeTokens.pill to RoundedCornerShape(percent = 50),
    ShapeTokens.tag to TagShape(tip = 10.dp, corner = 6.dp, hole = 2.5.dp),
)

internal val ThemeToken<Shape>.shape: Shape
    @Composable
    get() = Theme[ShapeTokens.shapes][this]
