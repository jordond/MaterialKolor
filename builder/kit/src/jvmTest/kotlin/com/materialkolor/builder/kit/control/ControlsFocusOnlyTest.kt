package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Modes: List<String> = listOf("Light", "Split", "Dark")

private val Styles: List<String> = listOf("Tonal spot", "Vibrant", "Expressive")

/**
 * Shows a segmented control and choice chips with [selectOnFocus] off in [skin], each between two
 * buttons, reading in [direction]. Every choice lands in [picks].
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showFocusOnlyGroups(
    skin: Skin,
    direction: LayoutDirection,
    picks: MutableList<String>,
) {
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Column {
                    BuilderButton({}, "Before")
                    var mode by remember { mutableStateOf(Modes.first()) }
                    BuilderSegmented(
                        options = Modes,
                        selected = mode,
                        onSelect = { next ->
                            picks += next
                            mode = next
                        },
                        label = "Preview mode",
                        selectOnFocus = false,
                    ) { it }
                    BuilderButton({}, "Between")
                    var style by remember { mutableStateOf(Styles.first()) }
                    BuilderChoiceChips(
                        options = Styles,
                        selected = style,
                        onSelect = { next ->
                            picks += next
                            style = next
                        },
                        label = "Scheme style",
                        selectOnFocus = false,
                    ) { it }
                    BuilderButton({}, "After")
                }
            }
        }
    }
    waitForIdle()
}

/**
 * The option labelled [label], by its radio role so the group's own text never matches.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.option(label: String): SemanticsNodeInteraction =
    onNode(hasText(label) and hasRole(Role.RadioButton))

/**
 * Presses [key] on whatever has focus and lets the focus land.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pressOnFocused(key: Key) {
    onNode(isFocused()).performKeyInput { pressKey(key) }
    waitForIdle()
}

/**
 * Presses Shift+Tab on whatever has focus and lets the focus land.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.shiftTabOnFocused() {
    onNode(isFocused()).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
    waitForIdle()
}

/**
 * Walks a focus-only group of [options], the first chosen, sitting between the [before] and
 * [after] buttons. [forward] is the arrow that reads on to the next option, [back] the other, and
 * [choose] the key that chooses the focused option.
 *
 * The arrows wrap at both ends without choosing, Shift+Tab and Tab leave the group from the
 * option the arrows reached even when the chosen one lies that way, and Tab back in lands on the
 * chosen one.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.walkFocusOnly(
    options: List<String>,
    before: String,
    after: String,
    forward: Key,
    back: Key,
    choose: Key,
    picks: MutableList<String>,
) {
    option(options.first()).requestFocus()
    waitForIdle()
    pressOnFocused(back)
    option(options.last()).assertIsFocused().assert(isNotSelected())
    pressOnFocused(forward)
    option(options.first()).assertIsFocused().assert(isSelected())
    picks shouldBe emptyList()

    pressOnFocused(forward)
    option(options[1]).assertIsFocused().assert(isNotSelected())
    shiftTabOnFocused()
    onNodeWithText(before).assertIsFocused()

    pressOnFocused(Key.Tab)
    option(options.first()).assertIsFocused().assert(isSelected())
    repeat(options.size - 1) { pressOnFocused(forward) }
    option(options.last()).assertIsFocused()
    pressOnFocused(choose)
    option(options.last()).assertIsFocused().assert(isSelected())
    picks shouldBe listOf(options.last())

    pressOnFocused(forward)
    option(options.first()).assertIsFocused().assert(isNotSelected())
    pressOnFocused(Key.Tab)
    onNodeWithText(after).assertIsFocused()
    picks shouldBe listOf(options.last())
}

@OptIn(ExperimentalTestApi::class)
class ControlsFocusOnlyTest {
    @Test
    fun segmentedAndChips_leftToRight_everySkin_focusOnlyWrapsAndTabLeavesFromTheArrowedOption() =
        forEachSkin { _, skin ->
            val picks = mutableListOf<String>()
            showFocusOnlyGroups(skin, LayoutDirection.Ltr, picks)
            walkFocusOnly(Modes, "Before", "Between", Key.DirectionRight, Key.DirectionLeft, Key.Enter, picks)
            picks.clear()
            walkFocusOnly(Styles, "Between", "After", Key.DirectionRight, Key.DirectionLeft, Key.Spacebar, picks)
        }

    @Test
    fun segmentedAndChips_rightToLeft_everySkin_focusOnlyWrapsAndTabLeavesFromTheArrowedOption() =
        forEachSkin { _, skin ->
            val picks = mutableListOf<String>()
            showFocusOnlyGroups(skin, LayoutDirection.Rtl, picks)
            walkFocusOnly(Modes, "Before", "Between", Key.DirectionLeft, Key.DirectionRight, Key.Enter, picks)
            picks.clear()
            walkFocusOnly(Styles, "Between", "After", Key.DirectionLeft, Key.DirectionRight, Key.Spacebar, picks)
        }
}
