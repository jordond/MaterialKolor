package com.materialkolor.sample.unstyled.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A luggage tag. The start edge comes to a point with a hole punched through it.
 *
 * @param[tip] How far the point reaches in from the start edge.
 * @param[corner] The radius of the two end corners.
 * @param[hole] The radius of the hole.
 */
@Immutable
internal data class TagShape(
    val tip: Dp,
    val corner: Dp,
    val hole: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val tip = with(density) { tip.toPx() }
        val corner = with(density) { corner.toPx() }
        val hole = with(density) { hole.toPx() }
        val middle = size.height / 2
        val x = { start: Float -> if (layoutDirection == LayoutDirection.Ltr) start else size.width - start }

        val path = Path().apply {
            moveTo(x(tip), 0f)
            lineTo(x(size.width - corner), 0f)
            quadraticTo(x(size.width), 0f, x(size.width), corner)
            lineTo(x(size.width), size.height - corner)
            quadraticTo(x(size.width), size.height, x(size.width - corner), size.height)
            lineTo(x(tip), size.height)
            lineTo(x(0f), middle)
            close()
            addOval(Rect(center = Offset(x(tip), middle), radius = hole))
            fillType = PathFillType.EvenOdd
        }

        return Outline.Generic(path)
    }
}
