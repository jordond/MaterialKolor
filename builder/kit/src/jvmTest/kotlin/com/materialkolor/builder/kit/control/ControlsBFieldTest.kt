package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.kit.headless.FieldDraft
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val Field = "field"

/**
 * Hands a value in from outside while the clock is held, the way a shuffle would. One frame
 * recomposes the field with it and a second shows the text its draft took on.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.handInFromOutside(write: () -> Unit) {
    write()
    Snapshot.sendApplyNotifications()
    mainClock.advanceTimeByFrame()
    mainClock.advanceTimeByFrame()
}

/** The key that holds undo on this machine, Cmd on a Mac and Ctrl elsewhere. */
private val UndoModifier: Key =
    if (System.getProperty("os.name").orEmpty().startsWith("Mac")) Key.MetaLeft else Key.CtrlLeft

@OptIn(ExperimentalTestApi::class)
class ControlsBFieldTest {
    @Test
    fun fieldDraft_ownCommitComingBack_leavesTheTextAsTyped() {
        val draft = FieldDraft("#6750A4")
        draft.focused = true
        draft.value = TextFieldValue("red")

        draft.commit("red", canonical = "#FF0000")
        draft.sync("#FF0000")
        draft.text shouldBe "red"
        draft.dirty shouldBe false
    }

    @Test
    fun fieldDraft_outsideValue_replacesTheDraftEvenMidEdit() {
        val draft = FieldDraft("#6750A4")
        draft.focused = true
        draft.value = TextFieldValue("#00f0")

        draft.sync("#00FF00")
        draft.text shouldBe "#00FF00"
        draft.dirty shouldBe false

        draft.value = TextFieldValue("red")
        draft.commit("red", canonical = "#FF0000")
        draft.sync("#0000FF")
        draft.text shouldBe "#0000FF"
        draft.dirty shouldBe false
    }

    @Test
    fun fieldDraft_revertAndComposition_followTheCommittedText() {
        val draft = FieldDraft("Ocean")
        draft.revert() shouldBe false

        draft.value = TextFieldValue("Oce\u3042", composition = TextRange(3, 4))
        draft.composing shouldBe true
        draft.revert() shouldBe true
        draft.text shouldBe "Ocean"
        draft.composing shouldBe false
    }

