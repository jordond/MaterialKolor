package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The Inspect card's first action, so its presence means a card is pinned.
 */
private const val PIN_ROLE = "Pin this role"

@OptIn(ExperimentalTestApi::class)
class InspectLeaveTest {
    @Test
    fun escTwice_afterAPointerPin_leavesInspectWithItsToggleFocused() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            setContent { Canvas(host) }
            waitForIdle()
            onNodeWithContentDescription("New trip").performMouseInput { click() }
            waitForIdle()
            onNode(hasAnyAncestor(hasTestTag(INSPECT_CARD_TAG)) and hasText(PIN_ROLE)).assertExists()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            host.state.inspect shouldBe false
            onNodeWithText("Inspect").assertIsFocused().assertIsOff()
        }

    /**
     * A canvas host at the default view with Inspect on.
     */
    private fun inspecting(): CanvasHost =
        CanvasHost(view = ProjectViewState()).also { host -> host.state = host.state.copy(inspect = true) }
}
