package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.kit.layout.CanvasContentCap
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.layout.WideBreakpoint
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val ShellHeight = 900
private const val ShellPaletteTag = "shell-palette"
private val ShellMargin = 12.dp
private val ShellDockLift = 16.dp
private val ShellTopBar = 64.dp
private val ShellSkin = Skin(Library.Material3, expressive = false)

/** One window from the spec's table and the poster treatment it calls for. */
private class ShellWindow(
    val width: Int,
    val mode: PosterMode,
    val height: Int = ShellHeight,
    val coarsePointer: Boolean = false,
    val peek: Dp = 344.dp,
) {
    override fun toString(): String = "${width}x$height dp, coarse $coarsePointer"
}

private val ShellWindows = listOf(
    ShellWindow(360, PosterMode.Sheet),
    ShellWindow(390, PosterMode.Sheet),
    ShellWindow(600, PosterMode.Rail72),
    ShellWindow(840, PosterMode.Docked320),
    ShellWindow(1280, PosterMode.Docked400),
    ShellWindow(1600, PosterMode.Docked400),
    ShellWindow(1920, PosterMode.Docked400),
    // Phones on their side, Medium by width, and a short desktop window that keeps its dock (D38).
    ShellWindow(844, PosterMode.Sheet, height = 390, coarsePointer = true, peek = 96.dp),
    ShellWindow(915, PosterMode.Sheet, height = 412, coarsePointer = true, peek = 96.dp),
    ShellWindow(1280, PosterMode.Docked400, height = 450),
)

