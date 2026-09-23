package com.materialkolor.builder.kit.transition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.LocalTabVisible
import com.materialkolor.builder.kit.motion.reducedBuilderMotion
import com.materialkolor.builder.kit.motion.tweenBuilderMotion
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertTrue

private const val HostTag = "host"
private val Old = Color.Red
private val New = Color.Blue
private val Newer = Color.Green

@OptIn(ExperimentalTestApi::class)
class SkinTransitionTest {
    @Test
    fun reveal_whenMotionIsFrozen_appliesTheChangeWithNoCapture() =
        runComposeUiTest {
            val harness = showHost(frozen = true)

            val reveal = reveal(harness, to = New)

            reveal.isCompleted shouldBe true
            harness.color shouldBe New
            harness.transition.snapshot.size shouldBe IntSize.Zero
            mainClock.advanceTimeByFrame()
            hostPixels().farCorner() shouldBe New
        }

    @Test
    fun reveal_whenTheTabIsHidden_appliesTheChangeWithNoCapture() =
        runComposeUiTest {
            val harness = showHost(tabVisible = false)

            val reveal = reveal(harness, to = New)

            reveal.isCompleted shouldBe true
            harness.color shouldBe New
            harness.transition.snapshot.size shouldBe IntSize.Zero
            mainClock.advanceTimeByFrame()
            hostPixels().farCorner() shouldBe New
        }

    @Test
    fun reveal_whenTheHostNeverDraws_appliesTheChangeAfterTheCaptureTimeout() =
        runComposeUiTest {
            val harness = showHost(withHost = false)

            val reveal = reveal(harness, to = New)
            mainClock.advanceTimeBy(80)
            harness.color shouldBe Old

            mainClock.advanceTimeBy(40)
            harness.color shouldBe New
            reveal.isCompleted shouldBe true
            harness.transition.progress.value shouldBe 1f
        }

    @Test
    fun reveal_normally_capturesTheOldFrameBeforeTheChange() =
        runComposeUiTest {
            val harness = showHost()

            reveal(harness, to = New)
            harness.transition.pendingCapture shouldNotBe null
            harness.color shouldBe Old

            mainClock.advanceTimeByFrame()
            harness.transition.snapshot.size shouldNotBe IntSize.Zero
            harness.color shouldBe Old

            mainClock.advanceTimeByFrame()
            harness.transition.pendingCapture shouldBe null
            harness.color shouldBe New
            harness.transition.snapshot.size shouldNotBe IntSize.Zero
            harness.transition.progress.value shouldBe 0f

            mainClock.advanceTimeByFrame()
            val covered = hostPixels()
            covered.nearOrigin() shouldBe Old
            covered.farCorner() shouldBe Old
        }

    @Test
    fun reveal_normally_neverAppliesTheChangeDuringADraw() =
        runComposeUiTest {
            val harness = showHost()

            val reveal = reveal(harness, to = New)
            repeat(3) { mainClock.advanceTimeByFrame() }
            harness.color shouldBe New
            harness.changedWhileDrawing shouldBe false

            mainClock.advanceTimeBy(500)
            reveal.isCompleted shouldBe true
        }

    @Test
    fun reveal_inBitmapMode_neverAppliesTheChangeDuringADraw() =
        runComposeUiTest {
            val harness = showHost(mode = SnapshotMode.Bitmap)

            reveal(harness, to = New)
            repeat(3) { mainClock.advanceTimeByFrame() }
            harness.color shouldBe New
            harness.changedWhileDrawing shouldBe false
        }

    @Test
    fun reveal_normally_growsTheCircleWithoutRecomposingTheContent() =
        runComposeUiTest {
            val harness = showHost()
            val reveal = reveal(harness, to = New)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            // Under the v2 test dispatcher the change recomposes the content on the frame after it lands.
            mainClock.advanceTimeByFrame()
            val compositionsAfterChange = harness.compositions

            mainClock.advanceTimeBy(150)
            val midway = harness.transition.progress.value
            assertTrue(midway > 0f && midway < 1f, "expected the reveal to be under way, got $midway")
            val pixels = hostPixels()
            pixels.nearOrigin() shouldBe New
            pixels.farCorner() shouldBe Old

            mainClock.advanceTimeBy(400)
            harness.transition.progress.value shouldBe 1f
            reveal.isCompleted shouldBe true
            hostPixels().farCorner() shouldBe New
            harness.compositions shouldBe compositionsAfterChange
        }

