package com.materialkolor.builder.feature.command

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.input.SetComposingTextCommand
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.canvas.DEVICE_SCREEN_TAG
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.ShuffleLock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ShortcutsTest {
    private val harness = CommandHarness()
    private val platform = harness.platform

    @Test
    fun space_afterBootWithNothingFocused_shuffles() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val seed = seed()

            keys { pressKey(Key.Spacebar) }

            seed() shouldNotBe seed
        }

    @Test
    fun numberKeys_eachSwitchTheLibraryAsOneUndoEntry() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val start = harness.graph.session.document.value
            val keysToChoices = listOf(
                Key.Two to LibraryChoice.M3Expressive,
                Key.Three to LibraryChoice.Unstyled,
                Key.Four to LibraryChoice.Fluent,
                Key.Five to LibraryChoice.Custom,
            )
            keysToChoices.forEach { (key, choice) ->
                keys { pressKey(key) }
                waitUntil { LibraryChoice.of(harness.graph.session.document.value) == choice }

                runOnUiThread { harness.workspace.undo() }
                waitForIdle()
                harness.graph.session.document.value shouldBe start
                harness.graph.session.history.value.canUndo shouldBe false
            }
        }

    @Test
    fun shiftE_doesNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val start = harness.graph.session.document.value

            keys { withKeyDown(Key.ShiftLeft) { pressKey(Key.E) } }
            waitForIdle()

            harness.graph.session.document.value shouldBe start
            harness.graph.session.history.value.canUndo shouldBe false
            harness.workspace.state.value.panel shouldBe null
        }

    @Test
    fun c_writesTheSeedBeforeTheHandlerReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            // No frame and no task runs after the key, so only a write started inside it lands.
            mainClock.autoAdvance = false

            onAllNodes(isRoot()).onFirst().performKeyInput { pressKey(Key.C) }

            platform.clipboard.texts shouldBe listOf(
                harness.graph.session.document.value.seed
                    .toHex(),
            )
        }

    @Test
    fun c_whenTheClipboardRefuses_opensTheManualCopy() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.clipboard.failure = IllegalStateException("No user activation")
            boot()

            keys { pressKey(Key.C) }

            named("Copy it yourself") shouldBe true
        }

    @Test
    fun shiftC_copiesEveryFile() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            mainClock.autoAdvance = false

            onAllNodes(isRoot()).onFirst().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.C) } }

            platform.clipboard.texts.size shouldBe 1
            platform.clipboard.texts.single() shouldContain "package com.example.theme"
        }

    @Test
    fun singleKeys_typedIntoTheSeedField_changeNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            onAllNodes(hasSetTextAction()).onFirst().requestFocus()
            waitForIdle()

            typeEverySingleKeyChangesNothing()
        }

    @Test
    fun singleKeys_typedIntoThePreviewTextField_changeNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.setPreviewTab(PreviewTab.Components) }
            waitForIdle()
            val destination = hasSetTextAction() and hasText("Destination", substring = true)
            if (onAllNodes(destination).fetchSemanticsNodes().isEmpty()) {
                onAllNodes(hasScrollToNodeAction()).onLast().performScrollToNode(destination)
            }
            onAllNodes(destination).onFirst().requestFocus()
            waitForIdle()

            typeEverySingleKeyChangesNothing()
        }

    @Test
    fun ctrlSKAndO_fireInsideAField() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            onAllNodes(hasSetTextAction()).onFirst().requestFocus()
            waitForIdle()

            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } }
            waitUntil { named("Saved") }
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.O) } }
            harness.workspace.state.value.panel shouldBe Panel.Projects
            // An open panel owns the keyboard, so the field takes focus again before Ctrl+K.
            runOnUiThread { harness.workspace.closePanel() }
            waitForIdle()
            onAllNodes(hasSetTextAction()).onFirst().requestFocus()
            waitForIdle()
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }

            harness.workspace.state.value.panel shouldBe Panel.Palette
        }

    @Test
    fun ctrlZ_inADirtyField_undoesNoThemeChange() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.edit(DocumentChange.SetThemeName("EditedTheme"), EditPhase.Discrete) }
            waitForIdle()
            val edited = harness.graph.session.document.value
            val field = onAllNodes(hasSetTextAction()).onFirst()
            field.requestFocus()
            field.performTextInput("1")
            waitForIdle()

            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }

            harness.graph.session.document.value shouldBe edited
            harness.graph.session.history.value.canUndo shouldBe true
        }

    @Test
    fun space_onAFocusedButton_pressesItAndNeverShuffles() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val seed = seed()
            onNodeWithContentDescription("Copy hex").requestFocus()
            waitForIdle()

            keys { pressKey(Key.Spacebar) }

            seed() shouldBe seed
            platform.clipboard.texts shouldBe listOf(seed.toHex())
        }

    @Test
    fun space_onAFocusedPreviewButton_pressesItAndNeverShuffles() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val seed = seed()
            onAllNodes(hasClickAction() and hasText("Past")).onFirst().requestFocus()
            waitForIdle()

            keys { pressKey(Key.Spacebar) }

            seed() shouldBe seed
            onAllNodes(hasText("No past trips yet")).fetchSemanticsNodes().size shouldNotBe 0
        }

    @Test
    fun space_afterABareCanvasClickLeavesTheSeedField_shuffles() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            // A phone leaves bare canvas on both sides of its frame.
            runOnUiThread {
                harness.workspace.setDeviceWidth(DeviceWidth.Phone)
                harness.workspace.setPreviewMode(PreviewMode.Light)
            }
            waitForIdle()
            val field = onAllNodes(hasSetTextAction()).onFirst()
            field.requestFocus()
            waitForIdle()
            val seed = seed()
            // Halfway from the phone's right edge to the window's, with nothing under it but the canvas.
            val screen = onAllNodesWithTag(DEVICE_SCREEN_TAG).onFirst().fetchSemanticsNode().boundsInRoot
            val bare = Offset((screen.right + WIDTH) / 2f, HEIGHT / 2f)

            onAllNodes(isRoot()).onFirst().performMouseInput { click(bare) }
            waitForIdle()
            field.assertIsNotFocused()
            keys { pressKey(Key.Spacebar) }

            seed() shouldNotBe seed
        }

    @Test
    fun singleKeys_whileAnInputMethodComposesInTheSeedField_changeNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            // The field's own text input session, driven the way an input method drives it.
            var session: PlatformTextInputMethodRequest? = null
            with(harness) { show(onTextInput = { request -> session = request }) }
            onAllNodes(hasSetTextAction()).onFirst().requestFocus()
            waitUntil { session != null }

            typeEverySingleKeyChangesNothing {
                val request = checkNotNull(session)
                runOnUiThread { request.onEditCommand(listOf(SetComposingTextCommand("ka", 1))) }
                waitForIdle()
                request.value().composition shouldNotBe null
            }
        }

    @Test
    fun ctrlZAndCtrlY_withNothingToUndoOrRedo_sayNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            harness.graph.session.history.value.canUndo shouldBe false

            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.Y) } }

            named("Nothing to undo") shouldBe false
            named("Nothing to redo") shouldBe false
        }

    @Test
    fun space_withEverythingLocked_toastsTheReasonOncePerPress() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread {
                harness.workspace.setLock(ShuffleLock.Seed, true)
                harness.workspace.setLock(ShuffleLock.Style, true)
            }
            waitForIdle()
            val reason = "The seed and the style are both locked, so Shuffle has nothing to change"
            val hint = hasText(reason)
            // b-522 The poster's Shuffle reads it out all along, once it has caught up with the locks.
            val shuffleSaysWhy = hasContentDescription(reason, substring = true)
            waitUntil { onAllNodes(shuffleSaysWhy).fetchSemanticsNodes().isNotEmpty() }
            val before = onAllNodes(hint, useUnmergedTree = true).fetchSemanticsNodes().size
            val seed = seed()

            keys { pressKey(Key.Spacebar) }

            seed() shouldBe seed
            onAllNodes(hint, useUnmergedTree = true).fetchSemanticsNodes().size shouldBe before + 1
        }

    @Test
    fun f_thenFAgain_leavesFullscreenAfterTheButtonWent() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            onNodeWithContentDescription("Fullscreen").requestFocus()
            waitForIdle()

            keys { pressKey(Key.F) }
            harness.workspace.state.value.fullscreen shouldBe true
            keys { pressKey(Key.F) }

            harness.workspace.state.value.fullscreen shouldBe false
        }

    @Test
    fun questionMark_thenEsc_leavesKeysWorking() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()

            keys { withKeyDown(Key.ShiftLeft) { pressKey(Key.Slash) } }
            harness.workspace.state.value.panel shouldBe Panel.CheatSheet
            named("Single-key shortcuts") shouldBe true
            onAllNodes(isFocused()).onLast().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe null
            val seed = seed()
            keys { pressKey(Key.Spacebar) }

            seed() shouldNotBe seed
        }

    @Test
    fun singleKeysOff_leavesOnlyCtrlShortcuts() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.command("singleKeys").run() }
            waitUntil { !harness.workspace.state.value.preferences.singleKeyShortcuts }
            val seed = seed()

            keys { pressKey(Key.Spacebar) }
            seed() shouldBe seed
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }

            harness.workspace.state.value.panel shouldBe Panel.Palette
        }

    private fun ComposeUiTest.boot() {
        with(harness) { show() }
    }

    private fun seed() = harness.graph.session.document.value.seed

    /**
     * Every single key and Space, typed into the focused field with [beforeEach] run ahead of each,
     * leaves the workspace as it was.
     */
    private fun ComposeUiTest.typeEverySingleKeyChangesNothing(beforeEach: () -> Unit = {}) {
        val before = harness.workspace.state.value
        val presses: List<KeyInjectionScope.() -> Unit> =
            SINGLE_KEYS.map { key -> fun KeyInjectionScope.() = pressKey(key) } +
                SHIFTED_KEYS.map { key -> fun KeyInjectionScope.() = withKeyDown(Key.ShiftLeft) { pressKey(key) } }
        presses.forEach { press ->
            beforeEach()
            onAllNodes(isFocused()).onLast().performKeyInput(press)
            waitForIdle()
        }
        val after = harness.workspace.state.value

        after.document shouldBe before.document
        after.preferences shouldBe before.preferences
        after.view shouldBe before.view
        after.panel shouldBe null
        after.inspect shouldBe false
        after.fullscreen shouldBe false
        after.visionMenuOpen shouldBe false
        after.grayscaleHeld shouldBe false
        platform.clipboard.texts shouldBe emptyList()
        onAllNodes(hasContentDescription("Fullscreen")).fetchSemanticsNodes().size shouldBe 1
    }
}

private val SINGLE_KEYS = listOf(
    Key.Spacebar,
    Key.L,
    Key.One,
    Key.Two,
    Key.Three,
    Key.Four,
    Key.Five,
    Key.D,
    Key.LeftBracket,
    Key.RightBracket,
    Key.E,
    Key.C,
    Key.S,
    Key.P,
    Key.I,
    Key.U,
    Key.W,
    Key.F,
    Key.V,
    Key.B,
    Key.H,
)

private val SHIFTED_KEYS = listOf(Key.L, Key.D, Key.C, Key.N, Key.E, Key.Slash)
