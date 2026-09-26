package com.materialkolor.builder.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.feature.command.CommandHarness
import com.materialkolor.builder.feature.command.InWorkspace
import com.materialkolor.builder.feature.command.keys
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.workspace.Panel
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

/**
 * How many times the drag under the open list moves the seed, one move every other frame.
 */
private const val DRAG_MOVES = 20

/**
 * A Medium window whose top bar has moved History into More.
 */
private const val TIGHT_WIDTH = 600

/**
 * The frames a jump across a library switch takes to land in every part of the page, with room to spare.
 */
private const val SWITCH_FRAMES = 4

/**
 * Frames enough for a row's swatch to read its new step, and well short of a swatch's hold-still wait.
 */
private const val SHIFT_FRAMES = 6

/**
 * The History list on the keyboard, drawn in the page the way the web draws it, where Esc and the
 * focus hand back behave as people meet them, and in a popup window of its own as the desktop draws
 * it.
 */
@OptIn(ExperimentalTestApi::class)
class TimelineTest {
    private val harness = CommandHarness()

    @Test
    fun timeline_keysOpenMoveAndApply_staysOpenAndEscReturnsFocus() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            listOf(Style.Vibrant, Style.Rainbow, Style.Neutral).forEach { style ->
                runOnUiThread { harness.workspace.edit(DocumentChange.SetStyle(style), EditPhase.Discrete) }
                waitForIdle()
            }

            keys { pressKey(Key.H) }
            harness.workspace.state.value.panel shouldBe Panel.History
            focusedRow().assertIsSelected().assert(hasText("Style change"))
            keys { pressKey(Key.DirectionDown) }
            keys { pressKey(Key.DirectionDown) }
            keys { pressKey(Key.Enter) }

