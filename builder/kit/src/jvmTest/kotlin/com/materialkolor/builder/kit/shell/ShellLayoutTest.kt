package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.layout.CanvasContentCap
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.layout.WideBreakpoint
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val ShellHeight = 900
private val ShellMargin = 12.dp
private val ShellSkin = Skin(Library.Material3, expressive = false)

/** One width from the spec's table and the poster treatment it calls for. */
private class ShellWidth(
    val width: Int,
    val mode: PosterMode,
)

private val ShellWidths = listOf(
    ShellWidth(360, PosterMode.Sheet),
    ShellWidth(390, PosterMode.Sheet),
    ShellWidth(600, PosterMode.Rail72),
    ShellWidth(840, PosterMode.Docked320),
    ShellWidth(1280, PosterMode.Docked400),
    ShellWidth(1920, PosterMode.Docked400),
)

@OptIn(ExperimentalTestApi::class)
class ShellLayoutTest {
    @Test
    fun shell_everyWidth_placesThePosterPerTheTableWithNoOverflow() {
        for (case in ShellWidths) {
            withClue("${case.width} dp") {
                shellTest(case.width) {
                    var mode: PosterMode? = null
                    setContent {
                        ShellHarness(ShellSkin) {
                            mode = LocalLayout.current.posterMode
                            ShellUnderTest(collapsed = case.mode == PosterMode.Rail72)
                        }
                    }
                    waitForIdle()

                    mode shouldBe case.mode
                    val width = case.width.dp
                    val poster = shellBounds(ShellPosterTag)
                    val canvas = shellBounds(ShellCanvasTag)
                    when (case.mode) {
                        PosterMode.Sheet -> {
                            onNode(shellStateDescription("Peek")).assertExists()
                            poster.left shouldBe 0.dp
                            poster.right shouldBe width
                            poster.top shouldBeGreaterThan (ShellHeight.dp - shellPeek())
                            shellBounds(ShellDockTag).bottom shouldBeLessThan (ShellHeight.dp - shellPeek())
                            canvas.width shouldBe width - ShellMargin * 2
                        }
                        PosterMode.Rail72 -> {
                            shellDocked(poster, canvas, width, 72.dp)
                        }
                        PosterMode.Docked320 -> {
                            shellDocked(poster, canvas, width, 320.dp)
                        }
                        PosterMode.Docked400 -> {
                            shellDocked(poster, canvas, width, 400.dp)
                        }
                    }
                    onNode(shellPaneTitle(ShellPosterLabel)).assertExists()
                    val overflow = listOf(ShellPosterTag, ShellCanvasTag, ShellTopBarTag, ShellDockTag).filter { tag ->
                        val bounds = shellBounds(tag)
                        bounds.left < 0.dp || bounds.right > width
                    }
                    overflow.shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun canvas_fromWideBreakpoint_capsItsContentAndCentresIt() =
        shellTest(1920) {
            setContent { ShellHarness(ShellSkin) { ShellUnderTest(collapsed = false) } }
            waitForIdle()

            val frameStart = ShellMargin + 400.dp + ShellMargin
            val frameWidth = 1920.dp - frameStart - ShellMargin
            val canvas = shellBounds(ShellCanvasTag)
            canvas.width shouldBe CanvasContentCap
            canvas.left shouldBe frameStart + (frameWidth - CanvasContentCap) / 2
        }

    @Test
    fun canvas_belowWideBreakpoint_fillsItsFrame() =
        shellTest(1280) {
            setContent { ShellHarness(ShellSkin) { ShellUnderTest(collapsed = false) } }
            waitForIdle()

            shellBounds(ShellCanvasTag).width shouldBe 1280.dp - 424.dp - ShellMargin
        }

    @Test
    fun posterCollapsed_expanded_foldsToTheRailAndHandsTheCanvasTheRoom() =
        shellTest(1280) {
            setContent { ShellHarness(ShellSkin) { ShellUnderTest(collapsed = true) } }
            waitForIdle()

            shellDocked(shellBounds(ShellPosterTag), shellBounds(ShellCanvasTag), 1280.dp, 72.dp)
        }

    @Test
    fun posterOpened_narrowMedium_floatsOverTheCanvas() =
        shellTest(600) {
            setContent { ShellHarness(ShellSkin) { ShellUnderTest(collapsed = false) } }
            waitForIdle()

            shellBounds(ShellPosterTag).width shouldBe 320.dp
            shellBounds(ShellCanvasTag).left shouldBe ShellMargin + 72.dp + ShellMargin
        }

    @Test
    fun posterCollapse_expanded_animatesTheWidthAndSnapsUnderReducedMotion() {
        for (reduced in listOf(false, true)) {
            withClue("reduced motion $reduced") {
                shellTest(1280) {
                    var collapsed by mutableStateOf(false)
                    setContent { ShellHarness(ShellSkin, reducedMotion = reduced) { ShellUnderTest(collapsed) } }
                    waitForIdle()

                    mainClock.autoAdvance = false
                    collapsed = true
                    mainClock.advanceTimeBy(ShellMidwayMillis)
                    val midway = shellBounds(ShellPosterTag).width
                    if (reduced) {
                        midway shouldBe 72.dp
                    } else {
                        midway shouldBeGreaterThan 72.dp
                        midway shouldBeLessThan 400.dp
                    }
                    mainClock.autoAdvance = true
                    waitForIdle()
                    shellBounds(ShellPosterTag).width shouldBe 72.dp
                }
            }
        }
    }
}

/** Far enough into the 200 ms panel exit to catch the rail moving. */
private const val ShellMidwayMillis = 120L

@OptIn(ExperimentalTestApi::class)
private fun shellTest(
    width: Int,
    block: suspend ComposeUiTest.() -> Unit,
) = runSkikoComposeUiTest(size = Size(width.toFloat(), ShellHeight.toFloat())) { block() }

/** The shell with every slot tagged, the dock a fixed size like a real one. */
@Composable
private fun ShellUnderTest(collapsed: Boolean) {
    WorkspaceShell(
        posterColors = ShellPosterColors,
        posterCollapsed = collapsed,
        poster = { ShellSlot(ShellPosterTag) },
        topBar = { Box(Modifier.fillMaxWidth().height(64.dp).testTag(ShellTopBarTag)) },
        canvas = { ShellSlot(ShellCanvasTag) },
        dock = { Box(Modifier.size(width = 240.dp, height = 56.dp).testTag(ShellDockTag)) },
    )
}

/**
 * A docked or railed poster of [posterWidth] at the start, and the canvas frame in the rest, its
 * content capped and centred from the wide breakpoint.
 */
private fun shellDocked(
    poster: DpRect,
    canvas: DpRect,
    width: Dp,
    posterWidth: Dp,
) {
    poster.left shouldBe ShellMargin
    poster.width shouldBe posterWidth
    val frameStart = ShellMargin + posterWidth + ShellMargin
    val frameWidth = width - frameStart - ShellMargin
    val contentWidth = if (width >= WideBreakpoint) minOf(frameWidth, CanvasContentCap) else frameWidth
    canvas.width shouldBe contentWidth
    canvas.left shouldBe frameStart + (frameWidth - contentWidth) / 2
}

/** The peek on a window as tall as these tests use. */
private fun shellPeek(): Dp = 344.dp

private fun shellPaneTitle(title: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)

private fun shellStateDescription(state: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)
