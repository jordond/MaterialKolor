package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal val InkShift: DpOffset = DpOffset(3.dp, 2.dp)

/**
 * Print a flat block of [color] in [shape], moved by [offset], with the paper showing through as grain.
 *
 * The block overprints whatever is already on the page, so two inks that overlap mix instead of covering each other.
 */
internal fun Modifier.ink(
    color: Color,
    colors: AppColors,
    shape: Shape = RectangleShape,
    offset: DpOffset = DpOffset.Zero,
): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip = Path().apply { addOutline(outline) }
        val dx = offset.x.toPx()
        val dy = offset.y.toPx()

        onDrawBehind {
            translate(left = dx, top = dy) {
                drawOutline(outline = outline, color = color, blendMode = colors.overprint)
                clipPath(clip) { drawGrain(colors.paper) }
            }
        }
    }

/**
 * Print [color] as a halftone screen. Each dot covers the share of its cell that [coverage] asks for at that point,
 * given as fractions of the width and the height, so a flat tint and a gradient are the same call.
 */
internal fun Modifier.halftone(
    color: Color,
    colors: AppColors,
    shape: Shape = RectangleShape,
    cell: Dp = 6.dp,
    offset: DpOffset = DpOffset.Zero,
    coverage: (x: Float, y: Float) -> Float,
): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip = Path().apply { addOutline(outline) }
        val dots = halftoneDots(size = size, cell = cell.toPx(), coverage = coverage)
        val dx = offset.x.toPx()
        val dy = offset.y.toPx()

        onDrawBehind {
            translate(left = dx, top = dy) {
                clipPath(clip) { drawPath(path = dots, color = color, blendMode = colors.overprint) }
            }
        }
    }

/**
 * Crop marks just outside each corner, the lines a printer trims the sheet along.
 */
internal fun Modifier.cropMarks(
    color: Color,
    length: Dp = 18.dp,
    gap: Dp = 8.dp,
): Modifier =
    drawBehind {
        val reach = length.toPx()
        val space = gap.toPx()
        val stroke = 1.dp.toPx()
        val corners = listOf(
            Offset(0f, 0f) to Offset(-1f, -1f),
            Offset(size.width, 0f) to Offset(1f, -1f),
            Offset(0f, size.height) to Offset(-1f, 1f),
            Offset(size.width, size.height) to Offset(1f, 1f),
        )

        for ((corner, away) in corners) {
            val horizontalStart = Offset(corner.x + away.x * space, corner.y)
            val verticalStart = Offset(corner.x, corner.y + away.y * space)
            drawLine(
                color = color,
                start = horizontalStart,
                end = horizontalStart + Offset(away.x * reach, 0f),
                strokeWidth = stroke,
            )
            drawLine(
                color = color,
                start = verticalStart,
                end = verticalStart + Offset(0f, away.y * reach),
                strokeWidth = stroke,
            )
        }
    }

/**
 * A highlighter pass behind the lower part of the content, slanted at both ends like a marker stroke.
 */
internal fun Modifier.highlighter(
    color: Color,
    colors: AppColors,
): Modifier =
    drawWithCache {
        val top = size.height * HIGHLIGHT_TOP
        val bottom = size.height * HIGHLIGHT_BOTTOM
        val slant = (bottom - top) * HIGHLIGHT_SLANT
        val overhang = (bottom - top) * HIGHLIGHT_OVERHANG
        val band = Path().apply {
            moveTo(-overhang, bottom)
            lineTo(-overhang + slant, top)
            lineTo(size.width + overhang + slant, top)
            lineTo(size.width + overhang, bottom)
            close()
        }

        onDrawBehind { drawPath(path = band, color = color, blendMode = colors.overprint) }
    }

/**
 * A row of dashes along the bottom edge, the tear line of a coupon.
 */
internal fun Modifier.perforation(color: Color): Modifier =
    drawBehind {
        val stroke = 1.5.dp.toPx()
        val y = size.height - stroke / 2
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
    }

