package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * What the Trips app's floating action button reads as.
 */
private const val NEW_TRIP = "New trip"

/**
 * The first action of a pinned card, so its presence means a card is pinned.
 */
private const val PIN_ROLE = "Pin this role"

/**
 * How many Tab presses it may take to reach the Trips app from the canvas tabs.
 */
private const val MAX_TABS = 12

/**
 * Anything drawn on the Inspect card.
 */
private val OnCard: SemanticsMatcher = hasAnyAncestor(hasTestTag(INSPECT_CARD_TAG))

@OptIn(ExperimentalTestApi::class)
class InspectPinKeyTest {
    @Test
    fun shiftEnter_onTheFocusedFab_pinsItsCard_whosePinDispatchesSetPin_thenEscTwiceRefocusesTheToggle() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val host = CanvasHost(view = ProjectViewState())
            host.state = host.state.copy(inspect = true)
            val result = ThemeResolver().resolve(host.state.document)
            setContent { Canvas(host) }
            waitForIdle()
            onNodeWithText("App").requestFocus()
            val fab = hasContentDescription(NEW_TRIP) and isFocused()
            var presses = 0
            while (onAllNodes(fab).fetchSemanticsNodes().isEmpty()) {
                check(presses++ < MAX_TABS) { "Tab never reached the Trips app's floating action button" }
                onRoot().performKeyInput { pressKey(Key.Tab) }
                waitForIdle()
            }
            onNode(OnCard and hasText(PIN_ROLE)).assertDoesNotExist()

            onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE) and isFocused()).assertExists()
            val before = host.actions.size
            onRoot().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            val argb = result.roles[Role.PrimaryContainer, false].argb
            val pin = DocumentChange.SetPin(Role.PrimaryContainer, PinMode.Light, argb)
            host.actions.drop(before) shouldBe listOf(WorkspaceAction.Edit(pin, EditPhase.Discrete))

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertDoesNotExist()
            host.state.inspect shouldBe true
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            host.state.inspect shouldBe false
            onNodeWithText("Inspect").assertIsFocused().assertIsOff()
        }
}
