package com.materialkolor.sample.fluent

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.sample.fluent.ui.FluentSampleApp
import com.materialkolor.sample.shared.ui.SampleTags
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FluentKeyboardTest {
    @Test
    fun addTask_keepsTheFocusInTheField() =
        runComposeUiTest {
            setContent { FluentSampleApp() }
            waitForIdle()

            onNodeWithTag(SampleTags.TaskInput).performTextInput("Water the plants")
            onNodeWithTag(SampleTags.AddTask).performClick()
            waitForIdle()

            onNodeWithTag(SampleTags.TaskInput).assertIsFocused()
        }

    @Test
    fun clearDialog_closesOnEscapeAndKeepsEveryTask() =
        runComposeUiTest {
            setContent { FluentSampleApp() }
            waitForIdle()

            onNodeWithTag(SampleTags.ClearDone).performClick()
            waitForIdle()
            onNodeWithTag(SampleTags.ClearDialog).performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            onNodeWithTag(SampleTags.ClearDialog, useUnmergedTree = true).assertDoesNotExist()
            onNodeWithTag(SampleTags.taskRow(1), useUnmergedTree = true).assertExists()
        }
}
