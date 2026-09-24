package com.materialkolor.builder.kit.control

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OverlayFocusTest {
    @Test
    fun dialogWithAFieldAskingForFocus_eachWay_leavesFocusOnTheFieldBelowAButton() =
        hostEachWay { skin, inTree ->
            var open by mutableStateOf(false)
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderDialog(open, { open = false }, "Rename", actions = { OverlayTestButton("save") }) {
                        OverlayTestButton("above")
                        val field = remember { FocusRequester() }
                        Box(
                            Modifier
                                .testTag("field")
                                .size(40.dp)
                                .focusRequester(field)
                                .focusable(),
                        )
                        LaunchedEffect(Unit) { field.requestFocus() }
                    }
                }
            }
            open = true
            waitForIdle()
            onNodeWithTag("above").assertExists()
            onNodeWithTag("field").assertIsFocused()
        }

    @Test
    fun toastUndo_inTree_joinsAModalsTabCycleButNotAPopoversAndLeavesFocusInTheDialog() =
        forEachSkin { _, skin ->
            val toasts = BuilderToastHostState()
            var menu by mutableStateOf(false)
            setContent {
                HostOverlays(skin, inTree = true) {
                    Box(Modifier.fillMaxSize()) {
                        BuilderToastHost(toasts)
                        BuilderDialog(
                            visible = true,
                            onDismissRequest = {},
                            title = "Export",
                            actions = {
                                OverlayTestButton("cancel")
                                OverlayTestButton("confirm")
                            },
                        ) {
                            val items = listOf(BuilderMenuItem("Duplicate", {}), BuilderMenuItem("Delete", {}))
                            BuilderMenu(menu, { menu = false }, items) { BuilderText("Theme") }
                        }
                    }
                }
            }
            waitForIdle()
            toasts.show("Deleted Sunset", "Undo", ToastDuration.Indefinite) {}
            waitForIdle()
            val undo = onNode(hasText("Undo") and hasRole(Role.Button))
            onNodeWithTag("confirm").requestFocus()
            onNodeWithTag("confirm").performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            undo.assertIsFocused()
            undo.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onNodeWithTag("cancel").assertIsFocused()
            onNodeWithTag("cancel").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
            waitForIdle()
            undo.assertIsFocused()

            menu = true
            waitForIdle()
            val duplicate = onNode(hasText("Duplicate") and hasRole(Role.Button))
            val delete = onNode(hasText("Delete") and hasRole(Role.Button))
            duplicate.assertIsFocused()
            duplicate.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            delete.assertIsFocused()
            delete.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            duplicate.assertIsFocused()
            duplicate.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
            waitForIdle()
            delete.assertIsFocused()
            menu shouldBe true

            delete.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            menu shouldBe false
            undo.requestFocus()
            undo.assertIsFocused()
            undo.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            toasts.toasts shouldBe emptyList()
            onNode(isFocused() and hasAnyAncestor(hasOverlayPaneTitle("Export"))).assertExists()
        }

    @Test
    fun toastHostLeavingWhileUndoHasFocus_inTree_leavesFocusInTheDialog() =
        forEachSkin { _, skin ->
            val toasts = BuilderToastHostState()
            var toastHost by mutableStateOf(true)
            setContent {
                HostOverlays(skin, inTree = true) {
                    Box(Modifier.fillMaxSize()) {
                        if (toastHost) BuilderToastHost(toasts)
                        BuilderDialog(
                            visible = true,
                            onDismissRequest = {},
                            title = "Export",
                            actions = {
                                OverlayTestButton("cancel")
                                OverlayTestButton("confirm")
                            },
                        ) { BuilderText("Kotlin") }
                    }
                }
            }
            waitForIdle()
            toasts.show("Deleted Sunset", "Undo", ToastDuration.Indefinite) {}
            waitForIdle()
            val undo = onNode(hasText("Undo") and hasRole(Role.Button))
            undo.requestFocus()
            undo.assertIsFocused()
            toastHost = false
            waitForIdle()
            undo.assertDoesNotExist()
            onNode(isFocused() and hasAnyAncestor(hasOverlayPaneTitle("Export"))).assertExists()
        }

    @Test
    fun toastUndo_inTree_withNoModalOpen_handsFocusBackToThePage() =
        forEachSkin { _, skin ->
            val toasts = BuilderToastHostState()
            setContent {
                HostOverlays(skin, inTree = true) {
                    Column {
                        OverlayTestButton("first")
                        OverlayTestButton("second")
                        BuilderToastHost(toasts)
                    }
                }
            }
            waitForIdle()
            toasts.show("Deleted Sunset", "Undo", ToastDuration.Indefinite) {}
            waitForIdle()
            val undo = onNode(hasText("Undo") and hasRole(Role.Button))
            onNodeWithTag("second").requestFocus()
            onNodeWithTag("second").performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            undo.assertIsFocused()
            undo.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            toasts.toasts shouldBe emptyList()
            onNodeWithTag("second").assertIsFocused()
        }
}
