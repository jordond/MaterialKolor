package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The frame [tabOntoRing] captures, the control on the panel with room for an outset ring.
 */
private const val RingFrameTag = "ring-frame"

/**
 * The button [tabOntoRing] presses Tab from.
 */
private const val RingStartTag = "ring-start"

/**
 * How far a channel has to move for S5 to count the pixel as changed.
 */
private const val MovedBy = 24f / 255f

/**
 * How close to the ring colour a pixel has to sit, on every channel, to count as ring.
 */
private const val NearRing = 0.02f

/**
 * The least contrast a ring pixel needs against what was there before (WCAG 2.4.13).
 */
private const val RingContrast = 3.0

/**
 * How many ring pixels a control needs before it counts as ringed.
 */
internal const val RingPixelsNeeded: Int = 100

/**
 * How strong Material3's focus state layer is over the content colour.
 */
private const val MaterialFocusLayer = 0.1f

/**
 * The document [tabOntoRing] resolves unless a test asks for another.
 */
internal val RingDocument: ThemeDocument = ThemeDocument(seed = Argb(0x6750A4))

/**
 * The frame around a control before and after Tab moved focus onto it.
 *
 * @property[rings] The colours a ring pixel may show, the skin's focus colour first. Material3 adds
 * that colour under its own focus layer, since a ring inside a tab sits beneath it.
 * @property[focused] Where the focused node sits in the frame, in pixels.
 * @property[density] How many pixels the capture drew per dp.
 * @property[region] Where ring pixels are looked for, in pixels, or the whole frame when null.
 */
internal class RingCapture(
    val rings: List<Color>,
    val before: PixelMap,
    val after: PixelMap,
    val focused: Rect,
    val density: Float,
    val region: Rect? = null,
) {
    /**
     * Every pixel that moved and now sits on a ring colour, whatever its contrast.
     */
    val candidates: List<IntOffset> = buildList {
        for (y in 0 until minOf(before.height, after.height)) {
            for (x in 0 until minOf(before.width, after.width)) {
                if (region != null && !region.contains(Offset(x + 0.5f, y + 0.5f))) continue
                val now = after[x, y]
                if (moved(before[x, y], now) && rings.any { ring -> near(now, ring) }) add(IntOffset(x, y))
            }
        }
    }

    /**
     * The same capture with ring pixels looked for only within [reach] of the focused node. A tooltip
     * that opens with focus can use the focus colour, as Fluent's does, and this leaves it out.
     */
    fun nearFocused(reach: Dp): RingCapture =
        RingCapture(rings, before, after, focused, density, focused.inflate(reach.value * density))

    /**
     * The ring colour the pixel at [point] shows, or null when it shows none.
     */
    fun ringAt(point: IntOffset): Color? = rings.firstOrNull { ring -> near(after[point.x, point.y], ring) }

    /**
     * The candidates at 3 to 1 or better against their unfocused colour, which is the ring.
     */
    val pixels: List<IntOffset> = candidates.filter { point -> ratioAt(point) >= RingContrast }

    /**
     * The contrast of the pixel at [point] against its unfocused colour.
     */
    fun ratioAt(point: IntOffset): Double = contrast(after[point.x, point.y], before[point.x, point.y])

    /**
     * How far Material3's focus layer pulls the ring off the focus colour, on the channel it moves
     * most. Zero where only the focus colour counts.
     */
    val layerDrift: Float
        get() {
            val layered = rings.getOrNull(1) ?: return 0f
            val ring = rings.first()
            return maxOf(abs(layered.red - ring.red), abs(layered.green - ring.green), abs(layered.blue - ring.blue))
        }

    /**
     * The box around every ring coloured pixel, where the ring was drawn whatever its contrast.
     */
    val ringBounds: Rect
        get() {
            if (candidates.isEmpty()) return Rect.Zero
            return Rect(
                left = candidates.minOf { point -> point.x }.toFloat(),
                top = candidates.minOf { point -> point.y }.toFloat(),
                right = candidates.maxOf { point -> point.x } + 1f,
                bottom = candidates.maxOf { point -> point.y } + 1f,
            )
        }

    /**
     * How many pixels the ring drew and how many of them stand out, for a failure message.
     */
    val summary: String
        get() {
            val lowest = candidates.minOfOrNull(::ratioAt) ?: 0.0
            return "${pixels.size} of ${candidates.size} ring pixels at 3:1, lowest ${"%.2f".format(lowest)}"
        }
}