@OptIn(ExperimentalTestApi::class)
class ShellLayoutTest {
    @Test
    fun shell_everyWindow_placesTheRegionsPerTheTableWithNoOverflow() {
        for (case in ShellWindows) {
            withClue(case) {
                shellTest(case.width, case.height) {
                    var mode: PosterMode? = null
                    setContent {
                        ShellHarness(ShellSkin, coarsePointer = case.coarsePointer) {
                            mode = LocalLayout.current.posterMode
                            ShellUnderTest(collapsed = case.mode == PosterMode.Rail72)
                        }
                    }
                    waitForIdle()

                    mode shouldBe case.mode
                    val width = case.width.dp
                    val height = case.height.dp
                    val poster = shellBounds(ShellPosterTag)
                    val canvas = shellBounds(ShellCanvasTag)
                    val topBar = shellBounds(ShellTopBarTag)
                    topBar.top shouldBe 0.dp
                    topBar.height shouldBe ShellTopBar
                    topBar.right shouldBe width
                    val posterWidth = when (case.mode) {
                        PosterMode.Sheet -> null
                        PosterMode.Rail72 -> 72.dp
                        PosterMode.Docked320 -> 320.dp
                        PosterMode.Docked400 -> 400.dp
                    }
                    if (posterWidth == null) {
                        onNode(shellStateDescription("Peek")).assertExists()
                        topBar.left shouldBe 0.dp
                        poster.left shouldBe 0.dp
                        poster.right shouldBe width
                        poster.top shouldBeGreaterThan (height - case.peek)
                        shellBounds(ShellDockTag).bottom shouldBeLessThan (height - case.peek)
                        canvas.top shouldBe ShellTopBar + 4.dp
                        canvas.width shouldBe width - ShellMargin * 2
                    } else {
                        shellDocked(poster, canvas, width, posterWidth)
                        topBar.left shouldBe ShellMargin * 2 + posterWidth
                        poster.top shouldBe ShellMargin
                        poster.bottom shouldBe height - ShellMargin
                        canvas.top shouldBe ShellTopBar
                        shellDockOnTheFrame(shellBounds(ShellDockTag), width, height, posterWidth)
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
    fun canvasPadding_clearsThePeekAndTheDockOnlyInTheSheet() {
        val cases = listOf(
            ShellWindow(390, PosterMode.Sheet) to 344.dp + ShellDockLift + 64.dp,
            ShellWindow(844, PosterMode.Sheet, height = 390, coarsePointer = true) to 96.dp + ShellDockLift + 64.dp,
            ShellWindow(840, PosterMode.Docked320) to 0.dp,
            ShellWindow(1280, PosterMode.Docked400) to 0.dp,
        )
        for ((case, bottom) in cases) {
            withClue(case) {
                shellTest(case.width, case.height) {
                    var padding: PaddingValues? = null
                    setContent {
                        ShellHarness(ShellSkin, coarsePointer = case.coarsePointer) {
                            ShellUnderTest(collapsed = false, onCanvasPadding = { padding = it })
                        }
                    }
                    waitForIdle()

                    padding?.calculateBottomPadding() shouldBe bottom
                    padding?.calculateTopPadding() shouldBe 0.dp
                }
            }
        }
    }

    @Test
    fun posterCollapsed_docked_foldsToTheRailAndHandsTheCanvasTheRoom() {
        for (width in listOf(840, 1280)) {
            withClue("$width dp") {
                shellTest(width) {
                    var rail: Boolean? = null
                    setContent { ShellHarness(ShellSkin) { ShellUnderTest(collapsed = true, onRail = { rail = it }) } }
                    waitForIdle()

                    rail shouldBe true
                    shellDocked(shellBounds(ShellPosterTag), shellBounds(ShellCanvasTag), width.dp, 72.dp)
                }
            }
        }
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

    @Test
    fun posterSlot_railFlag_switchesAtTheStartAndNeverSqueezesThePanel() =
        shellTest(1280) {
            var collapsed by mutableStateOf(false)
            var rail: Boolean? = null
            var slotWidth = 0
            setContent {
                ShellHarness(ShellSkin) {
                    ShellUnderTest(collapsed, onRail = { rail = it }, onPosterWidth = { slotWidth = it })
                }
            }
            waitForIdle()
            rail shouldBe false

            mainClock.autoAdvance = false
            collapsed = true
            mainClock.advanceTimeBy(ShellMidwayMillis)
            rail shouldBe true
            onNode(shellPaneTitle(ShellPosterLabel)).getBoundsInRoot().width shouldBeGreaterThan 72.dp

            mainClock.autoAdvance = true
            waitForIdle()
            mainClock.autoAdvance = false
            collapsed = false
            mainClock.advanceTimeBy(ShellMidwayMillis)
            rail shouldBe false
            onNode(shellPaneTitle(ShellPosterLabel)).getBoundsInRoot().width shouldBeLessThan 400.dp
            slotWidth shouldBe 400
            mainClock.autoAdvance = true
        }

    @Test
    fun frames_openPoster_roundTheirCornersAtTwentyEight() =
        shellTest(1280) {
            var tokens: BuilderTokens? = null
            setContent {
                ShellHarness(ShellSkin) {
                    tokens = LocalBuilderTokens.current
                    ShellUnderTest(collapsed = false)
                }
            }
            waitForIdle()

            val shell = requireNotNull(tokens)
            val frames = listOf(
                Triple("poster", shellBounds(ShellPosterTag), ShellPosterColors.seed.toColor()),
                Triple("canvas", shellBounds(ShellCanvasTag), shell.canvas),
            )
            val pixels = onNodeWithTag(ShellRootTag).captureToImage().toPixelMap()
            for ((name, frame, fill) in frames) {
                withClue(name) {
                    val x = frame.left.value.toInt()
                    val y = frame.top.value.toInt()
                    // On the top row a 28 dp corner leaves 19 dp in clear and is solid from 28 dp, where
                    // a 24 dp corner would already cover the first and a 32 dp one not yet the second.
                    shellArgb(pixels[x + 19, y]) shouldBe shellArgb(shell.panel)
                    shellArgb(pixels[x + 28, y]) shouldBe shellArgb(fill)
                }
            }
        }

    @Test
    fun paletteFrame_perWindowClass_takesTheSpecWidth() {
        for ((width, expected) in listOf(390 to 390.dp, 840 to 560.dp, 1280 to 640.dp)) {
            withClue("$width dp") {
                shellTest(width) {
                    setContent {
                        ShellHarness(ShellSkin) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                PaletteFrame(Modifier.testTag(ShellPaletteTag)) {
                                    Box(Modifier.fillMaxWidth().height(40.dp))
                                }
                            }
                        }
                    }
                    waitForIdle()

                    shellBounds(ShellPaletteTag).width shouldBe expected
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
    height: Int = ShellHeight,
    block: suspend ComposeUiTest.() -> Unit,
) = runSkikoComposeUiTest(size = Size(width.toFloat(), height.toFloat())) { block() }

/** The shell with every slot tagged, the top bar the skin's own and the dock a fixed size like a real one. */
@Composable
private fun ShellUnderTest(
    collapsed: Boolean,
    onRail: (Boolean) -> Unit = {},
    onCanvasPadding: (PaddingValues) -> Unit = {},
    onPosterWidth: (Int) -> Unit = {},
) {
    WorkspaceShell(
        posterColors = ShellPosterColors,
        posterCollapsed = collapsed,
        poster = { rail ->
            onRail(rail)
            Box(Modifier.fillMaxSize().testTag(ShellPosterTag).onSizeChanged { size -> onPosterWidth(size.width) })
        },
        topBar = { TopBarRegion(Modifier.testTag(ShellTopBarTag)) {} },
        canvas = { padding ->
            onCanvasPadding(padding)
            ShellSlot(ShellCanvasTag)
        },
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

/** The dock centred on the canvas frame, standing just off its bottom edge. */
private fun shellDockOnTheFrame(
    dock: DpRect,
    width: Dp,
    height: Dp,
    posterWidth: Dp,
) {
    val frameStart = ShellMargin + posterWidth + ShellMargin
    val frameCentre = (frameStart + width - ShellMargin) / 2
    val dockCentre = (dock.left + dock.right) / 2
    (dockCentre - frameCentre).value.let { gap -> if (gap < 0) -gap else gap } shouldBeLessThanOrEqualTo 1f
    dock.bottom shouldBe height - ShellMargin - ShellDockLift
}

private fun shellArgb(color: Color): String = "#%08X".format(color.toArgb())

private fun shellPaneTitle(title: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)

private fun shellStateDescription(state: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)
