package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpRect
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CanvasFullscreenTest {
    @Test
    fun fullscreen_fromTheDock_hidesTheTopBarUntilTheExitPillBringsItBack() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            showWorkspace(host)
            waitForIdle()
            onNodeWithText("Export code").assertExists()

            onNodeWithContentDescription("Fullscreen").performClick()
            waitForIdle()
            host.state.fullscreen shouldBe true
            onNodeWithText("Export code").assertDoesNotExist()
            onNodeWithContentDescription("Fullscreen").assertDoesNotExist()

            onNodeWithText("Exit fullscreen").performClick()
            waitForIdle()
            host.state.fullscreen shouldBe false
            onNodeWithText("Export code").assertExists()
        }

    @Test
    fun fullscreen_enteredAndLeftByKeyboard_movesFocusToThePillAndBackToTheButton() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            showWorkspace(host)
            waitForIdle()

            onNodeWithContentDescription("Fullscreen").requestFocus()
            onNodeWithContentDescription("Fullscreen").performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            host.state.fullscreen shouldBe true
            onNodeWithText("Exit fullscreen").assertIsFocused()

            onNodeWithText("Exit fullscreen").performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            host.state.fullscreen shouldBe false
            onNodeWithContentDescription("Fullscreen").assertIsFocused()
        }

    @Test
    fun fullscreen_atCompact_keepsTheExitPillOffEveryTab() =
        runDesktopComposeUiTest(width = PHONE, height = HEIGHT) {
            val host = CanvasHost()
            showWorkspace(host)
            waitForIdle()
            host.dispatcher.dispatch(WorkspaceAction.ToggleFullscreen)
            waitForIdle()

            // Arrowing back from App wraps to the last tab and scrolls it into view at the end.
            onNode(canvasTab(PreviewTab.App)).requestFocus()
            onNode(canvasTab(PreviewTab.App)).performKeyInput { pressKey(Key.DirectionLeft) }
            waitForIdle()
            host.state.view.tab shouldBe PreviewTab.Contrast

            val pill = onNodeWithText("Exit fullscreen").getBoundsInRoot()
            for (tab in PreviewTab.entries) {
                withClue(tab.name) { onNode(canvasTab(tab)).getBoundsInRoot().overlaps(pill) shouldBe false }
            }
        }
}

/** The canvas tab named for [tab]. */
private fun canvasTab(tab: PreviewTab): SemanticsMatcher =
    hasText(tab.name) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

private fun DpRect.overlaps(other: DpRect): Boolean =
    left < other.right && other.left < right && top < other.bottom && other.top < bottom
