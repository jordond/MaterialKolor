package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A declared element that counts its clicks.
 */
private const val ELEMENT = "element"

/**
 * The line the card keyboard focus shows ends on.
 */
private const val PIN_HINT = "Press Shift+Enter to pin this card"

/**
 * How many Tab presses it may take to reach something in the preview.
 */
private const val MAX_TABS = 8

@OptIn(ExperimentalTestApi::class)
class InspectPinKeyTest {
    @Test
    fun pinKey_onAFocusedElement_pinsItsCard_tabWalksTheActions_andEscUnpinsThenLeaves() =
        runComposeUiTest {
            var clicks = 0
            var leaves = 0
            setContent {
                Inspecting(PreviewMode.Light, remember { SplitState() }, onLeave = { leaves++ }) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .testTag(ELEMENT)
                            .previewRoles(*PrimaryPair)
                            .clickable { clicks++ },
                    )
                }
            }
            waitForIdle()
            tabTo(hasTestTag(ELEMENT))
            onNode(OnCard and hasText(PIN_HINT)).assertExists()
            onNode(OnCard and hasText("Pin this role")).assertDoesNotExist()

            onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
            waitForIdle()
            clicks shouldBe 0
            onNode(OnCard and hasText(PIN_HINT)).assertDoesNotExist()
            onNode(OnCard and hasText("Pin this role") and isFocused()).assertExists()
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onNode(OnCard and hasText("Show on ramp") and isFocused()).assertExists()
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onNode(OnCard and hasText("Jump to key color") and isFocused()).assertExists()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNode(OnCard and hasText("Pin this role")).assertDoesNotExist()
            leaves shouldBe 0
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            leaves shouldBe 1
        }

    @Test
    fun enterAndSpace_onAFocusedElement_stillActivateIt_whileShiftEnterDoesNot() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                Inspecting(PreviewMode.Light, remember { SplitState() }) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .testTag(ELEMENT)
                            .previewRoles(*PrimaryPair)
                            .clickable { clicks++ },
                    )
                }
            }
            waitForIdle()
            tabTo(hasTestTag(ELEMENT))

            onRoot().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            clicks shouldBe 1
            onRoot().performKeyInput { pressKey(Key.Spacebar) }
            waitForIdle()
            clicks shouldBe 2

            onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
            waitForIdle()
            clicks shouldBe 2
            onNode(OnCard and hasText("Pin this role")).assertExists()
        }

    /**
     * Press Tab until [matcher] holds focus.
     */
    private fun ComposeUiTest.tabTo(matcher: SemanticsMatcher) {
        var presses = 0
        while (onAllNodes(matcher and isFocused()).fetchSemanticsNodes().isEmpty()) {
            check(presses++ < MAX_TABS) { "Tab never reached it" }
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
        }
    }
}
