package com.materialkolor.builder.preview.inspect

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.preview.split.PaneSide
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The copy of the preview at [x] across a layout [width] wide. The start copy runs from the start
 * edge to the handle at [fraction] and the end copy on past it. One copy alone is the start copy.
 */
internal fun paneSideAt(
    x: Float,
    width: Int,
    shown: PreviewMode,
    fraction: Float,
    layoutDirection: LayoutDirection,
): PaneSide {
    if (shown != PreviewMode.Split) return PaneSide.Start
    val end = if (layoutDirection == LayoutDirection.Ltr) x >= width * fraction else x < width * (1f - fraction)
    return if (end) PaneSide.End else PaneSide.Start
}

/** Whether a copy on [side] wears the dark scheme while the canvas shows [shown]. A split is light then dark. */
internal fun isDark(
    side: PaneSide,
    shown: PreviewMode,
): Boolean =
    when (shown) {
        PreviewMode.Light -> false
        PreviewMode.Split -> side == PaneSide.End
        PreviewMode.Dark -> true
    }

/**
 * Whether [x] falls in the split handle's strip, [thickness] wide and centred on [fraction] of
 * [width] from the start edge, the way the handle places itself.
 */
internal fun onHandle(
    x: Float,
    width: Int,
    fraction: Float,
    thickness: Int,
    layoutDirection: LayoutDirection,
): Boolean {
    val along = if (layoutDirection == LayoutDirection.Ltr) x else width - x
    val start = (width * fraction - thickness / 2f).roundToInt().coerceIn(0, max(0, width - thickness))
    return along >= start && along < start + thickness
}

/**
 * Where a [card] goes beside an [element] in a layout of size [room], both in its own space.
 *
 * It sits [gap] past the side with more room, level with the element's top, or on the other side
 * when that one is too narrow. With no room on either side it goes below, or above when below is too
 * short. It is then held inside the room.
 */
internal fun cardSpot(
    element: Rect,
    card: IntSize,
    room: IntSize,
    gap: Int,
): IntOffset {
    val after = element.right + gap
    val before = element.left - gap - card.width
    val fitsAfter = after + card.width <= room.width
    val fitsBefore = before >= 0f
    val x: Float
    val y: Float
    if (fitsAfter || fitsBefore) {
        val moreAfter = room.width - element.right >= element.left
        x = if (fitsAfter && (moreAfter || !fitsBefore)) after else before
        y = element.top
    } else {
        val below = element.bottom + gap
        x = element.left
        y = if (below + card.height <= room.height) below else element.top - gap - card.height
    }
    return IntOffset(
        x.roundToInt().coerceIn(0, max(0, room.width - card.width)),
        y.roundToInt().coerceIn(0, max(0, room.height - card.height)),
    )
}
