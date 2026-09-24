package com.materialkolor.builder.feature.poster

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import com.materialkolor.builder.engine.audit.rate
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.canvas.RampTarget
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

private val Brand = Accent(name = "brand", seed = Argb(0x00897B))

private val Status = Accent(name = "status", seed = Argb(0xE53935))

private const val ADD = "Add extra color"

private const val FIRST_NAME = "Extra color 1 name"

@OptIn(ExperimentalTestApi::class)
class AccentsEditorTest {
    @Test
    fun empty_saysWhatExtraColorsAreFor() =
        runComposeUiTest {
            showSection(PosterHarness(Plain)) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText("Extra colors").assertExists()
            onNodeWithText("brand, a status or a category", substring = true).assertExists()
            onNodeWithText(ADD).assertIsEnabled()
        }

    @Test
    fun add_appendsTheFirstFreeNameAsOneEntry() =
        runComposeUiTest {
            val start = Plain.copy(accents = listOf(Accent(name = "accent1", seed = Argb(0x00897B))))
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText(ADD).performClick()
            waitForIdle()

            val added = newAccent(start)
            added.name shouldBe "accent2"
            harness.actions shouldBe listOf(WorkspaceAction.Edit(DocumentChange.AddAccent(added), EditPhase.Discrete))
            harness.document.accents.map { accent -> accent.name } shouldBe listOf("accent1", "accent2")
            onNodeWithText("Extra color 2 name").assertExists()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun add_byKeyboard_handsFocusToTheNewName() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            // The eighth turns Add off under the focus, which has to land on the new name all the same.
            repeat(MAX_ACCENTS) { index ->
                pressByKeyboard(onNodeWithText(ADD))
                onNode(hasText("Extra color ${index + 1} name") and hasSetTextAction()).assertIsFocused()
            }

            harness.document.accents.size shouldBe MAX_ACCENTS
            onNodeWithText(ADD).assertIsNotEnabled()
        }

