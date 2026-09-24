package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/** A declared element for the card to pin to. */
private const val ELEMENT = "element"

/** The first action of a pinned card, so its presence means a card is pinned. */
private const val PIN_ROLE = "Pin this role"

@OptIn(ExperimentalTestApi::class)
class InspectFocusTest {
    @Test
    fun pointerPin_thenANewMode_escStillLeavesInspect() =
        runComposeUiTest {
            var shown by mutableStateOf(PreviewMode.Light)
            var leaves = 0
            setContent {
                Inspecting(shown, remember { SplitState() }, onLeave = { leaves++ }) { Element() }
            }
            waitForIdle()
            onNodeWithTag(ELEMENT).performClick()
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertExists()

            shown = PreviewMode.Dark
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            leaves shouldBe 1
        }

    @Test
    fun pointerPin_holdsFocusWithNoRing_untilEscBringsTheKeyboardAndTheRing() =
        runComposeUiTest {
            var focus = Color.Unspecified
            setContent {
                Inspecting(PreviewMode.Light, remember { SplitState() }) {
                    focus = LocalBuilderTokens.current.focus
                    Element()
                }
            }
            waitForIdle()
            onNodeWithTag(ELEMENT).performClick()
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertExists()
            previewEdge() shouldNotBe focus.toArgb()

            // Esc unpins and leaves the preview holding focus, now with the keyboard in use.
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertDoesNotExist()
            previewEdge() shouldBe focus.toArgb()
        }

    /** The color at the middle of the preview's start edge, where a focus ring would be drawn. */
    private fun ComposeUiTest.previewEdge(): Int {
        val image = onNodeWithTag(PREVIEW_TAG).captureToImage().toPixelMap()
        return image[0, image.height / 2].toArgb()
    }
}

/** A small element declaring a role pair, at the preview's top start corner. */
@Composable
private fun Element() {
    Box(Modifier.size(40.dp).testTag(ELEMENT).previewRoles(*PrimaryPair))
}
