package com.materialkolor.builder.feature.command

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.input.SetComposingTextCommand
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.workspace.Panel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
internal class CommandPaletteKeysTest {
    private val harness: CommandHarness = CommandHarness()

    private val platform: FakePlatform = harness.platform

    @AfterTest
    fun tearDown() {
        harness.close()
    }

    @Test
    fun enterOnCopyShareLink_writesBeforeTheKeyHandlerReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            openPalette(harness)
            search("copy share link")
            rowLabels().first() shouldBe "Copy share link"
            // No frame and no task runs after the key, so only a write started inside it lands.
            mainClock.autoAdvance = false

            field().performKeyInput { pressKey(Key.Enter) }

            platform.clipboard.texts.size shouldBe 1
            platform.clipboard.texts.single() shouldContain "/t/"
        }

    @Test
    fun enterWhileAnInputMethodComposes_runsNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            var session: PlatformTextInputMethodRequest? = null
            with(harness) { show(onTextInput = { request -> session = request }, probe = { categoryTitles() }) }
            session = null
            openPalette(harness)
            search("copy share lin")
            waitUntil(timeoutMillis = WAIT_MILLIS) { session != null }
            val request = checkNotNull(session)
            runOnUiThread { request.onEditCommand(listOf(SetComposingTextCommand("k", 1))) }
            waitForIdle()
            request.value().composition shouldNotBe null
            rowLabels().first() shouldBe "Copy share link"

            field().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            platform.clipboard.texts shouldBe emptyList()
            harness.workspace.state.value.panel shouldBe Panel.Palette
        }

    @Test
    fun esc_throwsTheSearchAway_thenCloses() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            openPalette(harness)
            search("zzz")
            rowLabels() shouldBe emptyList()

            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe Panel.Palette
            rowLabels().first() shouldBe harness.commands.first { command -> command.id != "palette" }.label
            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe null
        }

    @Test
    fun downAndUp_moveBetweenTheFieldAndTheRows() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            openPalette(harness)
            search("shuffle")
            val shuffle = harness.command("shuffle").label

            field().performKeyInput { pressKey(Key.DirectionDown) }
            waitForIdle()
            onNode(rowMatcher(shuffle)).assertIsFocused()
            onNode(rowMatcher(shuffle)).performKeyInput { pressKey(Key.DirectionUp) }
            waitForIdle()

            field().assertIsFocused()
        }

    @Test
    fun closing_handsFocusBackToCommands_whenItsButtonOpenedIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            val commands = onNode(
                hasClickAction() and hasContentDescription("Command palette") and !InPalette,
            )
            commands.performClick()
            waitForIdle()
            harness.workspace.state.value.panel shouldBe Panel.Palette

            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe null
            commands.assertIsFocused()
        }

    @Test
    fun closing_handsFocusBackToThePage_whenCtrlKOpenedIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }
            harness.workspace.state.value.panel shouldBe Panel.Palette

            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe null
            onNode(hasClickAction() and hasContentDescription("Command palette")).assertIsNotFocused()
            val seed = harness.graph.session.document.value.seed
            keys { pressKey(Key.Spacebar) }

            harness.graph.session.document.value.seed shouldNotBe seed
        }

    /**
     * On the desktop the palette is a window of its own, so the page's holder keeps focus in the
     * page's window, the way it does on the web in the frames before the palette takes focus.
     */
    @Test
    fun esc_thatReachesThePage_closesThePalette() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }
            harness.workspace.state.value.panel shouldBe Panel.Palette

            onNode(isRoot() and hasAnyDescendant(CommandsButton)).performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe null
        }

    @Test
    fun ctrlSInsideThePalette_saves_andCtrlO_leavesItOpen() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot(harness)
            openPalette(harness)

            field().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.O) } }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe Panel.Palette
            field().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe Panel.Palette
            // The poster's Projects button carries the save state now, and the open palette keeps
            // the poster out of the tree, so the button reads once the palette has closed.
            onNode(isRoot() and hasAnyDescendant(CommandsButton)).performKeyInput { pressKey(Key.Escape) }
            val saved = hasContentDescription(", saved", substring = true) and InWorkspace
            waitUntil(
                timeoutMillis = WAIT_MILLIS,
            ) { onAllNodes(saved, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        }
}