/**
 * Draws [content] on the panel of [skin] after a plain button, focuses the button, then presses
 * Tab [presses] times so focus lands on the control, and captures the frame on both sides of the
 * last press.
 *
 * @param[document] The theme the skin uses, for a test that needs a seed or style far from the default.
 * @param[density] Pixels per dp. A small round control draws few whole ring pixels at 1, since the
 * edges of a curve are blended, so its test can draw at 2 instead of lowering the pixel line.
 * @param[ringColors] The colours a ring pixel may show, for a control whose own focus outline is its
 * ring, such as a field's. Unset, the skin's focus colour counts, with Material3's layer over it.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tabOntoRing(
    skin: Skin,
    document: ThemeDocument = RingDocument,
    density: Float = 1f,
    presses: Int = 1,
    ringColors: (@Composable () -> List<Color>)? = null,
    content: @Composable () -> Unit,
): RingCapture {
    require(presses >= 1) { "Tab has to be pressed at least once, got $presses" }
    var rings = emptyList<Color>()
    setContent {
        val result = remember(document) { ThemeResolver().resolve(document) }
        CompositionLocalProvider(LocalMotionFrozen provides true, LocalDensity provides Density(density)) {
            BuilderTheme(skin, result, isDark = false, reducedMotion = false) {
                ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                    val tokens = LocalBuilderTokens.current
                    val focus = tokens.focus
                    val layered = MaterialTheme.colorScheme.primary
                        .copy(alpha = MaterialFocusLayer)
                        .compositeOver(focus)
                    val own = ringColors?.invoke()
                    rings = when {
                        own != null -> own
                        skin.library == Library.Material3 -> listOf(focus, layered)
                        else -> listOf(focus)
                    }
                    Column {
                        OverlayTestButton(RingStartTag)
                        Box(Modifier.testTag(RingFrameTag).background(tokens.panel).padding(8.dp)) { content() }
                    }
                }
            }
        }
    }
    val start = onNodeWithTag(RingStartTag)
    start.requestFocus()
    waitForIdle()
    repeat(presses - 1) {
        onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
    }
    val frame = onNodeWithTag(RingFrameTag)
    val before = frame.captureToImage().toPixelMap()
    onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
    val after = frame.captureToImage().toPixelMap()
    val origin = frame.fetchSemanticsNode().boundsInRoot.topLeft
    val focused = onNode(isFocused()).fetchSemanticsNode().boundsInRoot.translate(-origin)
    return RingCapture(rings, before, after, focused, density)
}

/**
 * Checks that the ring drew at least [least] pixels at 3 to 1 or better.
 */
internal fun RingCapture.shouldShowRing(least: Int = RingPixelsNeeded) {
    withClue(summary) { pixels.size shouldBeGreaterThanOrEqual least }
}

/**
 * Checks that the ring runs along all four sides of [around], inside it or out. Each side is looked
 * for in its middle half, within a quarter of the box's size or [reach] outside. The box is the
 * focused node unless the ring goes round only a part of it, such as a slider's thumb, whose test
 * passes [RingCapture.ringBounds].
 */
internal fun RingCapture.shouldRingEverySide(
    reach: Dp = 8.dp,
    around: Rect = focused,
) {
    val bands = sideBands(reach, around)
    val sides = mapOf<String, (Float, Float) -> Boolean>(
        "left" to { x, y -> x in bands.left && y in bands.middleY },
        "right" to { x, y -> x in bands.right && y in bands.middleY },
        "top" to { x, y -> y in bands.top && x in bands.middleX },
        "bottom" to { x, y -> y in bands.bottom && x in bands.middleX },
    )
    for ((side, holds) in sides) {
        withClue("$side side of $around") {
            pixels.filter { point -> holds(point.x + 0.5f, point.y + 0.5f) }.shouldNotBeEmpty()
        }
    }
}

