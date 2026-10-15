package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.withView
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * How many frames a stepped drag of the handle takes.
 */
private const val DRAG_STEPS = 6

/**
 * How far either side of the settle time the keyboard save is checked, a few frames.
 */
private const val SETTLE_MARGIN_MILLIS = 50L

@OptIn(ExperimentalTestApi::class)
class CanvasModeTest {
    private val app = AppHarness()

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun modeSwitch_underFrozenMotion_composesOneCopyAndComesBackToTheSavedHandle() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost(view = ProjectViewState(splitFraction = 0.3f))
            setContent { Canvas(host) }
            waitForIdle()
            onNode(SplitHandle).assertExists()

            onNodeWithText("Dark").performClick()
            waitForIdle()
            host.state.view.mode shouldBe PreviewMode.Dark
            onNode(SplitHandle).assertDoesNotExist()

            onNodeWithText("Split").performClick()
            waitForIdle()
            handleFraction() shouldBe (0.3f plusOrMinus 0.001f)
            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe emptyList()
        }

    @Test
    fun modeSwitch_withMotion_slidesTheHandleToTheEdgeBeforeDroppingACopy() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host, frozen = false) }
            waitForIdle()
            mainClock.autoAdvance = false

            onNodeWithText("Dark").performClick()
            mainClock.advanceTimeBy(150)
            handleFraction() shouldBeGreaterThan 0f
            handleFraction() shouldBeLessThan 0.5f

            mainClock.advanceTimeBy(1_000)
            onNode(SplitHandle).assertDoesNotExist()
            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe emptyList()
        }

    @Test
    fun splitHandle_moved_savesTheFractionOnceAndStaysThere() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            onNode(SplitHandle).performSemanticsAction(SemanticsActions.SetProgress) { move -> move(0.25f) }
            settle()

            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe
                listOf(WorkspaceAction.SetSplitFraction(0.25f))
            host.state.view.splitFraction shouldBe 0.25f
            handleFraction() shouldBe (0.25f plusOrMinus 0.001f)
        }

    @Test
    fun splitHandle_savedElsewhere_movesToTheSavedFraction() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            host.state = host.state.withView(host.state.view.copy(splitFraction = 0.8f))
            waitForIdle()

            handleFraction() shouldBe (0.8f plusOrMinus 0.001f)
            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe emptyList()
        }

    @Test
    fun projectSwitch_withMotion_keepsTheNewProjectsHandle() {
        val switches = listOf(
            ProjectViewState(mode = PreviewMode.Light, splitFraction = 0.5f) to
                ProjectViewState(mode = PreviewMode.Split, splitFraction = 0.3f),
            ProjectViewState(mode = PreviewMode.Dark, splitFraction = 0.7f) to
                ProjectViewState(mode = PreviewMode.Split, splitFraction = 0.2f),
            ProjectViewState(mode = PreviewMode.Split, splitFraction = 0.4f) to
                ProjectViewState(mode = PreviewMode.Light, splitFraction = 0.6f),
        )
        for ((first, second) in switches) {
            withClue("${first.mode} at ${first.splitFraction} to ${second.mode} at ${second.splitFraction}") {
                runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                    val host = CanvasHost(view = first)
                    setContent { Canvas(host, frozen = false) }
                    waitForIdle()

                    host.state = host.state.withView(second)
                    settle()
                    if (second.mode != PreviewMode.Split) {
                        onNodeWithText("Split").performClick()
                        settle()
                    }

                    handleFraction() shouldBe (second.splitFraction plusOrMinus 0.001f)
                    host.savedFractions shouldBe emptyList()
                    host.state.view.splitFraction shouldBe second.splitFraction
                }
            }
        }
    }

    @Test
    fun splitHandle_draggedOverSeveralFrames_savesOnceWithTheFinalFraction() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            val handle = onNode(SplitHandle)
            handle.performTouchInput { down(center) }
            repeat(DRAG_STEPS) {
                handle.performTouchInput { moveBy(Offset(-40f, 0f)) }
                mainClock.advanceTimeByFrame()
            }
            handle.performTouchInput { up() }
            settle()

            val fraction = handleFraction()
            fraction shouldBeLessThan 0.45f
            host.savedFractions shouldBe listOf(fraction)
            host.state.view.splitFraction shouldBe fraction
        }

    @Test
    fun splitHandle_touchDraggedAndReleased_savesAFrameLaterWithoutTheWait() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()
            mainClock.autoAdvance = false

            val handle = onNode(SplitHandle)
            handle.performTouchInput { down(center) }
            repeat(DRAG_STEPS) {
                handle.performTouchInput { moveBy(Offset(-40f, 0f)) }
                mainClock.advanceTimeByFrame()
            }
            handle.performTouchInput { up() }
            mainClock.advanceTimeByFrame()

            val fraction = handleFraction()
            fraction shouldBeLessThan 0.45f
            host.savedFractions shouldBe listOf(fraction)
            mainClock.advanceTimeBy(HANDLE_SETTLE_MILLIS * 2)
            host.savedFractions shouldBe listOf(fraction)
        }

    @Test
    fun splitHandle_movedByAnArrowKey_savesOnlyOnceItHasRested() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()
            onNode(SplitHandle).requestFocus()
            waitForIdle()
            mainClock.autoAdvance = false

            // The key goes down, and moves the handle, when the press starts. The press itself takes time.
            val pressed = mainClock.currentTime
            onNode(SplitHandle).performKeyInput { pressKey(Key.DirectionLeft) }
            mainClock.advanceTimeBy(HANDLE_SETTLE_MILLIS - SETTLE_MARGIN_MILLIS - (mainClock.currentTime - pressed))
            host.savedFractions shouldBe emptyList()

            mainClock.advanceTimeBy(SETTLE_MARGIN_MILLIS * 2)
            host.savedFractions shouldBe listOf(handleFraction())
            handleFraction() shouldBe (0.45f plusOrMinus 0.001f)
        }

    @Test
    fun splitHandle_savedElsewhereWhileASaveWaits_dropsTheWaitingSave() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()
            mainClock.autoAdvance = false

            onNode(SplitHandle).performSemanticsAction(SemanticsActions.SetProgress) { move -> move(0.25f) }
            mainClock.advanceTimeByFrame()
            // Leaving Split keeps the save waiting, and the handle stays put for the save from elsewhere.
            host.state = host.state.withView(host.state.view.copy(mode = PreviewMode.Dark))
            mainClock.advanceTimeByFrame()
            host.state = host.state.withView(host.state.view.copy(splitFraction = 0.8f))
            mainClock.advanceTimeBy(HANDLE_SETTLE_MILLIS * 2)

            host.savedFractions shouldBe emptyList()
        }

    @Test
    fun previewSplit_mark_goesStaleOnlyOnceAFractionIsSavedElsewhere() {
        val preview = PreviewSplit(PreviewMode.Split, saved = 0.5f)
        val mark = preview.mark()

        preview.send(0.3f) shouldBe true
        preview.onSaved(0.3f)
        preview.savedElsewhereSince(mark) shouldBe false

        preview.onSaved(0.7f)
        preview.savedElsewhereSince(mark) shouldBe true
    }

    @Test
    fun modeSwitch_fromTheDock_appliesAtOnceWithoutAReveal() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = with(app) { bootRoot() }
            graph.session.viewState.value.mode shouldBe PreviewMode.Split
            mainClock.autoAdvance = false

            onAllNodesWithText("Dark").filterToOne(hasClickAction()).performClick()

            // A reveal would hold the change until it had captured a frame, and no frame has passed.
            graph.session.viewState.value.mode shouldBe PreviewMode.Dark
            mainClock.autoAdvance = true
            waitForIdle()
        }

    /**
     * Lets any slide finish and the handle rest long enough to be saved.
     */
    private fun ComposeUiTest.settle() {
        waitForIdle()
        mainClock.advanceTimeBy(HANDLE_SETTLE_MILLIS * 4)
        waitForIdle()
    }
}
