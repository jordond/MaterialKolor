package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderProgress
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val BarTag = "bar"

private const val FrameTag = "frame"

/** How wide the bars are laid out, wider than any shape Material would pin a bar to. */
private val BarWidth = 320.dp

/**
 * The Material3 skin's progress bar, the expressive flavour most of all. Its bar fills the width it
 * is given, and while nobody can tell how far along the work is its semantics hold still as the sweep
 * moves (R-B-402c, D40).
 */
@OptIn(ExperimentalTestApi::class)
class MaterialProgressTest {
    @Test
    fun progress_expressiveIndeterminate_keepsItsSemanticsWhileTheSweepMoves() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ControlsHarness(Skin(Library.Material3, expressive = true)) {
                    CompositionLocalProvider(LocalMotionFrozen provides false) {
                        BuilderProgress("Exporting", Modifier.testTag(BarTag))
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val configs = mutableListOf<SemanticsConfiguration>()
            val frames = mutableListOf<PixelMap>()
            repeat(5) {
                configs += onNodeWithTag(BarTag, useUnmergedTree = true).fetchSemanticsNode().config
                frames += onNodeWithTag(BarTag).captureToImage().toPixelMap()
                mainClock.advanceTimeBy(120)
            }

            withClue("frames the sweep moved in") {
                frames.zipWithNext().count { (earlier, later) -> earlier.differsFrom(later) } shouldBeGreaterThan 0
            }
            // A semantics block that read the moving phase would be rebuilt into a new configuration.
            withClue("semantics configurations over five frames") {
                configs.distinctBy { config -> System.identityHashCode(config) } shouldHaveSize 1
            }
            configs.first()[SemanticsProperties.ProgressBarRangeInfo] shouldBe ProgressBarRangeInfo.Indeterminate
        }

    @Test
    fun progress_bothFlavours_determinate_readsTheAmountItIsGiven() {
        for (expressive in listOf(false, true)) {
            withClue(if (expressive) "expressive" else "material3") {
                runComposeUiTest {
                    var amount by mutableStateOf(0.6f)
                    setContent {
                        ControlsHarness(Skin(Library.Material3, expressive)) {
                            BuilderProgress("Exporting", Modifier.testTag(BarTag), amount)
                        }
                    }
                    rangeInfo() shouldBe ProgressBarRangeInfo(0.6f, 0f..1f)
                    amount = 0.25f
                    waitForIdle()
                    rangeInfo() shouldBe ProgressBarRangeInfo(0.25f, 0f..1f)
                }
            }
        }
    }

    @Test
    fun progress_everySkin_eitherWay_drawsAcrossTheWholeWidthItIsGiven() =
        forEachSkin { _, skin ->
            var amount by mutableStateOf<Float?>(0.6f)
            setContent {
                ControlsHarness(skin) {
                    Box(Modifier.testTag(FrameTag).width(BarWidth).padding(vertical = 8.dp)) {
                        BuilderProgress("Exporting", Modifier.fillMaxWidth().testTag(BarTag), amount)
                    }
                }
            }
            for (given in listOf(0.6f, null)) {
                withClue(if (given == null) "indeterminate" else "at $given") {
                    amount = given
                    waitForIdle()
                    onNodeWithTag(BarTag).assertWidthIsEqualTo(BarWidth)
                    val frame = onNodeWithTag(FrameTag).captureToImage().toPixelMap()
                    val ground = frame[frame.width / 2, 0]
                    val drawn = (0 until frame.width).filter { x ->
                        (0 until frame.height).any { y -> frame[x, y] != ground }
                    }
                    withClue("columns the bar drew in, of ${frame.width}") {
                        (drawn.first().toFloat() / frame.width) shouldBeLessThan 0.05f
                        (drawn.last().toFloat() / frame.width) shouldBeGreaterThan 0.95f
                    }
                }
            }
        }
}

/** The range the bar tagged [BarTag] reports. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.rangeInfo(): ProgressBarRangeInfo =
    onNodeWithTag(BarTag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

/** Whether any pixel of this frame differs from the same pixel of [other]. */
private fun PixelMap.differsFrom(other: PixelMap): Boolean =
    (0 until minOf(height, other.height)).any { y ->
        (0 until minOf(width, other.width)).any { x -> this[x, y] != other[x, y] }
    }