/**
 * Where a side of [box] is looked for, in pixels. Each side is looked for along its middle half,
 * from [out] outside the box to a quarter of the box inside it. [shouldRingEverySide] and
 * [sideCoverage] both read a side here, so the two always judge the same stretch of ring.
 */
internal class SideBands(
    box: Rect,
    out: Float,
) {
    /**
     * The middle half of the top and the bottom, from left to right.
     */
    val middleX: ClosedFloatingPointRange<Float> = (box.left + box.width / 4)..(box.right - box.width / 4)

    /**
     * The middle half of the left and the right side, from top to bottom.
     */
    val middleY: ClosedFloatingPointRange<Float> = (box.top + box.height / 4)..(box.bottom - box.height / 4)

    /**
     * How far across the left side reaches.
     */
    val left: ClosedFloatingPointRange<Float> = (box.left - out)..(box.left + box.width / 4)

    /**
     * How far across the right side reaches.
     */
    val right: ClosedFloatingPointRange<Float> = (box.right - box.width / 4)..(box.right + out)

    /**
     * How far down the top reaches.
     */
    val top: ClosedFloatingPointRange<Float> = (box.top - out)..(box.top + box.height / 4)

    /**
     * How far down the bottom reaches.
     */
    val bottom: ClosedFloatingPointRange<Float> = (box.bottom - box.height / 4)..(box.bottom + out)
}

/**
 * The bands each side of [around] is looked for in, reaching [reach] outside it.
 */
internal fun RingCapture.sideBands(
    reach: Dp,
    around: Rect,
): SideBands = SideBands(around, reach.value * density)

/**
 * Checks that the ring colour stands 3 to 1 from what every ring pixel covered, so no stretch of the
 * ring lies on a ground it matches. [shouldRingEverySide] lets a side through on a handful of 3 to 1
 * pixels, which is how the thumbs, the scheme chip and the copy buttons of the S5 rerun rang only
 * part of the way round. Each pixel is judged by the ring colour it shows rather than its own, so a
 * blended edge pixel on a ground the ring clears by a hair does not fail it.
 */
internal fun RingCapture.shouldRingAllTheWayRound() {
    val matched = candidates.filter { point ->
        val ring = ringAt(point) ?: return@filter false
        contrast(ring, before[point.x, point.y]) < RingContrast
    }
    val first = matched.firstOrNull()
    val where = if (first == null) "" else ", first at (${first.x}, ${first.y})"
    withClue("$summary, ${matched.size} on a ground under 3:1$where") { matched.shouldBeEmpty() }
}

/**
 * Checks that the ring stands 3 to 1 from what lies [gap] beyond each of its edges, along the row
 * through the middle of the focused node. A slider's track runs along that row, so a ring drawn
 * straight over the track fails here even when the rest of it shows. A ring the colour of the track
 * leaves no changed pixels where it crosses it, so only the crossings that changed are looked at.
 */
internal fun RingCapture.shouldClearTheTrack(gap: Dp = 1.dp) {
    val y = focused.center.y.toInt()
    val step = maxOf(1, (gap.value * density).roundToInt())
    val xs = candidates.filter { point -> point.y == y }.map { point -> point.x }.sorted()
    val runs = mutableListOf<IntRange>()
    for (x in xs) {
        val last = runs.lastOrNull()
        if (last != null && x == last.last + 1) runs[runs.lastIndex] = last.first..x else runs += x..x
    }
    withClue("ring crossings on row $y") { runs.shouldNotBeEmpty() }
    for (run in runs) {
        for (x in listOf(run.first - step, run.last + step)) {
            val ratio = contrast(rings.first(), after[x, y])
            withClue("beside the ring at ($x, $y), ${"%.2f".format(ratio)}:1") {
                ratio shouldBeGreaterThanOrEqual RingContrast
            }
        }
    }
}

private fun moved(
    before: Color,
    after: Color,
): Boolean =
    abs(after.red - before.red) > MovedBy ||
        abs(after.green - before.green) > MovedBy ||
        abs(after.blue - before.blue) > MovedBy

private fun near(
    pixel: Color,
    ring: Color,
): Boolean =
    abs(pixel.red - ring.red) <= NearRing &&
        abs(pixel.green - ring.green) <= NearRing &&
        abs(pixel.blue - ring.blue) <= NearRing
