package com.materialkolor.builder.kit.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Path
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Where the reveal circle grows from.
 *
 * A caller that has no button to point at gives [RevealStyle.Circle] an [Offset.Unspecified] origin,
 * and the circle then grows out of the middle of the host.
 */
internal fun revealCenter(
    origin: Offset,
    size: Size,
): Offset = if (origin.isSpecified) origin else size.center

/**
 * How far the circle has to grow before it covers the whole host, which is the distance from
 * [center] to the corner furthest from it.
 */
internal fun farthestCorner(
    center: Offset,
    size: Size,
): Float {
    val dx = max(center.x, size.width - center.x)
    val dy = max(center.y, size.height - center.y)
    return sqrt(dx * dx + dy * dy)
}

/**
 * Turns [path] into a circle of [radius] around [center].
 *
 * The host keeps one path and rewrites it every frame, so the reveal allocates nothing while it
 * runs.
 */
internal fun circlePath(
    path: Path,
    center: Offset,
    radius: Float,
): Path {
    path.reset()
    path.addOval(Rect(center = center, radius = radius))
    return path
}
