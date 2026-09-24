package com.materialkolor.builder.feature.poster

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

/** The four levels as the choice names them. */
private val Levels = listOf("Reduced", "Standard", "Medium", "High")

private const val FLUENT_FIXED = "Fluent’s text colors are fixed and its ramps ignore contrast."

private const val TONES_KEPT = "Contrast changes the slots that follow a role. Slots picked by tone keep their tones."

@OptIn(ExperimentalTestApi::class)
class ContrastSectionTest {
    @Test
    fun fluent_turnsTheLevelsOffAndSaysWhy() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(library = Library.Fluent))) { context, dispatcher ->
                ContrastSection(context, dispatcher)
            }

            Levels.forEach { level -> onNodeWithText(level).assertIsNotEnabled() }
            onNodeWithText(FLUENT_FIXED).assertExists()
        }

    @Test
    fun custom_keepsTheLevelsOnWithItsNote() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(library = Library.Custom))) { context, dispatcher ->
                ContrastSection(context, dispatcher)
            }

            Levels.forEach { level -> onNodeWithText(level).assertIsEnabled() }
            onNodeWithText(TONES_KEPT).assertExists()
        }
}
