package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.widget.SchemeChip
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val SchemeNames: List<String> = listOf("Tonal spot", "Vibrant", "Expressive", "Fidelity")

/**
 * Shows a group of scheme chips between the Before and After buttons, the first chosen, and hands
 * back the current choice. Every pick lands in [picks].
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showSchemeGroup(
    skin: Skin,
    picks: MutableList<String>,
    direction: LayoutDirection = LayoutDirection.Ltr,
    selectOnFocus: Boolean = true,
    folds: Boolean = false,
): () -> String {
    var chosen by mutableStateOf(SchemeNames.first())
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalFoldsStateIntoName provides folds,
            ) {
                Column {
                    BuilderButton({}, "Before", Modifier.testTag("before"))
                    BuilderChoiceGroup(
                        options = SchemeNames,
                        selected = chosen,
                        onSelect = { name ->
                            picks += name
                            chosen = name
                        },
                        label = "Palette style",
                        modifier = Modifier.testTag("group"),
                        selectOnFocus = selectOnFocus,
                    ) { name, isSelected, optionModifier ->
                        SchemeChip(
                            primary = Color(0xFF6750A4),
                            secondaryContainer = Color(0xFFE8DEF8),
                            tertiaryContainer = Color(0xFFFFD8E4),
                            selected = isSelected,
                            onClick = {
                                picks += name
                                chosen = name
                            },
                            label = name,
                            modifier = optionModifier,
                        )
                    }
                    BuilderButton({}, "After", Modifier.testTag("after"))
                }
            }
        }
    }
    waitForIdle()
    return { chosen }
}

/** The scheme chip named [name], by its radio role so the tooltip's text never matches. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.chip(name: String): SemanticsNodeInteraction =
    onNode(hasContentDescription(name, substring = true) and hasRole(Role.RadioButton))

/** Presses [key] on whatever has focus and lets the focus land. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pressOnChosen(key: Key) {
    onNode(isFocused()).performKeyInput { pressKey(key) }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
class ChoiceGroupTest {
    @Test
    fun choiceGroup_schemeChips_everySkin_isOneTabStop() =
        forEachSkin { _, skin ->
            showSchemeGroup(skin, mutableListOf())

            onNodeWithTag("before").requestFocus()
            pressOnChosen(Key.Tab)
            chip("Tonal spot").assertIsFocused().assertIsSelected()
            pressOnChosen(Key.Tab)
            onNodeWithTag("after").assertIsFocused()
        }

    @Test
    fun choiceGroup_arrowsHomeAndEnd_everySkin_moveTheChoiceAndTheFocus() =
        forEachSkin { _, skin ->
            val picks = mutableListOf<String>()
            val chosen = showSchemeGroup(skin, picks)

            chip("Tonal spot").requestFocus()
            pressOnChosen(Key.DirectionRight)
            chosen() shouldBe "Vibrant"
            chip("Vibrant").assertIsFocused().assertIsSelected()
            pressOnChosen(Key.DirectionLeft)
            chosen() shouldBe "Tonal spot"
            pressOnChosen(Key.MoveEnd)
            chosen() shouldBe "Fidelity"
            chip("Fidelity").assertIsFocused()
            pressOnChosen(Key.MoveHome)
            chosen() shouldBe "Tonal spot"
            chip("Tonal spot").assertIsFocused()
            picks shouldBe listOf("Vibrant", "Tonal spot", "Fidelity", "Tonal spot")
        }

    @Test
    fun choiceGroup_rightToLeft_everySkin_leftMovesOn() =
        forEachSkin { _, skin ->
            val chosen = showSchemeGroup(skin, mutableListOf(), direction = LayoutDirection.Rtl)

            chip("Tonal spot").requestFocus()
            pressOnChosen(Key.DirectionLeft)
            chosen() shouldBe "Vibrant"
            chip("Vibrant").assertIsFocused()
            pressOnChosen(Key.DirectionRight)
            chosen() shouldBe "Tonal spot"
            pressOnChosen(Key.MoveEnd)
            chosen() shouldBe "Fidelity"
        }

    @Test
    fun choiceGroup_selectOnFocusOff_everySkin_picksOnlyOnEnter() =
        forEachSkin { _, skin ->
            val picks = mutableListOf<String>()
            val chosen = showSchemeGroup(skin, picks, selectOnFocus = false)

            chip("Tonal spot").requestFocus()
            pressOnChosen(Key.DirectionRight)
            pressOnChosen(Key.DirectionRight)
            chip("Expressive").assertIsFocused().assertIsNotSelected()
            chosen() shouldBe "Tonal spot"
            picks shouldBe emptyList()

            pressOnChosen(Key.Enter)
            chosen() shouldBe "Expressive"
            chip("Expressive").assertIsFocused().assertIsSelected()
            picks shouldBe listOf("Expressive")
        }

    @Test
    fun choiceGroup_foldOn_everySkin_namesTheGroupAsText() =
        forEachSkin { _, skin ->
            showSchemeGroup(skin, mutableListOf(), folds = true)

            onNodeWithTag("group").assert(hasText("Palette style")).assert(hasNoContentDescription())
            chip("Tonal spot").assert(hasContentDescriptionExactly("Tonal spot, radio, selected"))
        }
}
