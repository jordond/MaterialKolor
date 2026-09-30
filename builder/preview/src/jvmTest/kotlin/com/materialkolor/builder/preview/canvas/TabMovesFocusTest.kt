package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import com.materialkolor.builder.preview.material.TripDetail
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.Trips
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TabMovesFocusTest {
    @Test
    fun tab_inABareMultiLineField_typesATab() =
        runComposeUiTest {
            var text by mutableStateOf("")
            setContent { Between { MultiLineField(text, { typed -> text = typed }, Modifier) } }

            focusField()
            onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Tab) }
            waitForIdle()

            text shouldBe "\t"
            onNode(hasSetTextAction()).assertIsFocused()
        }

    @Test
    fun tab_inAMultiLineFieldUsingIt_movesOnWithoutTyping() =
        runComposeUiTest {
            var text by mutableStateOf("")
            setContent {
                Between {
                    MultiLineField(text, { typed -> text = typed }, Modifier.tabMovesFocus(LocalFocusManager.current))
                }
            }

            focusField()
            onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Tab) }
            waitForIdle()

            text shouldBe ""
            onNode(hasClickAction() and hasText(After)).assertIsFocused()
        }

    @Test
    fun shiftTab_inAMultiLineFieldUsingIt_movesBackWithoutTyping() =
        runComposeUiTest {
            var text by mutableStateOf("")
            setContent {
                Between {
                    MultiLineField(text, { typed -> text = typed }, Modifier.tabMovesFocus(LocalFocusManager.current))
                }
            }

            focusField()
            onNode(hasSetTextAction()).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
            waitForIdle()

            text shouldBe ""
            onNode(hasClickAction() and hasText(Before)).assertIsFocused()
        }

    @Test
    fun tabAndShiftTab_inTheTripsNote_leaveItUntyped() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                MaterialTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        TripDetail(Trips.first(), state)
                        Button(onClick = {}) { Text(After) }
                    }
                }
            }

            focusField()
            onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            state.text shouldBe ""
            onNode(hasClickAction() and hasText(After)).assertIsFocused()

            focusField()
            onNode(hasSetTextAction()).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
            waitForIdle()
            state.text shouldBe ""
            onNode(hasSetTextAction()).assertIsNotFocused()
            onNode(isToggleable() and hasText(PackingItem.entries.last().label)).assertIsFocused()
        }
}

private const val Before = "Before"
private const val After = "After"

/**
 * A button, then [field], then another button.
 */
@Composable
private fun Between(field: @Composable () -> Unit) {
    MaterialTheme {
        Column {
            Button(onClick = {}) { Text(Before) }
            field()
            Button(onClick = {}) { Text(After) }
        }
    }
}

@Composable
private fun MultiLineField(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text("Note") },
        minLines = 2,
    )
}

/**
 * Focuses the one text field in the content, the way a click would.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.focusField() {
    onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.RequestFocus)
    waitForIdle()
    onNode(hasSetTextAction()).assertIsFocused()
}