    @Test
    fun reveal_duringAnotherReveal_snapsTheFirstToItsEndBeforeCapturing() =
        runComposeUiTest {
            val harness = showHost()
            val first = reveal(harness, to = New)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(100)
            harness.transition.progress.value shouldNotBe 1f

            val second = reveal(harness, to = Newer)
            mainClock.advanceTimeBy(0)
            harness.transition.pendingCapture shouldNotBe null
            harness.transition.progress.value shouldBe 1f
            first.isCompleted shouldBe true
            harness.color shouldBe New

            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(150)
            val pixels = hostPixels()
            pixels.nearOrigin() shouldBe Newer
            pixels.farCorner() shouldBe New

            mainClock.advanceTimeBy(400)
            second.isCompleted shouldBe true
            hostPixels().farCorner() shouldBe Newer
        }

    @Test
    fun reveal_underReducedMotion_crossfadesInPlace() =
        runComposeUiTest {
            val harness = showHost(reduced = true)
            val reveal = reveal(harness, to = New)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            harness.transition.style shouldBe RevealStyle.Crossfade

            mainClock.advanceTimeBy(64)
            val pixels = hostPixels()
            pixels.nearOrigin() shouldBe pixels.farCorner()
            pixels.nearOrigin() shouldNotBe Old
            pixels.nearOrigin() shouldNotBe New

            mainClock.advanceTimeBy(120)
            reveal.isCompleted shouldBe true
            hostPixels().farCorner() shouldBe New
        }

    @Test
    fun reveal_underReducedMotion_turnsTheCircleIntoTheReducedCrossfade() =
        runComposeUiTest {
            val harness = showHost(reduced = true)
            val reveal = reveal(harness, to = New, style = RevealStyle.Circle(Offset.Unspecified))
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            harness.transition.style shouldBe RevealStyle.Crossfade

            mainClock.advanceTimeBy(176)
            reveal.isCompleted shouldBe true
            hostPixels().farCorner() shouldBe New
        }

    @Test
    fun reveal_withTheCrossfadeStyle_fadesTheOldFrameOutOverTheCrossfadeSpec() =
        runComposeUiTest {
            val harness = showHost()
            val reveal = reveal(harness, to = New, style = RevealStyle.Crossfade)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            harness.transition.style shouldBe RevealStyle.Crossfade

            mainClock.advanceTimeBy(96)
            val pixels = hostPixels()
            pixels.nearOrigin() shouldBe pixels.farCorner()
            pixels.center() shouldBe pixels.farCorner()
            pixels.nearOrigin() shouldNotBe Old
            pixels.nearOrigin() shouldNotBe New

            mainClock.advanceTimeBy(96)
            val late = harness.transition.progress.value
            assertTrue(late < 1f, "expected the crossfade to outlast the reduced one, got $late")
            reveal.isCompleted shouldBe false

            mainClock.advanceTimeBy(64)
            reveal.isCompleted shouldBe true
            hostPixels().farCorner() shouldBe New
        }

    @Test
    fun reveal_withTheCircleStyle_clipsTheOldFrameAroundTheOrigin() =
        runComposeUiTest {
            val harness = showHost()
            val reveal = reveal(harness, to = New, style = RevealStyle.Circle(Offset.Unspecified))
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            harness.transition.style shouldBe RevealStyle.Circle(Offset.Unspecified)

            mainClock.advanceTimeBy(100)
            val pixels = hostPixels()
            pixels.center() shouldBe New
            pixels.nearOrigin() shouldBe Old
            pixels.farCorner() shouldBe Old

            mainClock.advanceTimeBy(400)
            reveal.isCompleted shouldBe true
            hostPixels().nearOrigin() shouldBe New
        }

