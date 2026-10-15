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
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.topbar.LibraryChoice
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * V opens the dock's Vision menu, and a held B shows the canvas in grayscale.
 */
@OptIn(ExperimentalTestApi::class)
class DockKeysTest {
    private val harness = CommandHarness()

    @AfterTest
    fun tearDown() {
        harness.close()
    }

    /**
     * With overlays in the page, as the web draws them, where the menu takes focus into its rows.
     * Material's own popup menu on the desktop leaves its rows unfocused. A pick closes the menu here
     * the way Esc does, through `onDismissRequest`.
     */
    @Test
    fun v_opensTheVisionMenuWithFocusInIt_andClosingItHandsFocusBackToItsButton() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show(inTree = true) }

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
            keys { pressKey(Key.Three) }
            waitUntil(
                timeoutMillis = WAIT_MILLIS,
            ) { harness.workspace.state.value.document.library == Library.Unstyled }
            waitForIdle()
            val seed = harness.graph.session.document.value.seed

            keys { pressKey(Key.Spacebar) }
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.graph.session.document.value.seed != seed }
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

            waitUntil(timeoutMillis = WAIT_MILLIS) { !harness.workspace.state.value.grayscaleHeld }
        }
}
