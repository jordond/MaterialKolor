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

/**
 * The few small marks the app needs, drawn as strokes so there is no icon library to pull in.
 *
 * Each line is a list of points given as shares of the glyph's width and height, so a glyph scales to any size.
 */
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

    /** A plain circle, drawn on its own because it has no corners. */
    Ring(lines = emptyList()),
}

/**
 * Draws [glyph] in [color], scaled to whatever size [modifier] gives it.
 */
@Composable
internal fun Icon(
    glyph: Glyph,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // The color moves on every frame of a seed or mode fade, so it is read while drawing rather than captured below.
    // That way the path and stroke are only built again for a new glyph or a new size.
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

/**
 * Every line of this glyph as one path at [size], each line its own stretch of it.
 */
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

/** Stroke width as a share of the glyph's size, so a glyph keeps its weight at any size. */
private const val STROKE_SHARE = 0.12f

/** The ring's radius as a share of the glyph's size. */
private const val RING_SHARE = 0.34f