    @Test
    fun reveal_inBitmapMode_drawsTheRasterizedFrame() =
        runComposeUiTest {
            val harness = showHost(mode = SnapshotMode.Bitmap)
            val reveal = reveal(harness, to = New)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            harness.transition.bitmap shouldNotBe null

            mainClock.advanceTimeBy(150)
            val pixels = hostPixels()
            pixels.nearOrigin() shouldBe New
            pixels.farCorner() shouldBe Old

            mainClock.advanceTimeBy(400)
            reveal.isCompleted shouldBe true
            harness.transition.bitmap shouldBe null
            hostPixels().farCorner() shouldBe New
        }

    @Test
    fun reveal_whileTheFontWaitHangs_capturesAfterThreeHundredMillis() =
        runComposeUiTest {
            val harness = showHost()

            reveal(harness, to = New, awaitBeforeReveal = { awaitCancellation() })
            mainClock.advanceTimeBy(250)
            harness.transition.pendingCapture shouldBe null
            harness.color shouldBe Old

            mainClock.advanceTimeBy(100)
            harness.color shouldBe New
            harness.transition.snapshot.size shouldNotBe IntSize.Zero
        }
}

private class Harness {
    lateinit var transition: SkinTransition
    lateinit var scope: CoroutineScope
    var color: Color by mutableStateOf(Old)
    var compositions: Int = 0

    /** True only while the host is drawing. A plain field so setting it from draw writes no state. */
    var drawing: Boolean = false

    /** Set by every change to whether a draw was in progress when it ran. */
    var changedWhileDrawing: Boolean? = null
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showHost(
    frozen: Boolean = false,
    tabVisible: Boolean = true,
    reduced: Boolean = false,
    mode: SnapshotMode = SnapshotMode.Layer,
    withHost: Boolean = true,
): Harness {
    mainClock.autoAdvance = false
    val harness = Harness()
    val motion = if (reduced) reducedBuilderMotion() else tweenBuilderMotion()
    setContent {
        CompositionLocalProvider(
            LocalBuilderMotion provides motion,
            LocalMotionFrozen provides frozen,
            LocalReducedMotion provides reduced,
            LocalTabVisible provides tabVisible,
        ) {
            harness.transition = rememberSkinTransition(mode)
            harness.scope = rememberCoroutineScope()
            if (withHost) {
                SkinTransitionHost(
                    transition = harness.transition,
                    modifier = Modifier.size(100.dp).testTag(HostTag).markDrawing(harness),
                ) {
                    harness.compositions++
                    Box(Modifier.fillMaxSize().background(harness.color))
                }
            }
        }
    }
    mainClock.advanceTimeByFrame()
    return harness
}

/** Raises [Harness.drawing] around the host's whole draw pass, the capture included. */
private fun Modifier.markDrawing(harness: Harness): Modifier =
    drawWithContent {
        harness.drawing = true
        try {
            drawContent()
        } finally {
            harness.drawing = false
        }
    }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.reveal(
    harness: Harness,
    to: Color,
    style: RevealStyle = RevealStyle.Circle(Offset.Zero),
    awaitBeforeReveal: suspend () -> Unit = {},
): Job {
    val job = runOnUiThread {
        harness.scope.launch {
            harness.transition.reveal(style = style, awaitBeforeReveal = awaitBeforeReveal) {
                harness.changedWhileDrawing = harness.drawing
                harness.color = to
            }
        }
    }
    // The v2 test dispatcher queues the launch, so run it now, up to its first wait on a frame or a delay.
    mainClock.advanceTimeBy(0)
    return job
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hostPixels(): PixelMap = onNodeWithTag(HostTag).captureToImage().toPixelMap()

private fun PixelMap.nearOrigin(): Color = this[2, 2]

private fun PixelMap.farCorner(): Color = this[width - 2, height - 2]

private fun PixelMap.center(): Color = this[width / 2, height / 2]
