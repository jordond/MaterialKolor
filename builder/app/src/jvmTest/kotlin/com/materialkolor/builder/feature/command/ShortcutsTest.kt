package com.materialkolor.builder.feature.command

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.ShuffleLock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Ignore
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

// b-315
@OptIn(ExperimentalTestApi::class)
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
    fun space_withEverythingLocked_showsTheHint() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread {
                harness.workspace.setLock(ShuffleLock.Seed, true)
                harness.workspace.setLock(ShuffleLock.Style, true)
            }
            waitForIdle()
            val seed = seed()

            keys { pressKey(Key.Spacebar) }

            seed() shouldBe seed
            named("The seed and the style are both locked, so Shuffle has nothing to change") shouldBe true
        }

    @Test
    fun numberKeys_eachSwitchTheLibraryAsOneUndoEntry() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val start = harness.graph.session.document.value
            val keysToChoices = listOf(
                Key.Two to LibraryChoice.Expressive,
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
    fun c_writesTheSeedBeforeTheHandlerReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            // No frame and no task runs after the key, so only a write started inside it lands.
            mainClock.autoAdvance = false

            onAllNodes(isRoot()).onFirst().performKeyInput { pressKey(Key.C) }

            platform.clipboard.texts shouldBe listOf(harness.graph.session.document.value.seed.toHex())
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
    fun ctrlKAndCtrlS_fireInsideAField() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            onAllNodes(hasSetTextAction()).onFirst().requestFocus()
            waitForIdle()

            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } }
            waitUntil { named("Saved") }
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
            onNodeWithText("Copy hex").requestFocus()
            waitForIdle()

            keys { pressKey(Key.Spacebar) }

            seed() shouldBe seed
            platform.clipboard.texts shouldBe listOf(seed.toHex())
        }

    @Test
    @Ignore("B-315 open: the test has not found a bare spot to press on the JVM yet")
    fun space_afterABareCanvasClickLeavesTheSeedField_shuffles() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            onAllNodes(hasSetTextAction()).onFirst().requestFocus()
            waitForIdle()
            val seed = seed()

            onAllNodes(isRoot()).onFirst().performMouseInput { click(Offset(WIDTH - 2f, HEIGHT - 2f)) }
            waitForIdle()
            keys { pressKey(Key.Spacebar) }

            seed() shouldNotBe seed
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

    /** Every single key and Space, typed into the focused field, leaves the workspace as it was. */
    private fun ComposeUiTest.typeEverySingleKeyChangesNothing() {
        val before = harness.workspace.state.value
        val presses: List<KeyInjectionScope.() -> Unit> =
            SINGLE_KEYS.map { key -> fun KeyInjectionScope.() = pressKey(key) } +
                SHIFTED_KEYS.map { key -> fun KeyInjectionScope.() = withKeyDown(Key.ShiftLeft) { pressKey(key) } }
        presses.forEach { press ->
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
)

private val SHIFTED_KEYS = listOf(Key.L, Key.D, Key.C, Key.N, Key.Slash)
