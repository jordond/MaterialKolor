package com.materialkolor.sample.fluent

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.materialkolor.sample.fluent.ui.FluentSampleApp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FluentSampleAppTest {
    @Test
    fun sampleApp_rendersTheSeedPickerAndTheRamp() =
        runComposeUiTest {
            setContent { FluentSampleApp() }
            waitForIdle()

            onNodeWithText("Windows").assertIsDisplayed()
            onNodeWithText("Slate").assertIsDisplayed()
            onNodeWithText("base").assertIsDisplayed()
            onNodeWithText("light3").assertIsDisplayed()
        }

    @Test
    fun sampleApp_flipsBetweenLightAndDark() =
        runComposeUiTest {
            setContent { FluentSampleApp() }
            waitForIdle()

            onNodeWithText("Switch to light").performClick()
            waitForIdle()

            onNodeWithText("Switch to dark").assertIsDisplayed()
        }

    @Test
    fun sampleApp_picksUpASeedFromTheLabelAsWellAsTheSwatch() =
        runComposeUiTest {
            setContent { FluentSampleApp() }
            waitForIdle()

            onNodeWithText("Seed, Windows").assertIsDisplayed()

            // Clicking the label, not the swatch. This failed while the click was on the swatch
            // alone, which is how that bug was found.
            onNodeWithText("Teal").performClick()
            waitForIdle()

            onNodeWithText("Seed, Teal").assertIsDisplayed()
        }
}
