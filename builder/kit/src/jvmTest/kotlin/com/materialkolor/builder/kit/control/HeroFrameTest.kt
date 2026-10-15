package com.materialkolor.builder.kit.control

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val Title = "Seed color"

@OptIn(ExperimentalTestApi::class)
class HeroFrameTest {
    @Test
    fun dialogWithAHero_eachWay_everySkin_isNamedByItsTitleKeepsFocusAndClosesOnEsc() =
        hostEachWay { skin, inTree ->
            var open by mutableStateOf(true)
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderDialog(
                        visible = open,
                        onDismissRequest = { open = false },
                        title = Title,
                        closeButton = true,
                        actions = { OverlayTestButton("done") },
                        hero = { OverlayTestButton("hero") },
                    ) { OverlayTestButton("body") }
                }
            }
            waitForIdle()

            onNode(hasOverlayPaneTitle(Title)).assertExists()
            onNodeWithText(Title).assertDoesNotExist()
            onNode(hasContentDescription("Close")).assertDoesNotExist()
            assertFocusStaysInside(first = "hero", last = "done")

            onNodeWithTag("body").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            open shouldBe false
        }

    @Test
    fun sheetWithAHero_eachWay_everySkin_isNamedByItsTitleKeepsFocusAndClosesOnEsc() =
        hostEachWay { skin, inTree ->
            var open by mutableStateOf(true)
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderSheet(
                        visible = open,
                        onDismissRequest = { open = false },
                        title = Title,
                        presentation = SheetPresentation.FullScreen,
                        footer = { OverlayTestButton("done") },
                        hero = { OverlayTestButton("hero") },
                    ) { OverlayTestButton("body") }
                }
            }
            waitForIdle()

            onNode(hasOverlayPaneTitle(Title)).assertExists()
            onNodeWithText(Title).assertDoesNotExist()
            onNode(hasContentDescription("Close")).assertDoesNotExist()
            assertFocusStaysInside(first = "hero", last = "done")

            onNodeWithTag("body").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            open shouldBe false
        }

    @Test
    fun buttonWithAFill_everySkin_isOneButtonNamedByItsLabel() =
        forEachSkin { _, skin ->
            var presses = 0
            setContent {
                ControlsHarness(skin) {
                    BuilderButton(
                        onClick = { presses++ },
                        label = "Done",
                        emphasis = Emphasis.Primary,
                        fill = Color(0xFF3F7A5C),
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasRole(Role.Button)).fetchSemanticsNodes().size shouldBe 1
            onNode(hasRole(Role.Button) and hasText("Done")).performClick()
            waitForIdle()

            presses shouldBe 1
        }

    /**
     * Focus is somewhere in the pane, Tab from the [last] stop wraps round inside it and Shift Tab
     * from the [first] stop does too.
     */
    private fun ComposeUiTest.assertFocusStaysInside(
        first: String,
        last: String,
    ) {
        val focusInside = isFocused() and hasAnyAncestor(hasOverlayPaneTitle(Title))
        onNode(focusInside).assertExists()

        onNodeWithTag(last).requestFocus()
        onNodeWithTag(last).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(first).assertIsFocused()

        onNodeWithTag(first).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        waitForIdle()
        onNodeWithTag(last).assertIsFocused()
    }
}
