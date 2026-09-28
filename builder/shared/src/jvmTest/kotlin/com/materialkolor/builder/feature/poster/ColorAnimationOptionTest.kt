package com.materialkolor.builder.feature.poster

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

private const val ANIMATE = "Animate color changes"

private const val LABEL = "Target options"

@OptIn(ExperimentalTestApi::class)
class ColorAnimationOptionTest {
    @Test
    fun animate_expressive_writesOnlyThatTargetsPrefsWithNoUndoEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(expressive = true))
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText(ANIMATE).assertIsOff()
            onNodeWithText("300 ms").assertDoesNotExist()
            onNodeWithText(ANIMATE).performClick()
            waitForIdle()
            onNodeWithText("500 ms").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.SetColorAnimation(ExportTarget.Material3Expressive, on = true),
                WorkspaceAction.SetColorAnimationDuration(ExportTarget.Material3Expressive, durationMs = 500),
            )
            harness.preferences.exportPrefs shouldBe mapOf(
                ExportTarget.Material3Expressive to ExportPrefs(animate = true, animationDurationMs = 500),
            )
            onNodeWithText(ANIMATE).assertIsOn()
            onNodeWithText("500 ms").assertIsSelected()
            harness.undoEntries() shouldBe 0
        }

    @Test
    fun duration_storedFromElsewhere_joinsTheChoice() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            harness.preferences = Preferences().withExportPrefs(
                ExportTarget.Material3,
                ExportPrefs(animate = true, animationDurationMs = 750),
            )
            showSection(harness) { context, dispatcher -> ColorAnimationOption(context, dispatcher) }

            onNodeWithText("750 ms").assertIsSelected()
            onNodeWithText("1000 ms").assertExists()
        }

    @Test
    fun animation_custom_isHidden() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Custom))
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText(LABEL).assertExists()
            onNodeWithText(ANIMATE).assertDoesNotExist()
        }

    @Test
    fun animation_frozenExport_isHidden() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            harness.preferences =
                Preferences().withExportPrefs(ExportTarget.Material3, ExportPrefs(mode = ExportMode.Frozen))
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText(LABEL).assertExists()
            onNodeWithText(ANIMATE).assertDoesNotExist()
        }

    @Test
    fun targetOptions_fluentFrozen_leavesNoLabelOverNothing() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Fluent))
            harness.preferences =
                Preferences().withExportPrefs(ExportTarget.Fluent, ExportPrefs(mode = ExportMode.Frozen))
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText(LABEL).assertDoesNotExist()
            onNodeWithText(ANIMATE).assertDoesNotExist()
        }

    @Test
    fun animation_readsTheCurrentTargetsPrefsOnly() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Unstyled))
            harness.preferences = Preferences().withExportPrefs(ExportTarget.Material3, ExportPrefs(animate = true))
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText(ANIMATE).assertIsOff()
            onNodeWithText("300 ms").assertDoesNotExist()
        }
}
