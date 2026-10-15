package com.materialkolor.builder.preview.trips

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState

/**
 * The measures of the Trips app, the same in every library so the four apps line up.
 *
 * Shapes stay with each library, since corner radii are part of its look.
 */
internal object TripsLayout {
    /**
     * The small gap, between chips, buttons and the rows of a card.
     */
    val Gap: Dp = 8.dp

    /**
     * The gap around a pane and between the parts of a row.
     */
    val PaneGap: Dp = 12.dp

    /**
     * The gap between the sections of the open trip.
     */
    val SectionGap: Dp = 16.dp

    /**
     * The room a phone's list leaves under its last row for the floating new trip button.
     */
    val FabClearance: Dp = 88.dp

    /**
     * The side of a trip's thumbnail.
     */
    val ThumbSize: Dp = 52.dp

    /**
     * The height of the search field.
     */
    val SearchHeight: Dp = 48.dp

    /**
     * The space between the rail and the trip list.
     */
    val RailGap: Dp = 4.dp

    /**
     * Half the space between two trip rows.
     */
    val RowGap: Dp = 2.dp

    /**
     * The height of the scene at the top of the open trip.
     */
    val SceneHeight: Dp = 184.dp

    /**
     * The side of a numbered stop in the day's plan.
     */
    val StopSize: Dp = 36.dp

    /**
     * How far the trip's text sits in from the scene and the buttons.
     */
    val TextInset: Dp = 4.dp

    /**
     * The width of the trip list next to the open trip, null on a phone where the two share one column.
     */
    fun listWidth(deviceWidth: DeviceWidth): Dp? =
        when (deviceWidth) {
            DeviceWidth.Phone -> null
            DeviceWidth.Tablet -> 340.dp
            DeviceWidth.Desktop -> 360.dp
        }
}

/**
 * Where the open trip sits in [Trips], clamped so a stale index still opens one.
 */
internal fun openTrip(state: DemoAppState): Int = state.selectedItem.coerceIn(Trips.indices)

/**
 * Draws the trip scene stretched over the whole canvas, a [sky], a [sun] and the three [ridges],
 * farthest first.
 */
internal fun DrawScope.drawTripScene(
    sky: Color,
    sun: Color,
    ridges: List<Color>,
) {
    val scaleX = size.width / SceneSize.width
    val scaleY = size.height / SceneSize.height
    drawRect(sky)
    drawCircle(sun, radius = SceneSunRadius * scaleY, center = Offset(SceneSun.x * scaleX, SceneSun.y * scaleY))
    SceneRidges.forEachIndexed { index, ridge ->
        drawPath(ridgePath(ridge, scaleX, scaleY, size.height), ridges[index])
    }
}

/**
 * The ridge [points] scaled to the canvas, closed along its [bottom] edge.
 */
private fun ridgePath(
    points: FloatArray,
    scaleX: Float,
    scaleY: Float,
    bottom: Float,
): Path {
    val path = Path()
    path.moveTo(0f, bottom)
    for (index in points.indices step 2) path.lineTo(points[index] * scaleX, points[index + 1] * scaleY)
    path.lineTo(points[points.size - 2] * scaleX, bottom)
    path.close()
    return path
}
