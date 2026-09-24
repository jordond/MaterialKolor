package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class Glyph(
    val lines: List<List<Offset>>,
) {
    Check(
        lines = listOf(
            listOf(Offset(0.2f, 0.53f), Offset(0.42f, 0.74f), Offset(0.8f, 0.3f)),
        ),
    ),
    Cross(
        lines = listOf(
            listOf(Offset(0.25f, 0.25f), Offset(0.75f, 0.75f)),
            listOf(Offset(0.75f, 0.25f), Offset(0.25f, 0.75f)),
        ),
    ),
    Plus(
        lines = listOf(
            listOf(Offset(0.5f, 0.2f), Offset(0.5f, 0.8f)),
            listOf(Offset(0.2f, 0.5f), Offset(0.8f, 0.5f)),
        ),
    ),

    Ring(lines = emptyList()),
}

@Composable
internal fun Icon(
    glyph: Glyph,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val currentColor by rememberUpdatedState(color)
    Spacer(
        modifier = modifier.drawWithCache {
            val stroke = Stroke(
                width = size.minDimension * STROKE_SHARE,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )

            if (glyph == Glyph.Ring) {
                val radius = size.minDimension * RING_SHARE
                onDrawBehind { drawCircle(color = currentColor, radius = radius, style = stroke) }
            } else {
                val path = glyph.toPath(size)
                onDrawBehind { drawPath(path = path, color = currentColor, style = stroke) }
            }
        },
    )
}

private fun Glyph.toPath(size: Size): Path {
    val path = Path()
    for (line in lines) {
        line.forEachIndexed { index, share ->
            val x = size.width * share.x
            val y = size.height * share.y
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }
    return path
}

private const val STROKE_SHARE = 0.12f
private const val RING_SHARE = 0.34f
