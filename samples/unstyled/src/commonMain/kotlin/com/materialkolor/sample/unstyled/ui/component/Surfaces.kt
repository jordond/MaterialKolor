package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShadowTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.brush
import com.materialkolor.sample.unstyled.theme.shadow
import com.materialkolor.sample.unstyled.theme.shape

@Composable
internal fun Card(
    modifier: Modifier = Modifier,
    shadow: Shadow = ShadowTokens.resting.shadow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        content = content,
        modifier = modifier.raised(shape = ShapeTokens.card.shape, shadow = shadow),
    )
}

/**
 * Lifts the element off the page with a drop shadow and the raised gradient.
 */
@Composable
internal fun Modifier.raised(
    shape: Shape,
    shadow: Shadow = ShadowTokens.resting.shadow,
): Modifier =
    dropShadow(shape = shape, shadow = shadow)
        .clip(shape)
        .background(GradientTokens.raised.brush)

/**
 * Presses the element into the page with the sunken gradient and an inner shadow.
 */
@Composable
internal fun Modifier.sunken(shape: Shape): Modifier =
    clip(shape)
        .background(GradientTokens.sunken.brush)
        .innerShadow(shape = shape, shadow = ShadowTokens.inset.shadow)
