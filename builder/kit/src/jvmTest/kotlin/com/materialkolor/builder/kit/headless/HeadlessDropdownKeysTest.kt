package com.materialkolor.builder.kit.headless

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideOverlaysInTreeForTest
import com.materialkolor.builder.kit.control.BuilderSelect
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.control.hasRole
import com.materialkolor.builder.kit.control.hasStateDescription
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Styles: List<String> = listOf("Tonal spot", "Vibrant", "Expressive", "Fidelity")

@OptIn(ExperimentalTestApi::class, KitTestApi::class)
class HeadlessDropdownKeysTest {
    @Test
    fun select_inTree_arrowsHomeAndEndWalkTheOptionsAndEnterOrSpacePicks() =
        forEachSkin { _, skin ->
            var style by mutableStateOf("Vibrant")
            setContent {
                ProvideOverlaysInTreeForTest {
                    ControlsHarness(skin) { BuilderSelect("Style", Styles, style, { style = it }) }
                }
            }
            val field = onNode(hasRole(Role.DropdownList))
            field.performClick()
            waitForIdle()
            option("Vibrant").assertIsFocused()
            press(Key.DirectionDown, lands = "Expressive")
            press(Key.DirectionDown, lands = "Fidelity")
            press(Key.DirectionDown, lands = "Tonal spot")
            press(Key.DirectionUp, lands = "Fidelity")
            press(Key.MoveHome, lands = "Tonal spot")
            press(Key.MoveEnd, lands = "Fidelity")
            press(Key.DirectionUp, lands = "Expressive")
            option("Expressive").performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            style shouldBe "Expressive"
            option("Tonal spot").assertDoesNotExist()
            onNode(hasRole(Role.DropdownList) and hasStateDescription("Expressive")).assertIsFocused()

            field.performClick()
            waitForIdle()
            press(Key.DirectionUp, lands = "Vibrant")
            option("Vibrant").performKeyInput { pressKey(Key.Spacebar) }
            waitForIdle()
            style shouldBe "Vibrant"
            option("Tonal spot").assertDoesNotExist()
        }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.option(label: String): SemanticsNodeInteraction =
    onNode(hasText(label) and hasRole(Role.RadioButton))

/** Presses [key] on the focused option and checks focus moved to the option named [lands]. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.press(
    key: Key,
    lands: String,
) {
    onNode(isFocused()).performKeyInput { pressKey(key) }
    waitForIdle()
    option(lands).assertIsFocused()
}
