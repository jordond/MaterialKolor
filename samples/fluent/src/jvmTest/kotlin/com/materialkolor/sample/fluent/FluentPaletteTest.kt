package com.materialkolor.sample.fluent

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.sample.fluent.ui.FluentSampleApp
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.ui.SampleTags
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FluentPaletteTest {
    @Test
    fun palette_namesEveryShadeAndComparesAgainstFluentsOwnLookup() =
        runComposeUiTest {
            setContent { FluentSampleApp() }
            waitForIdle()

            onNodeWithTag(SampleTags.section(AppSection.Palette)).performClick()
            waitForIdle()

            for (name in listOf("dark3", "dark2", "dark1", "base", "light1", "light2", "light3")) {
                onNodeWithText(name).assertExists()
            }
            onNodeWithText("rememberFluentColors").assertExists()
            onNodeWithText("generateShades").assertExists()
        }
}
