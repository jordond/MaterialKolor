package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun Text(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textDecoration = textDecoration),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * [text] printed twice, once per ink, with the second pass off register by [shift]. Where the passes overlap the
 * inks overprint into a third color.
 */
@Composable
internal fun OverprintText(
    text: String,
    style: TextStyle,
    first: Color,
    second: Color,
    modifier: Modifier = Modifier,
    shift: DpOffset = InkShift,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val layout = remember(text, style, measurer) { measurer.measure(text = text, style = style) }
    val size = with(density) { DpSize(layout.size.width.toDp() + shift.x, layout.size.height.toDp() + shift.y) }

    Spacer(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = colors.overprint
            }.drawBehind {
                drawText(textLayoutResult = layout, color = first)
                drawText(
                    textLayoutResult = layout,
                    color = second,
                    topLeft = Offset(shift.x.toPx(), shift.y.toPx()),
                    blendMode = colors.overprint,
                )
                knockOutGrain()
            },
    )
}

@Composable
internal fun GroupLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = AppType.Caption,
        color = LocalAppColors.current.inkSoft,
        modifier = modifier,
    )
}
