package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.LocalOverlayHost
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.headless.OverlayHost
import com.materialkolor.builder.kit.headless.OverlayHostState
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** The least share of each side's middle a ring has to cover, the bar the S5 probe holds it to. */
internal const val SideCoverageNeeded: Double = 0.5

/** The least contrast a ring pixel needs against what it covers (WCAG 2.4.13). */
private const val RingContrastNeeded = 3.0

/** How far a channel has to move for a pixel to count as drawn when something opens. */
private const val DrawnBy = 24f / 255f

/** How many pixels a tooltip bubble changes over its anchor at the least, at one pixel per dp. */
private const val BubblePixelsNeeded = 50

/** How far past its control a focus ring and the blend at its edge reach. */
internal val RingReach: Dp = FocusRingOffset + FocusRingWidth + 1.dp

/** How far over a ring [shouldShowABubbleAbove] looks for the bubble. */
private val BubbleReach: Dp = 48.dp

/**
 * The room [InThePage] keeps over its content. A bubble fits above it, and the anchor sits as low in
 * the window as a control a little way down a page. The light that casts a bubble's shadow stands
 * above the window, so the lower the bubble the further its shadow falls, and near the top of the
 * window even a bubble on the ring leaves it whole.
 */
private val BubbleRoom: Dp = 200.dp

/** How wide [InThePage] is at the least, so a bubble fits over a small anchor. */
private val PageWidth: Dp = 240.dp

/** The room [InThePage] keeps round the other sides of its content, for the ring. */
private val RingRoom: Dp = 8.dp

/** The overlays [InThePage] draws in the page, read once the frame is captured. */
internal class PageOverlays {
    var host: OverlayHostState? = null

    /** How many overlays are open in the page, such as a tooltip bubble. */
    val open: Int
        get() = host?.layers?.count { layer -> layer.open } ?: 0
}

/**
 * Draws [content] with its overlays in the page, the way the web draws them (D40), in a host of its
 * own with room over the content for a tooltip. The host clips at its edge like a preview pane's, so
 * it keeps room round the content for the ring too, and is wide enough for a bubble over a small
 * anchor.
 *
 * @param[overlays] Given the host, so a test can tell a bubble opened.
 */
@Composable
internal fun InThePage(
    overlays: PageOverlays,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalOverlaysInTree provides true) {
        OverlayHost(nested = true) {
            val host = LocalOverlayHost.current
            SideEffect { overlays.host = host }
            Box(
                modifier = Modifier
                    .widthIn(min = PageWidth)
                    .padding(start = RingRoom, top = BubbleRoom, end = RingRoom, bottom = RingRoom),
            ) { content() }
        }
    }
}

/**
 * How much of the middle half of each side of [around] the ring covers at 3 to 1, the way the S5
 * probe measures it. A row of the left or the right side counts when a ring pixel lies on it within
 * that side's band, and a column of the top or the bottom side the same way. The bands are the ones
 * [shouldRingEverySide] looks in, so a side reads zero where the ring is cut or sits under something.
 */
internal fun RingCapture.sideCoverage(
    reach: Dp = 8.dp,
    around: Rect = focused,
): Map<String, Double> {
    val box = around
    val out = reach.value * density
    val rows = pixelsWithin(box.top + box.height / 4, box.bottom - box.height / 4)
    val columns = pixelsWithin(box.left + box.width / 4, box.right - box.width / 4)
    val left = (box.left - out)..(box.left + box.width / 4)
    val right = (box.right - box.width / 4)..(box.right + out)
    val top = (box.top - out)..(box.top + box.height / 4)
    val bottom = (box.bottom - box.height / 4)..(box.bottom + out)
    return mapOf(
        "top" to share(columns, pixels.filter { point -> point.y + 0.5f in top }.map { point -> point.x }),
        "right" to share(rows, pixels.filter { point -> point.x + 0.5f in right }.map { point -> point.y }),
        "bottom" to share(columns, pixels.filter { point -> point.y + 0.5f in bottom }.map { point -> point.x }),
        "left" to share(rows, pixels.filter { point -> point.x + 0.5f in left }.map { point -> point.y }),
    )
}

/** The side coverage in the probe's order, top, right, bottom and left, with the lowest ratio drawn. */
internal fun RingCapture.coverageLine(
    reach: Dp = 8.dp,
    around: Rect = focused,
): String {
    val sides = sideCoverage(reach, around).values.joinToString("/") { share -> "${(share * 100).roundToInt()}" }
    return "$sides, $summary"
}

/**
 * Checks that the ring covers at least [least] of the middle of every side of [around] at 3 to 1,
 * the S5 probe's side rule. [shouldRingEverySide] lets a side through on one pixel, and this does
 * not, so a ring cut at a clip or shaded by a bubble fails here.
 */
internal fun RingCapture.shouldCoverEverySide(
    least: Double = SideCoverageNeeded,
    reach: Dp = 8.dp,
    around: Rect = focused,
) {
    val short = sideCoverage(reach, around).filterValues { share -> share < least }.keys
    withClue("sides ${coverageLine(reach, around)} around $around") { short.shouldBeEmpty() }
}

/**
 * [shouldRingAllTheWayRound] for a ring drawn outside [inner], which leaves out what focus draws
 * inside it in the ring's colour, such as a field's caret over the edge of its text.
 */
internal fun RingCapture.shouldRingAllTheWayRoundOutside(inner: Rect) {
    val matched = candidates.filter { point ->
        if (inner.contains(Offset(point.x + 0.5f, point.y + 0.5f))) return@filter false
        val ring = ringAt(point) ?: return@filter false
        contrast(ring, before[point.x, point.y]) < RingContrastNeeded
    }
    val first = matched.firstOrNull()
    val where = if (first == null) "" else ", first at (${first.x}, ${first.y})"
    withClue("$summary, ${matched.size} outside $inner on a ground under 3:1$where") { matched.shouldBeEmpty() }
}

/**
 * Checks that a bubble opened in the page and drew over the focused node, above its ring, so a test
 * of the ring under a bubble has a bubble to test. Only the node's own columns are looked at, where
 * a bubble centred over it lands.
 */
internal fun RingCapture.shouldShowABubbleAbove(overlays: PageOverlays) {
    withClue("overlays open in the page") { overlays.open shouldBeGreaterThanOrEqual 1 }
    val clear = focused.top - RingReach.value * density
    val rows = pixelsWithin(clear - BubbleReach.value * density, clear)
    val columns = pixelsWithin(focused.left, focused.right)
    var drawn = 0
    for (y in rows.first.coerceAtLeast(0)..minOf(rows.last, after.height - 1)) {
        for (x in columns.first.coerceAtLeast(0)..minOf(columns.last, after.width - 1)) {
            val was = before[x, y]
            val now = after[x, y]
            val moved = abs(now.red - was.red) > DrawnBy ||
                abs(now.green - was.green) > DrawnBy ||
                abs(now.blue - was.blue) > DrawnBy
            if (moved) drawn++
        }
    }
    withClue("$drawn pixels drawn over the ring") { drawn shouldBeGreaterThanOrEqual BubblePixelsNeeded }
}

/** The whole pixels whose centres lie between [from] and [to]. */
private fun pixelsWithin(
    from: Float,
    to: Float,
): IntRange = ceil(from - 0.5f).toInt()..floor(to - 0.5f).toInt()

/** The share of [line] that [hits] lands on. */
private fun share(
    line: IntRange,
    hits: List<Int>,
): Double {
    if (line.isEmpty()) return 0.0
    val covered = hits.toSet().count { hit -> hit in line }
    return covered.toDouble() / (line.last - line.first + 1)
}