/**
 * A registration target, the circle and cross printed in every ink so the passes can be lined up.
 */
internal fun DrawScope.drawRegistrationMark(
    color: Color,
    center: Offset,
    radius: Float,
) {
    val stroke = 1.5.dp.toPx()
    val reach = radius * REGISTRATION_REACH
    drawCircle(color = color, radius = radius, center = center, style = Stroke(stroke))
    drawLine(color, Offset(center.x - reach, center.y), Offset(center.x + reach, center.y), stroke)
    drawLine(color, Offset(center.x, center.y - reach), Offset(center.x, center.y + reach), stroke)
}

internal fun DrawScope.drawGrain(paper: Color) {
    drawRect(brush = GrainBrush, alpha = GRAIN_ALPHA, colorFilter = ColorFilter.tint(paper))
}

/**
 * Erase the grain specks from what is already drawn. Only useful inside an offscreen layer, where the paper is not
 * part of the destination.
 */
internal fun DrawScope.knockOutGrain() {
    drawRect(brush = GrainBrush, alpha = GRAIN_ALPHA, blendMode = BlendMode.DstOut)
}

/**
 * Lay a halftone screen over [size] at the usual 45 degree screen angle.
 */
internal fun halftoneDots(
    size: Size,
    cell: Float,
    coverage: (x: Float, y: Float) -> Float,
): Path {
    val dots = Path()
    if (size.width <= 0f || size.height <= 0f) return dots

    val center = Offset(size.width / 2, size.height / 2)
    val steps = ceil((hypot(size.width, size.height) / 2 + cell) / cell).toInt()
    val largest = cell * MAX_DOT_SHARE

    for (column in -steps..steps) {
        for (row in -steps..steps) {
            val u = column * cell
            val v = row * cell
            val x = center.x + u * SCREEN_COS - v * SCREEN_SIN
            val y = center.y + u * SCREEN_SIN + v * SCREEN_COS
            val isOnSheet = x in -cell..size.width + cell && y in -cell..size.height + cell
            if (!isOnSheet) continue

            val share = coverage(x / size.width, y / size.height).coerceIn(0f, 1f)
            if (share <= 0f) continue

            val radius = (cell * sqrt(share / PI.toFloat())).coerceAtMost(largest)
            dots.addOval(Rect(center = Offset(x, y), radius = radius))
        }
    }
    return dots
}

/**
 * Specks where the ink missed the paper. It is drawn once and tiled, and a fixed seed keeps every render identical.
 */
private val GrainBrush: ShaderBrush by lazy {
    val tile = ImageBitmap(GRAIN_TILE, GRAIN_TILE)
    val canvas = Canvas(tile)
    val random = Random(GRAIN_SEED)

    for ((width, count) in GrainSpecks) {
        val paint = Paint().apply {
            color = Color.White
            strokeWidth = width
            strokeCap = StrokeCap.Round
        }
        val points = List(count) { Offset(random.nextFloat() * GRAIN_TILE, random.nextFloat() * GRAIN_TILE) }
        canvas.drawPoints(PointMode.Points, points, paint)
    }

    ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

private val GrainSpecks = listOf(1f to 900, 2f to 260, 3f to 40)

private val SCREEN_COS = cos(PI / 4).toFloat()
private val SCREEN_SIN = sin(PI / 4).toFloat()

private const val GRAIN_TILE = 192
private const val GRAIN_SEED = 1984
private const val GRAIN_ALPHA = 0.55f
private const val MAX_DOT_SHARE = 0.72f
private const val HIGHLIGHT_TOP = 0.42f
private const val HIGHLIGHT_BOTTOM = 0.96f
private const val HIGHLIGHT_SLANT = 0.35f
private const val HIGHLIGHT_OVERHANG = 0.25f
private const val REGISTRATION_REACH = 1.6f
