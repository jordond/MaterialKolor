package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlin.math.abs

/** The frame [tabOntoRing] captures, the control on the panel with room for an outset ring. */
private const val RingFrameTag = "ring-frame"

/** The button [tabOntoRing] presses Tab from. */
private const val RingStartTag = "ring-start"

/** How far a channel has to move for S5 to count the pixel as changed. */
private const val MovedBy = 24f / 255f

/** How close to the ring colour a pixel has to sit, on every channel, to count as ring. */
private const val NearRing = 0.02f

/** The least contrast a ring pixel needs against what was there before (WCAG 2.4.13). */
private const val RingContrast = 3.0

/** How many ring pixels a control needs before it counts as ringed. */
private const val RingPixelsNeeded = 100

/**
 * The frame around a control before and after Tab moved focus onto it.
 *
 * @property[ring] The skin's focus colour.
 * @property[focused] Where the focused node sits in the frame, in pixels.
 */
internal class RingCapture(
    val ring: Color,
    val before: PixelMap,
    val after: PixelMap,
    val focused: Rect,
) {
    /** Every pixel that moved and now sits on the ring colour, whatever its contrast. */
    val candidates: List<IntOffset> = buildList {
        for (y in 0 until minOf(before.height, after.height)) {
            for (x in 0 until minOf(before.width, after.width)) {
                val now = after[x, y]
                if (moved(before[x, y], now) && near(now, ring)) add(IntOffset(x, y))
            }
        }
    }

    /** The candidates at 3 to 1 or better against their unfocused colour, which is the ring. */
    val pixels: List<IntOffset> = candidates.filter { point -> ratioAt(point) >= RingContrast }

    /** The contrast of the pixel at [point] against its unfocused colour. */
    fun ratioAt(point: IntOffset): Double = contrast(after[point.x, point.y], before[point.x, point.y])
}

/**
 * Draws [content] on the panel of [skin] after a plain button, focuses the button, then presses
 * Tab so focus moves onto the control, and captures the frame on both sides of the key.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tabOntoRing(
    skin: Skin,
    content: @Composable () -> Unit,
): RingCapture {
    var ring = Color.Unspecified
    setContent {
        ControlsHarness(skin) {
            val tokens = LocalBuilderTokens.current
            ring = tokens.focus
            Column {
                OverlayTestButton(RingStartTag)
                Box(Modifier.testTag(RingFrameTag).background(tokens.panel).padding(8.dp)) { content() }
            }
        }
    }
    val start = onNodeWithTag(RingStartTag)
    start.requestFocus()
    waitForIdle()
    val frame = onNodeWithTag(RingFrameTag)
    val before = frame.captureToImage().toPixelMap()
    start.performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
    val after = frame.captureToImage().toPixelMap()
    val origin = frame.fetchSemanticsNode().boundsInRoot.topLeft
    val focused = onNode(isFocused()).fetchSemanticsNode().boundsInRoot.translate(-origin)
    return RingCapture(ring, before, after, focused)
}

/** Checks that the ring drew at least [RingPixelsNeeded] pixels at 3 to 1 or better. */
internal fun RingCapture.shouldShowRing() {
    val lowest = candidates.minOfOrNull(::ratioAt)
    val seen = "${pixels.size} of ${candidates.size} ring pixels at 3:1, lowest ${"%.2f".format(lowest ?: 0.0)}"
    withClue(seen) { pixels.size shouldBeGreaterThanOrEqual RingPixelsNeeded }
}

/**
 * Checks that the ring runs along all four sides of the focused node, inside it or out. Each side
 * is looked for in its middle half, within a quarter of the node's size or [reach] pixels outside.
 */
internal fun RingCapture.shouldRingEverySide(reach: Float) {
    val box = focused
    val middleX = (box.left + box.width / 4)..(box.right - box.width / 4)
    val middleY = (box.top + box.height / 4)..(box.bottom - box.height / 4)
    val sides = mapOf<String, (Float, Float) -> Boolean>(
        "left" to { x, y -> x in (box.left - reach)..(box.left + box.width / 4) && y in middleY },
        "right" to { x, y -> x in (box.right - box.width / 4)..(box.right + reach) && y in middleY },
        "top" to { x, y -> y in (box.top - reach)..(box.top + box.height / 4) && x in middleX },
        "bottom" to { x, y -> y in (box.bottom - box.height / 4)..(box.bottom + reach) && x in middleX },
    )
    for ((side, holds) in sides) {
        withClue("$side side of $box") {
            pixels.filter { point -> holds(point.x + 0.5f, point.y + 0.5f) }.shouldNotBeEmpty()
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
