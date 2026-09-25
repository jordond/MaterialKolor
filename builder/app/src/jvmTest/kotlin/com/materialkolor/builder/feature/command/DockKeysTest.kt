package com.materialkolor.builder.feature.command

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.topbar.LibraryChoice
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

/**
 * V opens the dock's Vision menu, and a held B shows the canvas in grayscale.
 */
@OptIn(ExperimentalTestApi::class)
class DockKeysTest {
    private val harness = CommandHarness()

    /**
     * On the Unstyled skin, whose menu takes focus into its rows in a popup window the way every skin's
     * does in the page on the web. Material's own popup menu on the desktop leaves its rows unfocused.
     * Esc reaches a popup window through the window, where a test cannot press it, so a pick closes
     * the menu here. Both close it the same way, through `onDismissRequest`.
     */
    @Test
    fun v_opensTheVisionMenuWithFocusInIt_andClosingItHandsFocusBackToItsButton() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }
            // The switch moves the page into the new skin, and the holder takes focus back once it has.
            runOnUiThread { harness.workspace.edit(LibraryChoice.Unstyled.change, EditPhase.Discrete) }
            waitForIdle()
            harness.workspace.state.value.document.library shouldBe Library.Unstyled

            keys { pressKey(Key.V) }

            harness.workspace.state.value.visionMenuOpen shouldBe true
            // Focus went into the menu, on its first row.
            val row = onNode(isFocused() and hasText("None"))
            row.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            harness.workspace.state.value.visionMenuOpen shouldBe false
            onNode(hasContentDescription("Color vision, None") and InWorkspace).assertIsFocused()
        }

    @Test
    fun aNumberKey_thenSpaceAndV_workWithNoClick() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }
            keys { pressKey(Key.Two) }
            waitUntil { harness.workspace.state.value.document.library == Library.Unstyled }
            waitForIdle()
            val seed = harness.graph.session.document.value.seed

            keys { pressKey(Key.Spacebar) }
            waitUntil { harness.graph.session.document.value.seed != seed }
            keys { pressKey(Key.V) }

            harness.workspace.state.value.visionMenuOpen shouldBe true
        }

    @Test
    fun heldB_showsGrayscale_andLetsGoWhenBComesUp() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }

            keys { keyDown(Key.B) }
            harness.workspace.state.value.grayscaleHeld shouldBe true
            named("Color vision, Achromatopsia") shouldBe true
            keys { keyUp(Key.B) }

            harness.workspace.state.value.grayscaleHeld shouldBe false
            harness.workspace.state.value.vision shouldBe VisionSimulation.None
            named("Color vision, None") shouldBe true
        }

    @Test
    fun heldB_letsGoWhenTheHolderLosesFocus() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }
            keys { keyDown(Key.B) }
            harness.workspace.state.value.grayscaleHeld shouldBe true

            onNodeWithContentDescription("Copy hex").requestFocus()
            waitForIdle()

            harness.workspace.state.value.grayscaleHeld shouldBe false
        }

    @Test
    fun heldB_letsGoWhenThePageHides() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }
            keys { keyDown(Key.B) }
            harness.workspace.state.value.grayscaleHeld shouldBe true

            harness.platform.environment.pageHides
                .tryEmit(Unit)

            waitUntil { !harness.workspace.state.value.grayscaleHeld }
        }
}
