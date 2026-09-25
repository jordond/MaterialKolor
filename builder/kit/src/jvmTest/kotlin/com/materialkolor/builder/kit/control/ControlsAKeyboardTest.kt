package com.materialkolor.builder.kit.control

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Roving focus in the two single choice groups, the segmented control and the choice chips.
 */
@OptIn(ExperimentalTestApi::class)
class ControlsAKeyboardTest {
    @Test
    fun segmented_arrowKeys_moveTheChoiceAndTheFocus() =
        runComposeUiTest {
            var mode by mutableStateOf("Light")
            eachActionSkin(
                content = {
                    BuilderSegmented(ActionPreviewModes, mode, onSelect = { mode = it }, label = "Preview mode") { it }
                },
            ) {
                mode = "Light"
                waitForIdle()
                onNodeWithText("Light").requestFocus()
                onNodeWithText("Light").assertIsFocused()

                press("Light", Key.DirectionRight)
                mode shouldBe "Split"
                onNodeWithText("Split").assertIsSelected().assertIsFocused()

                press("Split", Key.DirectionLeft)
                mode shouldBe "Light"

                press("Light", Key.DirectionLeft)
                mode shouldBe "Dark"
                onNodeWithText("Dark").assertIsSelected().assertIsFocused()

                press("Dark", Key.MoveHome)
                mode shouldBe "Light"
            }
        }

    @Test
    fun segmented_tabThenEnd_landsOnTheChoiceAndJumpsToTheLastOption() =
        runComposeUiTest {
            var mode by mutableStateOf("Split")
            eachActionSkin(
                content = {
                    BuilderButton(onClick = {}, label = "Before")
                    BuilderSegmented(ActionPreviewModes, mode, onSelect = { mode = it }, label = "Preview mode") { it }
                },
            ) {
                mode = "Split"
                tabIntoGroup(landsOn = "Split")
                press("Split", Key.MoveEnd)
                mode shouldBe "Dark"
                onNodeWithText("Dark").assertIsSelected().assertIsFocused()
            }
        }

    @Test
    fun segmented_rightToLeft_arrowsFollowTheReadingDirection() =
        runComposeUiTest {
            var mode by mutableStateOf("Light")
            eachActionSkin(
                direction = LayoutDirection.Rtl,
                content = {
                    BuilderButton(onClick = {}, label = "Before")
                    BuilderSegmented(ActionPreviewModes, mode, onSelect = { mode = it }, label = "Preview mode") { it }
                },
            ) {
                mode = "Light"
                tabIntoGroup(landsOn = "Light")
                walkRightToLeft(ActionPreviewModes) { mode }
            }
        }

    @Test
    fun segmented_declinedArrowKey_leavesTheFocusAloneOnALaterChange() =
        runComposeUiTest {
            var mode by mutableStateOf("Light")
            eachActionSkin(
                content = {
                    BuilderSegmented(ActionPreviewModes, mode, onSelect = {}, label = "Preview mode") { it }
                    BuilderButton(onClick = {}, label = "After")
                },
            ) {
                mode = "Light"
                waitForIdle()
                onNodeWithText("Light").requestFocus()
                press("Light", Key.DirectionRight)
                onNodeWithText("Light").assertIsSelected()

                onNodeWithText("After").requestFocus()
                // An undo lands on some other option while the focus is elsewhere.
                mode = "Dark"
                waitForIdle()
                onNodeWithText("After").assertIsFocused()
            }
        }

    @Test
    fun choiceChips_tabArrowsHomeAndEnd_moveTheChoiceAndWrap() =
        runComposeUiTest {
            var style by mutableStateOf("Vibrant")
            eachActionSkin(
                content = {
                    BuilderButton(onClick = {}, label = "Before")
                    BuilderChoiceChips(ActionPaletteStyles, style, onSelect = { style = it }, label = "Style") { it }
                },
            ) {
                style = "Vibrant"
                tabIntoGroup(landsOn = "Vibrant")

                press("Vibrant", Key.DirectionRight)
                style shouldBe "Expressive"
                onNodeWithText("Expressive").assertIsSelected().assertIsFocused()

                press("Expressive", Key.MoveEnd)
                style shouldBe ActionPaletteStyles.last()

                press(ActionPaletteStyles.last(), Key.DirectionRight)
                style shouldBe ActionPaletteStyles.first()
                onNodeWithText(ActionPaletteStyles.first()).assertIsSelected().assertIsFocused()

                press(ActionPaletteStyles.first(), Key.DirectionUp)
                style shouldBe ActionPaletteStyles.last()

                press(ActionPaletteStyles.last(), Key.MoveHome)
                style shouldBe ActionPaletteStyles.first()
            }
        }

    @Test
    fun choiceChips_rightToLeft_arrowsFollowTheReadingDirection() =
        runComposeUiTest {
            var style by mutableStateOf(ActionPaletteStyles.first())
            eachActionSkin(
                direction = LayoutDirection.Rtl,
                content = {
                    BuilderButton(onClick = {}, label = "Before")
                    BuilderChoiceChips(ActionPaletteStyles, style, onSelect = { style = it }, label = "Style") { it }
                },
            ) {
                style = ActionPaletteStyles.first()
                tabIntoGroup(landsOn = ActionPaletteStyles.first())
                walkRightToLeft(ActionPaletteStyles) { style }
            }
        }
}

/**
 * Presses [key] on the option labelled [option] and lets the choice land.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.press(
    option: String,
    key: Key,
) {
    onNodeWithText(option).performKeyInput { pressKey(key) }
    waitForIdle()
}

/**
 * Focuses the Before button and tabs once, which has to land on the chosen option [landsOn].
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tabIntoGroup(landsOn: String) {
    waitForIdle()
    onNodeWithText("Before").requestFocus()
    press("Before", Key.Tab)
    onNodeWithText(landsOn).assertIsSelected().assertIsFocused()
}

/**
 * Walks a right to left group of [options], focused on the first. Left moves on towards the end,
 * right moves back and wraps, and End and Home still mean the last and the first.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.walkRightToLeft(
    options: List<String>,
    current: () -> String,
) {
    press(options[0], Key.DirectionLeft)
    current() shouldBe options[1]
    onNodeWithText(options[1]).assertIsFocused()

    press(options[1], Key.DirectionRight)
    current() shouldBe options[0]

    press(options[0], Key.DirectionRight)
    current() shouldBe options.last()
    onNodeWithText(options.last()).assertIsFocused()

    press(options.last(), Key.MoveHome)
    current() shouldBe options[0]

    press(options[0], Key.MoveEnd)
    current() shouldBe options.last()
}
