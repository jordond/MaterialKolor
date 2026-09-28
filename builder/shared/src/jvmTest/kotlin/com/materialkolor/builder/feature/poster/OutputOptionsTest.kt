package com.materialkolor.builder.feature.poster

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.model.TonalRamp
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

private const val CLASSIC_ONLY = "This style only exists in the 2021 spec."

private const val COMES_BACK = "Your 2025 spec comes back as soon as you pick a style that has it."

private const val CMF_ONLY = "This style only exists in the 2026 spec."

private const val RUNS_AS_2025 = "Runs as 2025 on this style."

private const val RUNS_AS_2021 = "Runs as 2021 on this style."

private const val AMOLED = "AMOLED dark mode"

private const val NO_AMOLED = "The Unstyled adapter has no AMOLED switch yet."

private const val LIGHT_TONE = "primaryPressed light tone"

@OptIn(ExperimentalTestApi::class)
class OutputOptionsTest {
    @Test
    fun spec_revisedStyle_startsOn2026RunningAs2025AndSetsTheSpecAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> SpecPlatformControl(context, dispatcher) }

            onNodeWithText("2026").assertIsSelected()
            onNodeWithText("2026").assertIsEnabled()
            onNodeWithText("2021").assertIsEnabled()
            onNodeWithText("2025").assertIsEnabled()
            onNodeWithText(RUNS_AS_2025).assertExists()
            onNodeWithText(CLASSIC_ONLY).assertDoesNotExist()

            onNodeWithText("2021").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.SetSpec(SpecVersion.Spec2021), EditPhase.Discrete),
            )
            onNodeWithText("2021").assertIsSelected()
            onNodeWithText(RUNS_AS_2025).assertDoesNotExist()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun spec_classicStyleAsking2025_showsItOffOn2021ThenBringsItBackWithTonalSpot() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(style = Style.Rainbow, spec = SpecVersion.Spec2025))
            showSection(harness) { context, dispatcher -> SpecPlatformControl(context, dispatcher) }

            onNodeWithText("2021").assertIsSelected()
            onNodeWithText("2025").assertIsNotEnabled()
            onNodeWithText("2026").assertIsEnabled()
            onNodeWithText(CLASSIC_ONLY).assertExists()
            onNodeWithText(COMES_BACK).assertExists()

            harness.dispatch(WorkspaceAction.Edit(DocumentChange.SetStyle(Style.TonalSpot), EditPhase.Discrete))
            waitForIdle()

            harness.document.spec shouldBe SpecVersion.Spec2025
            onNodeWithText("2025").assertIsSelected()
            onNodeWithText("2021").assertIsNotSelected()
            onNodeWithText(CLASSIC_ONLY).assertDoesNotExist()
            onNodeWithText(COMES_BACK).assertDoesNotExist()
        }

    @Test
    fun spec_classicStyleOnTheDefault_keeps2026AndSaysItRunsAs2021() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(style = Style.Fidelity))) { context, dispatcher ->
                SpecPlatformControl(context, dispatcher)
            }

            onNodeWithText("2026").assertIsSelected()
            onNodeWithText("2025").assertIsNotEnabled()
            onNodeWithText(RUNS_AS_2021).assertExists()
            onNodeWithText(COMES_BACK).assertDoesNotExist()
        }

    @Test
    fun spec_cmf_shows2026ChosenWithTheOtherTwoOff() =
        runComposeUiTest {
            showSection(
                PosterHarness(Plain.copy(style = Style.Cmf, spec = SpecVersion.Spec2021)),
            ) { context, dispatcher ->
                SpecPlatformControl(context, dispatcher)
            }

            onNodeWithText("2026").assertIsSelected()
            onNodeWithText("2021").assertIsNotEnabled()
            onNodeWithText("2025").assertIsNotEnabled()
            onNodeWithText(CMF_ONLY).assertExists()
        }

    @Test
    fun platform_showsOnlyAt2025AndSetsThePlatform() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(spec = SpecVersion.Spec2021))
            showSection(harness) { context, dispatcher -> SpecPlatformControl(context, dispatcher) }

            onNodeWithText("Phone").assertDoesNotExist()

            harness.dispatch(WorkspaceAction.Edit(DocumentChange.SetSpec(SpecVersion.Spec2025), EditPhase.Discrete))
            waitForIdle()

            onNodeWithText("Phone").assertIsSelected()
            onNodeWithText("Watch").performClick()
            waitForIdle()

            harness.actions.last() shouldBe
                WorkspaceAction.Edit(DocumentChange.SetPlatform(SchemePlatform.Watch), EditPhase.Discrete)
            harness.document.platform shouldBe SchemePlatform.Watch
        }

    @Test
    fun amoled_material3_switchesPureBlackOnAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText(AMOLED).assertIsOff()
            onNodeWithText(AMOLED).performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.Edit(DocumentChange.SetAmoled(true), EditPhase.Discrete))
            onNodeWithText(AMOLED).assertIsOn()
            harness.undoEntries() shouldBe 1
            onNodeWithText("Motion").assertDoesNotExist()
        }

    @Test
    fun amoled_unstyled_isOffWithR6() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(library = Library.Unstyled))) { context, dispatcher ->
                TargetOptions(context, dispatcher)
            }

            onNodeWithText(AMOLED).assertIsNotEnabled()
            onNodeWithText(NO_AMOLED).assertExists()
        }

    @Test
    fun targetOptions_fluent_leavesAmoledMotionAndTonesOut() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Fluent))
            showSection(harness) { context, dispatcher -> FineTuneContent(context, dispatcher) }

            onNodeWithText(AMOLED).assertDoesNotExist()
            onNodeWithText("Motion").assertDoesNotExist()
            onNodeWithText("Custom tones").assertDoesNotExist()
            // Color animation is the only option Fluent has, so the label stands over it.
            onNodeWithText("Target options").assertExists()
            onNodeWithText("Animate color changes").assertExists()
        }

    @Test
    fun motion_expressive_pickingStandardSetsItAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(expressive = true))
            showSection(harness) { context, dispatcher -> TargetOptions(context, dispatcher) }

            onNodeWithText("Expressive").assertIsSelected()
            onNodeWithText("Standard").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.SetMotionScheme(MotionSchemeChoice.Standard), EditPhase.Discrete),
            )
            harness.document.motionScheme shouldBe MotionSchemeChoice.Standard
            onNodeWithText("Standard").assertIsSelected()
        }

    @Test
    fun toneTable_custom_listsOnlyTheSlotsCutByTone() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Custom))
            showSection(harness) { context, dispatcher -> FineTuneContent(context, dispatcher) }

            ToneSlots.size shouldBe 11
            onNodeWithText("Custom tones").assertExists()
            onNodeWithText("primaryPressed").assertExists()
            onNodeWithText("shadow").assertExists()
            onNodeWithText("primary").assertDoesNotExist()
            onNodeWithText("Light 32").assertExists()
            onNodeWithText("Dark 88").assertExists()
            onNodeWithText(AMOLED).assertIsEnabled()
            onNodeWithText("Motion").assertDoesNotExist()
        }

    @Test
    fun toneTable_otherTargets_composeNothing() =
        runComposeUiTest {
            showSection(PosterHarness(Plain.copy(expressive = true))) { context, dispatcher ->
                CustomToneTable(context, dispatcher)
            }

            onNodeWithText("Custom tones").assertDoesNotExist()
            onNodeWithText("primaryPressed").assertDoesNotExist()
        }

    @Test
    fun toneTable_drag_movesOneModeAndLetsGoAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Custom))
            showSection(harness) { context, dispatcher -> CustomToneTable(context, dispatcher) }

            onNodeWithText("Reset primaryPressed").assertDoesNotExist()
            onNodeWithContentDescription(LIGHT_TONE)
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(61f) }
            waitForIdle()

            val moved = CustomTone(light = 61)
            val change = DocumentChange.SetCustomTone(CustomSlot.PrimaryPressed, moved)
            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(change, EditPhase.Dragging),
                WorkspaceAction.Edit(change, EditPhase.Released),
            )
            harness.document.customTones shouldBe mapOf(CustomSlot.PrimaryPressed to moved)
            onNodeWithText("Light 61").assertExists()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun toneTable_darkDrag_keepsTheLightToneAsStored() =
        runComposeUiTest {
            val tones = mapOf(CustomSlot.BorderSoft to CustomTone(light = 80))
            val start = Plain.copy(library = Library.Custom, customTones = tones)
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> CustomToneTable(context, dispatcher) }

            onNodeWithContentDescription("borderSoft dark tone")
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(20f) }
            waitForIdle()

            harness.document.customTones shouldBe mapOf(CustomSlot.BorderSoft to CustomTone(light = 80, dark = 20))
        }

    @Test
    fun toneTable_reset_putsTheSlotBackAsOneEntry() =
        runComposeUiTest {
            val tones = mapOf(CustomSlot.TextMuted to CustomTone(dark = 50))
            val start = Plain.copy(library = Library.Custom, customTones = tones)
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> CustomToneTable(context, dispatcher) }

            onNodeWithText("Dark 50").assertExists()
            onNodeWithText("Reset textMuted").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.SetCustomTone(CustomSlot.TextMuted, null), EditPhase.Discrete),
            )
            harness.document.customTones shouldBe emptyMap()
            onNodeWithText("Reset textMuted").assertDoesNotExist()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun toneTable_onRampSlot_startsAtTheOnColorsOwnToneNotItsBackground() {
        val onRamp = SlotResolution.OnRamp(TonalRamp.Primary, light = 40, dark = 80)
        val fromRamp = SlotResolution.FromRamp(TonalRamp.Primary, light = 40, dark = 80)

        onRamp.ownTone(isDark = false, color = Argb(0xFFFFFF)) shouldBe 100
        onRamp.ownTone(isDark = true, color = Argb(0x000000)) shouldBe 0
        fromRamp.ownTone(isDark = true, color = Argb(0x000000)) shouldBe 80
    }
}
