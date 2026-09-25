package com.materialkolor.builder.kit.control

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ControlsCFocusTest {
    @Test
    fun sidePanel_openedFromTrigger_trapsTabAndReturnsFocus() =
        forEachSkin { _, skin ->
            checkPanelFocus(skin, "Projects") { visible, onDismissRequest, trigger ->
                BuilderSidePanel(visible, onDismissRequest, "Projects", returnFocusTo = trigger) {
                    OverlayTestButton("first")
                    OverlayTestButton("second")
                }
            }
        }

    @Test
    fun sheet_openedFromTrigger_trapsTabAndReturnsFocus() =
        forEachSkin { _, skin ->
            checkPanelFocus(skin, "Export") { visible, onDismissRequest, trigger ->
                BuilderSheet(visible, onDismissRequest, "Export", SheetPresentation.EndPanel, returnFocusTo = trigger) {
                    OverlayTestButton("first")
                    OverlayTestButton("second")
                }
            }
        }

    @Test
    fun bottomSheet_tabOntoARowBelowTheFold_raisesTheSheetUntilItShows() =
        forEachSkin { _, skin ->
            lateinit var state: BottomSheetState
            setContent {
                ControlsHarness(skin) {
                    Box(Modifier.size(400.dp, 600.dp)) {
                        state = rememberBottomSheetState()
                        BuilderBottomSheet(state, label = "Poster") {
                            OverlayTestButton("top")
                            Spacer(Modifier.height(120.dp))
                            OverlayTestButton("middle")
                            Spacer(Modifier.height(200.dp))
                            OverlayTestButton("bottom")
                        }
                    }
                }
            }
            onNode(hasContentDescription("Poster") and hasRole(Role.Button)).requestFocus()
            val steps = listOf(
                "top" to BottomSheetDetent.Peek,
                "middle" to BottomSheetDetent.Half,
                "bottom" to BottomSheetDetent.Full,
            )
            for ((tag, detent) in steps) {
                onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
                waitForIdle()
                onNodeWithTag(tag).assertIsFocused()
                state.detent shouldBe detent
            }
        }

    @Test
    fun bottomSheet_detentLabel_isWhatTheHandleReads() =
        forEachSkin { _, skin ->
            setContent {
                ControlsHarness(skin) {
                    BuilderBottomSheet(
                        state = rememberBottomSheetState(),
                        label = "Poster",
                        detentLabel = { detent -> "Poster at ${detent.name.lowercase()}" },
                    ) { BuilderText("#6750A4") }
                }
            }
            onNode(hasContentDescription("Poster") and hasRole(Role.Button))
                .assert(hasStateDescription("Poster at peek"))
        }

    @Test
    fun select_downAndAltDown_openWithFocusOnTheChosenOption() =
        forEachSkin { _, skin ->
            // Material3 draws its own exposed dropdown, which keeps its own keys.
            if (skin.library == Library.Material3) return@forEachSkin
            var style by mutableStateOf("Vibrant")
            setContent {
                ControlsHarness(skin) {
                    BuilderSelect("Style", listOf("Tonal spot", "Vibrant", "Expressive"), style, { style = it })
                }
            }
            val chosen = onNode(hasText("Vibrant") and hasRole(Role.RadioButton))
            for (alt in listOf(false, true)) {
                val field = onNode(hasRole(Role.DropdownList))
                field.requestFocus()
                field.performKeyInput {
                    if (alt) withKeyDown(Key.AltLeft) { pressKey(Key.DirectionDown) } else pressKey(Key.DirectionDown)
                }
                waitForIdle()
                chosen.assertIsFocused()
                chosen.performClick()
                waitForIdle()
                chosen.assertDoesNotExist()
            }
        }

    @Test
    fun toastHost_focusOnTheAction_holdsTheToastThenResumesWithTheTimeLeft() =
        forEachSkin { _, skin ->
            val toasts = showHostOverPage(skin)
            toasts.show("Pin removed", actionLabel = "Undo") {}
            mainClock.advanceTimeBy(2_000)
            onNode(hasText("Undo") and hasRole(Role.Button)).requestFocus()
            mainClock.advanceTimeBy(30_000)
            onNodeWithText("Pin removed").assertExists()
            onNodeWithTag("page").requestFocus()
            mainClock.advanceTimeBy(7_000)
            onNodeWithText("Pin removed").assertExists()
            mainClock.advanceTimeBy(1_500)
            onNodeWithText("Pin removed").assertDoesNotExist()
        }

    @Test
    fun toastHost_pointerOnTheToast_holdsItThenResumesWithTheTimeLeft() =
        forEachSkin { _, skin ->
            val toasts = showHostOverPage(skin)
            toasts.show("Saved")
            mainClock.advanceTimeBy(1_000)
            onNodeWithText("Saved").performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(10_000)
            onNodeWithText("Saved").assertExists()
            onNodeWithTag("page").performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(2_500)
            onNodeWithText("Saved").assertExists()
            mainClock.advanceTimeBy(1_000)
            onNodeWithText("Saved").assertDoesNotExist()
        }
}

/**
 * A focusable page above a toast host, with the clock handed to the test.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showHostOverPage(skin: Skin): BuilderToastHostState {
    val toasts = BuilderToastHostState()
    setContent {
        ControlsHarness(skin) {
            Column {
                OverlayTestButton("page")
                BuilderToastHost(toasts)
            }
        }
    }
    waitForIdle()
    mainClock.autoAdvance = false
    return toasts
}

/**
 * Opens [panel] from a trigger, walks Tab both ways around its close button and two rows without
 * leaving it, closes it on Esc and checks that focus is back on the trigger.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.checkPanelFocus(
    skin: Skin,
    title: String,
    panel: @Composable (visible: Boolean, onDismissRequest: () -> Unit, trigger: FocusRequester) -> Unit,
) {
    var open by mutableStateOf(false)
    val trigger = FocusRequester()
    setContent {
        ControlsHarness(skin) {
            Column {
                Box(
                    Modifier
                        .testTag("trigger")
                        .size(40.dp)
                        .focusRequester(trigger)
                        .focusable(),
                )
                Box(Modifier.testTag("after").size(40.dp).focusable())
                panel(open, { open = false }, trigger)
            }
        }
    }
    onNodeWithTag("trigger").requestFocus()
    open = true
    waitForIdle()
    val close = onNode(hasContentDescription("Close") and hasRole(Role.Button))
    close.assertIsFocused()
    // The page keeps its own focused node under the modal layer, so each step names where it starts.
    var from = close
    for (tag in listOf("first", "second")) {
        from.performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        from = onNodeWithTag(tag)
        from.assertIsFocused()
    }
    onNodeWithTag("second").performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
    close.assertIsFocused()
    close.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
    waitForIdle()
    onNodeWithTag("second").assertIsFocused()
    onNodeWithTag("after").assertIsNotFocused()
    onNodeWithTag("second").performKeyInput { pressKey(Key.Escape) }
    waitForIdle()
    open shouldBe false
    onNode(hasOverlayPaneTitle(title)).assertDoesNotExist()
    onNodeWithTag("trigger").assertIsFocused()
}