    @Test
    fun remove_dropsThatColorAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand, Status)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText("Remove brand").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.Edit(DocumentChange.RemoveAccent(0), EditPhase.Discrete))
            harness.document.accents shouldBe listOf(Status)
            onNodeWithText("Remove status").assertExists()
            onNodeWithText("Remove brand").assertDoesNotExist()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun add_atTheCap_staysOffWithALine() =
        runComposeUiTest {
            val accents = List(MAX_ACCENTS) { index -> Accent(name = "color$index", seed = Argb(0x00897B)) }
            val harness = PosterHarness(Plain.copy(accents = accents))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText(ADD).assertIsNotEnabled()
            onNodeWithText("A theme holds up to 8 extra colors.").assertExists()
        }

    @Test
    fun name_badDraft_saysWhyAndNeverCommits() =
        runComposeUiTest {
            val start = Plain.copy(accents = listOf(Brand, Status))
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }
            val field = onNode(hasText(FIRST_NAME) and hasSetTextAction())

            field.performTextReplacement("status")
            waitForIdle()
            onNodeWithText("Another extra color already has that name.", substring = true).assertExists()
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            field.performTextReplacement("primary")
            waitForIdle()
            onNodeWithText("The scheme already has a role by that name.", substring = true).assertExists()
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            harness.actions shouldBe emptyList()
            harness.document shouldBe start
        }

    @Test
    fun name_goodDraft_commitsAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }
            val field = onNode(hasText(FIRST_NAME) and hasSetTextAction())

            field.performTextReplacement("sale")
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.UpdateAccent(0, Brand.copy(name = "sale")), EditPhase.Discrete),
            )
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun tone_drag_movesTheRatioAndLetsGoAsOneEntry() =
        runComposeUiTest {
            // At AAA no on color reaches 7 to 1 over tone 40, so the ratio moves as the tone does.
            val strict = Brand.copy(threshold = OnColorThreshold.Aaa)
            val start = Plain.copy(accents = listOf(strict))
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }
            onNodeWithText(ratios(start, isDark = false)).assertExists()

            onNodeWithContentDescription("brand light color tone")
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(20f) }
            waitForIdle()

            val moved = strict.copy(light = FamilyTones(color = 20, container = 90))
            val change = DocumentChange.UpdateAccent(0, moved)
            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(change, EditPhase.Dragging),
                WorkspaceAction.Edit(change, EditPhase.Released),
            )
            ratios(harness.document, isDark = false) shouldNotBe ratios(start, isDark = false)
            onNodeWithText(ratios(harness.document, isDark = false)).assertExists()
            onNodeWithText(ratios(start, isDark = true)).assertExists()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun threshold_aaa_movesTheRatioAsOneEntry() =
        runComposeUiTest {
            val start = Plain.copy(accents = listOf(Brand.copy(threshold = OnColorThreshold.AaLarge)))
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText("AAA").performClick()
            waitForIdle()

            harness.document.accents
                .single()
                .threshold shouldBe OnColorThreshold.Aaa
            onNodeWithText("AAA").assertIsSelected()
            ratios(harness.document, isDark = true) shouldNotBe ratios(start, isDark = true)
            onNodeWithText(ratios(harness.document, isDark = true)).assertExists()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun harmonize_flipsThatColorAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand, Status)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText("Harmonize brand with seed").performClick()
            waitForIdle()

            val flipped = Brand.copy(harmonize = !Brand.harmonize)
            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.UpdateAccent(0, flipped), EditPhase.Discrete),
            )
            harness.document.accents shouldBe listOf(flipped, Status)
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun rowControls_readOutWithTheColorsName() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand, Status)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText("Harmonize brand with seed").assertExists()
            onNodeWithText("Harmonize status with seed").assertExists()
            onNodeWithContentDescription("brand on colors clear").assertExists()
            onNodeWithContentDescription("status on colors clear").assertExists()
            onAllNodesWithText("On colors clear").assertCountEquals(2)
        }

    @Test
    fun pickAndShowOnRamp_askTheWorkspace() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithContentDescription("Pick brand seed").performClick()
            onNodeWithText("Show brand on ramp").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.OpenPicker(PickerTarget.Accent(0)),
                WorkspaceAction.ShowOnRamp(RampTarget.OfAccent(AccentSlot(0, AccentPart.Color), isDark = false)),
            )
        }

    @Test
    fun fluent_keepsTheListReadOnlyWithR8() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(library = Library.Fluent, accents = listOf(Brand)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            onNodeWithText("Fluent has no place for extra colors yet.").assertExists()
            onNodeWithText(FIRST_NAME).assertIsNotEnabled()
            onNodeWithText("Remove brand").assertIsNotEnabled()
            onNodeWithText(ADD).assertIsNotEnabled()
        }

    @Test
    fun remove_byKeyboard_handsFocusToTheNextName() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand, Status)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            pressByKeyboard(onNodeWithText("Remove brand"))

            harness.document.accents shouldBe listOf(Status)
            onNode(hasText(FIRST_NAME) and hasSetTextAction()).assertIsFocused()
            onNode(hasText(FIRST_NAME) and hasText("status")).assertExists()
        }

    @Test
    fun remove_lastByKeyboard_handsFocusToTheNameBefore() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand, Status)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            pressByKeyboard(onNodeWithText("Remove status"))

            harness.document.accents shouldBe listOf(Brand)
            onNode(hasText(FIRST_NAME) and hasSetTextAction()).assertIsFocused()
        }

    @Test
    fun remove_onlyByKeyboard_handsFocusToAdd() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(accents = listOf(Brand)))
            showSection(harness) { context, dispatcher -> AccentsEditor(context, dispatcher) }

            pressByKeyboard(onNodeWithText("Remove brand"))

            harness.document.accents shouldBe emptyList()
            onNodeWithText(ADD).assertIsFocused()
        }
}

/** The line over one mode's four colors of the only extra color of [document], as the editor words it. */
private fun ratios(
    document: ThemeDocument,
    isDark: Boolean,
): String {
    val result = ThemeResolver().resolve(document)

    fun ratio(
        foreground: AccentPart,
        background: AccentPart,
    ): String {
        val pair = ContrastPair(
            foreground = ColorRef.OfAccent(AccentSlot(0, foreground)),
            background = ColorRef.OfAccent(AccentSlot(0, background)),
            kind = PairKind.Text,
        )
        return ratioText(result.rate(pair, isDark).ratio)
    }

    val mode = if (isDark) "Dark" else "Light"
    val onColor = ratio(AccentPart.OnColor, AccentPart.Color)
    val onContainer = ratio(AccentPart.OnContainer, AccentPart.Container)
    return "$mode, on color $onColor to 1, on container $onContainer to 1"
}

/** Focuses [node] and presses Enter on it, the way a keyboard user does. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pressByKeyboard(node: SemanticsNodeInteraction) {
    node.requestFocus()
    node.performKeyInput { pressKey(Key.Enter) }
    waitForIdle()
}
