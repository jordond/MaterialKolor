package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val drawn by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = TICK_MILLIS),
    )

    Box(
        modifier = modifier
            .size(26.dp)
            .then(if (state.isHovered) Modifier.ink(colors.highlight, colors) else Modifier)
            .border(Rule, colors.ink)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            ).pointerHoverIcon(PointerIcon.Hand)
            .focusRing(state = state, color = colors.blue, shape = RectangleShape)
            .drawWithCache {
                val tick = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.5f)
                    lineTo(size.width * 0.42f, size.height * 0.88f)
                    lineTo(size.width * 1.25f, size.height * -0.3f)
                }
                val measure = PathMeasure().apply { setPath(tick, forceClosed = false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * drawn, partial, startWithMoveTo = true)
                val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val shift = Offset(InkShift.x.toPx(), InkShift.y.toPx())

                onDrawWithContent {
                    drawContent()
                    if (drawn == 0f) return@onDrawWithContent
                    drawPath(path = partial, color = colors.pink, style = stroke, blendMode = colors.overprint)
                    translate(shift.x, shift.y) {
                        drawPath(path = partial, color = colors.blue, style = stroke, blendMode = colors.overprint)
                    }
                }
            },
    )
}

private const val TICK_MILLIS = 220
