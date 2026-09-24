package com.materialkolor.builder.feature.poster

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

private val Slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

/** The level field, found by its text since a field that is off drops its SetText action. */
private val Field = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

private const val FLUENT_FIXED = "Fluent’s text colors are fixed and its ramps ignore contrast."

private const val TONES_KEPT = "Contrast changes the slots that follow a role. Slots picked by tone keep their tones."

@OptIn(ExperimentalTestApi::class)
class ContrastSectionTest {
    @Test
    fun fluent_turnsTheSliderAndFieldOffAndSaysWhy() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(library = Library.Fluent))) { context, dispatcher ->
                ContrastSection(context, dispatcher)
            }

            onNode(Slider).assertIsNotEnabled()
            onNode(Field).assertIsNotEnabled()
            onNodeWithText(FLUENT_FIXED).assertExists()
        }

    @Test
    fun custom_keepsTheSliderAndFieldOnWithItsNote() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(library = Library.Custom))) { context, dispatcher ->
                ContrastSection(context, dispatcher)
            }

            onNode(Slider).assertIsEnabled()
            onNode(Field).assertIsEnabled()
            onNodeWithText(TONES_KEPT).assertExists()
        }
}