    @Test
    fun hexField_enter_commitsOnceAndTidiesTheText() =
        forEverySkin { variant ->
            val commits = mutableListOf<Pair<Argb, Set<ParseNote>>>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(variant) {
                InputHexField(
                    value = seed,
                    onCommit = { argb, notes ->
                        commits += argb to notes
                        seed = argb
                    },
                    label = "Seed",
                    modifier = Modifier.testTag(Field),
                )
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("red")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            commits shouldBe listOf(Argb(0xFF0000) to emptySet())
            editableText(Field) shouldBe "#FF0000"
        }

    @Test
    fun hexField_leavingTheField_commitsOnce() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            var seed by mutableStateOf(Argb(0x6750A4))
            lateinit var focus: FocusManager
            setSkinnedContent(variant) {
                focus = LocalFocusManager.current
                InputHexField(seed, { argb, _ -> commits += argb.also { seed = it } }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("#00ff00")
            runOnIdle { focus.clearFocus() }
            waitForIdle()
            commits shouldBe listOf(Argb(0x00FF00))
            editableText(Field) shouldBe "#00FF00"
        }

    @Test
    fun hexField_pause_commitsFourHundredMillisAfterTheLastValidKeystroke() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(variant) {
                InputHexField(seed, { argb, _ -> commits += argb.also { seed = it } }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            mainClock.autoAdvance = false
            onNodeWithTag(Field).performTextReplacement("#ff0000")
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(CommitDelayMillis - 100)
            commits.shouldBeEmpty()
            onNodeWithTag(Field).performTextReplacement("#00ff00")
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(CommitDelayMillis - 100)
            commits.shouldBeEmpty()
            mainClock.advanceTimeBy(150)
            commits shouldBe listOf(Argb(0x00FF00))
            editableText(Field) shouldBe "#00ff00"
        }

    @Test
    fun hexField_outsideValueMidEdit_replacesTheDraftAndNothingCommitsOverIt() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            var seed by mutableStateOf(Argb(0x6750A4))
            lateinit var focus: FocusManager
            setSkinnedContent(variant) {
                focus = LocalFocusManager.current
                InputHexField(seed, { argb, _ -> commits += argb.also { seed = it } }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("#00f0")
            seed = Argb(0x00FF00)
            waitForIdle()
            editableText(Field) shouldBe "#00FF00"
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            runOnIdle { focus.clearFocus() }
            waitForIdle()
            commits.shouldBeEmpty()
            seed shouldBe Argb(0x00FF00)
        }

    @Test
    fun hexField_outsideValueAfterThePauseCommit_showsAndNothingCommitsOverIt() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(variant) {
                InputHexField(seed, { argb, _ -> commits += argb.also { seed = it } }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            mainClock.autoAdvance = false
            onNodeWithTag(Field).performTextReplacement("red")
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(CommitDelayMillis + 100)
            commits shouldBe listOf(Argb(0xFF0000))
            editableText(Field) shouldBe "red"

            handInFromOutside { seed = Argb(0x00FF00) }
            editableText(Field) shouldBe "#00FF00"
            mainClock.advanceTimeBy(CommitDelayMillis * 2)
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            commits shouldBe listOf(Argb(0xFF0000))
            seed shouldBe Argb(0x00FF00)
        }

    @Test
    fun hexField_outsideValueInsideThePause_cancelsThePendingCommit() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(variant) {
                InputHexField(seed, { argb, _ -> commits += argb.also { seed = it } }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            mainClock.autoAdvance = false
            onNodeWithTag(Field).performTextReplacement("red")
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(CommitDelayMillis / 2)
            handInFromOutside { seed = Argb(0x00FF00) }
            editableText(Field) shouldBe "#00FF00"
            mainClock.advanceTimeBy(CommitDelayMillis * 2)
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            commits.shouldBeEmpty()
            seed shouldBe Argb(0x00FF00)
        }

    @Test
    fun hexField_invalidText_showsTheErrorAndNeverCommits() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            lateinit var focus: FocusManager
            setSkinnedContent(variant) {
                focus = LocalFocusManager.current
                InputHexField(Argb(0x6750A4), { argb, _ -> commits += argb }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("#12345")
            mainClock.advanceTimeBy(CommitDelayMillis * 3)
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            runOnIdle { focus.clearFocus() }
            waitForIdle()
            commits.shouldBeEmpty()
            onNodeWithText("Hex takes 3, 6 or 8 digits", useUnmergedTree = true).assertExists()
            editableText(Field) shouldBe "#12345"
        }

    @Test
    fun hexField_escape_putsBackTheCommittedColor() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            setSkinnedContent(variant) {
                InputHexField(Argb(0x6750A4), { argb, _ -> commits += argb }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            mainClock.autoAdvance = false
            onNodeWithTag(Field).performTextReplacement("nope")
            mainClock.advanceTimeByFrame()
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            editableText(Field) shouldBe "#6750A4"
            mainClock.advanceTimeBy(CommitDelayMillis * 2)
            commits.shouldBeEmpty()
        }

    @Test
    fun hexField_alphaText_commitsTheNoteAndShowsTheCallersWords() =
        runComposeUiTest {
            val commits = mutableListOf<Pair<Argb, Set<ParseNote>>>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(SkinVariant.Custom) {
                Column {
                    InputHexField(
                        value = seed,
                        onCommit = { argb, notes ->
                            commits += argb to notes
                            seed = argb
                        },
                        label = "Seed",
                        modifier = Modifier.testTag(Field),
                        large = true,
                    )
                }
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("#80FF0000")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            commits shouldBe listOf(Argb(0xFF0000) to setOf(ParseNote.AlphaDropped))
            editableText(Field) shouldBe "#FF0000"
            onNodeWithText("Alpha dropped, the seed is always opaque", useUnmergedTree = true).assertExists()
        }

    @Test
    fun textField_enterCommitsEscapeRevertsAndErrorsHold() =
        forEverySkin { variant ->
            val commits = mutableListOf<String>()
            var name by mutableStateOf("Ocean")
            setSkinnedContent(variant) {
                BuilderTextField(
                    value = name,
                    onCommit = { text ->
                        commits += text
                        name = text.trim()
                    },
                    label = "Project name",
                    modifier = Modifier.testTag(Field),
                    error = { text -> if (text.isBlank()) "A project needs a name" else null },
                )
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("Forest ")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            commits shouldBe listOf("Forest ")
            editableText(Field) shouldBe "Forest"

            onNodeWithTag(Field).performTextReplacement("Dune")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Escape) }
            editableText(Field) shouldBe "Forest"

            onNodeWithTag(Field).performTextReplacement("  ")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            commits shouldBe listOf("Forest ")
            onNodeWithText("A project needs a name", useUnmergedTree = true).assertExists()
        }

    @Test
    fun textField_undoKey_undoesInTheFieldAndNeverBubblesOut() =
        forEverySkin { variant ->
            val bubbled = mutableListOf<Key>()
            setSkinnedContent(variant) {
                Box(
                    Modifier.onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) bubbled += event.key
                        false
                    },
                ) {
                    BuilderTextField("Ocean", {}, "Project name", Modifier.testTag(Field))
                }
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("Forest")
            onNodeWithTag(Field).performKeyInput { withKeyDown(UndoModifier) { pressKey(Key.Z) } }
            editableText(Field) shouldBe "Ocean"
            bubbled shouldNotContain Key.Z
        }

    @Test
    fun textField_draftChange_reportsEditsAndTheRevertOnly() =
        forEverySkin { variant ->
            val drafts = mutableListOf<String>()
            val commits = mutableListOf<String>()
            var name by mutableStateOf("Ocean")
            setSkinnedContent(variant) {
                BuilderTextField(
                    value = name,
                    onCommit = { text -> commits += text },
                    label = "Project name",
                    modifier = Modifier.testTag(Field),
                    onDraftChange = { text -> drafts += text },
                )
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("Forest")
            drafts shouldBe listOf("Forest")
            commits.shouldBeEmpty()

            onNodeWithTag(Field).performKeyInput { pressKey(Key.Escape) }
            drafts shouldBe listOf("Forest", "Ocean")

            onNodeWithTag(Field).performTextInputSelection(TextRange(2))
            waitForIdle()
            drafts shouldBe listOf("Forest", "Ocean")

            name = "Dune"
            waitForIdle()
            editableText(Field) shouldBe "Dune"
            drafts shouldBe listOf("Forest", "Ocean")

            onNodeWithTag(Field).performTextReplacement("Pine")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            commits shouldBe listOf("Pine")
            drafts shouldBe listOf("Forest", "Ocean", "Pine")
        }
}