            harness.graph.session.document.value.style shouldBe Style.Vibrant
            harness.workspace.state.value.panel shouldBe Panel.History
            focusedRow().assertIsSelected()
            keys { pressKey(Key.Escape) }
            harness.workspace.state.value.panel shouldBe null
            // H opened it, so focus went back to the page, where Space shuffles.
            onNode(hasContentDescription("History") and hasClickAction() and InWorkspace).assertIsNotFocused()
            val seed = harness.graph.session.document.value.seed
            keys { pressKey(Key.Spacebar) }
            harness.graph.session.document.value.seed shouldNotBe seed
        }

    @Test
    fun timeline_jumpAcrossALibrarySwitch_keepsFocusOnTheCurrentStep() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.edit(LibraryChoice.Unstyled.change, EditPhase.Discrete) }
            waitUntil { harness.workspace.state.value.document.library == Library.Unstyled }
            waitForIdle()

            keys { pressKey(Key.H) }
            focusedRow().assertIsSelected().assert(hasText("Library change to Unstyled"))
            keys { pressKey(Key.DirectionDown) }
            keys { pressKey(Key.Enter) }
            waitUntil { harness.workspace.state.value.document.library == Library.Material3 }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe Panel.History
            focusedRow().assertIsSelected().assert(hasText("Start"))
        }

    @Test
    fun timeline_popupJumpOntoFluent_staysOpenWithFocusOnTheCurrentStep() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            popupJumpOntoFluent(historyInBar = true)
        }

    @Test
    fun timeline_popupFromMoreJumpOntoFluent_staysOpenWithFocusOnTheCurrentStep() =
        runDesktopComposeUiTest(width = TIGHT_WIDTH, height = HEIGHT) {
            popupJumpOntoFluent(historyInBar = false)
        }

    @Test
    fun timeline_escOnAnyFrameOfAJumpAcrossALibrarySwitch_closesTheList() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.edit(LibraryChoice.Unstyled.change, EditPhase.Discrete) }
            waitUntil { harness.workspace.state.value.document.library == Library.Unstyled }
            waitForIdle()

            // A busy page can take the key on any frame of the switch, so Esc goes in after each
            // count of frames in turn, the list hopping between Start and the Unstyled step.
            val lost = (0..SWITCH_FRAMES).filterNot { frames -> escClosesTheListAfterAJump(frames) }

            lost.shouldBeEmpty()
        }

    @Test
    fun timeline_dragRunningWhileOpen_readsTheNewestSwatchOnceItHoldsStill() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            var reads = 0
            val dragging = mutableStateOf(false)
            with(harness) {
                show(
                    inTree = true,
                    swatchReads = { reads++ },
                    probe = { _ -> DragEveryOtherFrame(dragging) },
                )
            }
            // A drag held down on the seed while H opens the list. A press outside the list closes it
            // before it reaches the page, so only a drag that began first runs under it.
            runOnUiThread { harness.workspace.edit(seedChange(0), EditPhase.Dragging) }
            waitForIdle()
            keys { pressKey(Key.H) }
            harness.workspace.state.value.panel shouldBe Panel.History
            val filled = reads

            runOnIdle { dragging.value = true }
            waitForIdle()

            harness.graph.session.document.value.seed shouldBe seedChange(DRAG_MOVES).argb
            harness.workspace.state.value.panel shouldBe Panel.History
            reads shouldBe filled + 1
        }

    @Test
    fun timeline_rowTakingAnotherStepAtCapacity_readsItsSwatchWithoutWaitingToHoldStill() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            var reads = 0
            with(harness) { show(inTree = true, swatchReads = { reads++ }) }
            // A full history, so the next step drops the oldest and every row then shows another step.
            runOnUiThread {
                repeat(History.CAPACITY) { step -> harness.workspace.edit(styleChange(step), EditPhase.Discrete) }
            }
            waitForIdle()
            keys { pressKey(Key.H) }
            val filled = reads

            mainClock.autoAdvance = false
            runOnUiThread { harness.workspace.edit(styleChange(History.CAPACITY), EditPhase.Discrete) }
            repeat(SHIFT_FRAMES) { mainClock.advanceTimeByFrame() }

            reads shouldBeGreaterThan filled
        }

    private fun ComposeUiTest.boot() {
        with(harness) { show(inTree = true) }
    }

    /**
     * Opens the list in a popup window, the desktop's own overlays, and jumps from Start onto a
     * Fluent step, which moves the top bar into Fluent's while the window is up. The list hangs from
     * the History button when [historyInBar] holds, and from More otherwise.
     */
    private fun ComposeUiTest.popupJumpOntoFluent(historyInBar: Boolean) {
        with(harness) { show() }
        onAllNodes(hasContentDescription("History") and hasClickAction() and InWorkspace)
            .fetchSemanticsNodes()
            .size shouldBe if (historyInBar) 1 else 0
        runOnUiThread { harness.workspace.edit(LibraryChoice.Fluent.change, EditPhase.Discrete) }
        waitUntil { harness.workspace.state.value.document.library == Library.Fluent }
        waitForIdle()
        runOnUiThread { harness.workspace.undo() }
        waitUntil { harness.workspace.state.value.document.library == Library.Material3 }
        waitForIdle()

        keys { pressKey(Key.H) }
        // The page's root and the list's own window.
        onAllNodes(isRoot()).fetchSemanticsNodes().size shouldBe 2
        focusedRow().assertIsSelected().assert(hasText("Start"))
        // A popup window hears keys through its own root, so they go to the row holding focus.
        focusedRow().performKeyInput { pressKey(Key.DirectionUp) }
        waitForIdle()
        focusedRow().assert(hasText("Library change to Fluent")).performKeyInput { pressKey(Key.Enter) }
        waitUntil { harness.workspace.state.value.document.library == Library.Fluent }
        waitForIdle()

        harness.workspace.state.value.panel shouldBe Panel.History
        focusedRow().assertIsSelected().assert(hasText("Library change to Fluent"))
    }

    /**
     * Opens the list with H, jumps to the row on the other library, lets [frames] frames of the
     * switch go by and presses Esc. Says whether that closed the list, and closes it when it did not,
     * so the next round starts from a closed list.
     */
    private fun ComposeUiTest.escClosesTheListAfterAJump(frames: Int): Boolean {
        val library = harness.workspace.state.value.document.library
        keys { pressKey(Key.H) }
        // The list opens on the row the theme is at, and Start sits under the Unstyled step.
        keys { pressKey(if (library == Library.Unstyled) Key.DirectionDown else Key.DirectionUp) }
        mainClock.autoAdvance = false
        keys { pressKey(Key.Enter) }
        repeat(frames) { mainClock.advanceTimeByFrame() }
        keys { pressKey(Key.Escape) }
        mainClock.autoAdvance = true
        waitForIdle()

        harness.workspace.state.value.document.library shouldNotBe library
        val closed = harness.workspace.state.value.panel == null
        if (!closed) {
            runOnUiThread { harness.workspace.closePanel() }
            waitForIdle()
        }
        return closed
    }

    /**
     * Once [dragging] turns on, moves the seed one more notch every other frame, [DRAG_MOVES] times,
     * the way a pointer moves under a display that draws twice as often.
     */
    @Composable
    private fun DragEveryOtherFrame(dragging: MutableState<Boolean>) {
        if (!dragging.value) return
        LaunchedEffect(Unit) {
            repeat(DRAG_MOVES) { move ->
                repeat(2) { withFrameNanos { } }
                harness.workspace.edit(seedChange(move + 1), EditPhase.Dragging)
            }
        }
    }

    private fun styleChange(step: Int): DocumentChange.SetStyle =
        DocumentChange.SetStyle(if (step % 2 == 0) Style.Vibrant else Style.Rainbow)

    private fun seedChange(notch: Int): DocumentChange.SetSeed =
        DocumentChange.SetSeed(Argb(0x3366CC + notch * 0x010101), SeedSource.Picked)

    /**
     * Whatever holds focus, a row of the list while it is open.
     */
    private fun ComposeUiTest.focusedRow(): SemanticsNodeInteraction = onNode(isFocused() and InWorkspace)
}
