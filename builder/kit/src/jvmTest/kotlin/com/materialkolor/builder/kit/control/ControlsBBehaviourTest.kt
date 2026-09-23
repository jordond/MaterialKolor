package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.kit.headless.SliderRules
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val Slider = "slider"
private const val Field = "field"

/** The contrast control's stops and snap distance (F-12). */
private val ContrastStops = listOf(-1f, 0f, 0.5f, 1f)
private const val ContrastSnap = 0.04f
private const val Tolerance = 1e-4f

@OptIn(ExperimentalTestApi::class)
class ControlsBBehaviourTest {
    @Test
    fun sliderRules_dragNearAStop_snapsOnlyWithinTheDistance() {
        val rules = SliderRules(-1f..1f, step = 0.01f, stops = ContrastStops, snapDistance = ContrastSnap)

        rules.snap(0.47f) shouldBe 0.5f
        rules.snap(0.53f) shouldBe 0.5f
        rules.snap(0.45f) shouldBe 0.45f
        rules.snap(-0.97f) shouldBe -1f
        rules.snap(0.2f) shouldBe 0.2f
    }

    @Test
    fun sliderRules_move_landsOnTheStepGridAndStaysInRange() {
        val rules = SliderRules(-1f..1f, step = 0.01f, stops = ContrastStops, snapDistance = ContrastSnap)

        rules.move(0f, 1) shouldBe (0.01f plusOrMinus Tolerance)
        rules.move(0.337f, 1) shouldBe (0.35f plusOrMinus Tolerance)
        rules.move(0.5f, -10) shouldBe (0.4f plusOrMinus Tolerance)
        rules.move(0.98f, 10) shouldBe 1f
        rules.move(-0.98f, -10) shouldBe -1f
    }

    @Test
    fun sliderRules_badArguments_areTurnedAway() {
        assertFailsWith<IllegalArgumentException> {
            SliderRules(
                0f..1f,
                step = 0f,
                stops = emptyList(),
                snapDistance = 0f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SliderRules(
                0f..1f,
                step = 0.1f,
                stops = listOf(2f),
                snapDistance = 0f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SliderRules(
                1f..1f,
                step = 0.1f,
                stops = emptyList(),
                snapDistance = 0f,
            )
        }
    }

    @Test
    fun fieldDraft_valueArrivingMidEdit_leavesTheDraftAlone() {
        val draft = FieldDraft("#6750A4")
        draft.focused = true
        draft.value = TextFieldValue("red")

        draft.sync("#FF0000")
        draft.text shouldBe "red"
        draft.dirty shouldBe true

        draft.settle("#FF0000")
        draft.sync("#00FF00")
        draft.text shouldBe "#00FF00"
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
    fun slider_arrowKeys_moveOneStepAndTenWithShift() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(0f)
            var finished = 0
            setSkinnedContent(variant) {
                ContrastSlider(value, { value = it }, { finished++ })
            }

            onNodeWithTag(Slider).requestFocus()
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.DirectionRight) }
            value shouldBe (0.01f plusOrMinus Tolerance)
            onNodeWithTag(Slider).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) } }
            value shouldBe (0.11f plusOrMinus Tolerance)
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.DirectionLeft) }
            value shouldBe (0.1f plusOrMinus Tolerance)
            onNodeWithTag(Slider).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionDown) } }
            value shouldBe (0f plusOrMinus Tolerance)
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.MoveEnd) }
            value shouldBe 1f
            finished shouldBe 5
        }

    @Test
    fun slider_arrowNextToAStop_stepsPastItWithoutSnapping() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(0.5f)
            setSkinnedContent(variant) {
                ContrastSlider(value, { value = it }, {})
            }

            onNodeWithTag(Slider).requestFocus()
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.DirectionRight) }
            value shouldBe (0.51f plusOrMinus Tolerance)
        }

    @Test
    fun slider_tapNearAStop_snapsOntoItAndFinishesOnce() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(-1f)
            var finished = 0
            setSkinnedContent(variant) {
                ContrastSlider(value, { value = it }, { finished++ })
            }

            // Three quarters of the way along is 0.5 give or take the thumb's inset, well inside 0.04.
            onNodeWithTag(Slider).performTouchInput { click(Offset(width * 0.75f, centerY)) }
            waitForIdle()
            value shouldBe 0.5f
            finished shouldBe 1
        }

    @Test
    fun slider_drag_reportsEveryMoveAndFinishesOnce() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(-1f)
            val reports = mutableListOf<Float>()
            var finished = 0
            setSkinnedContent(variant) {
                ContrastSlider(
                    value = value,
                    onValueChange = { next ->
                        reports += next
                        value = next
                    },
                    onValueChangeFinished = { finished++ },
                )
            }

            onNodeWithTag(Slider).performTouchInput {
                down(Offset(width * 0.1f, centerY))
                for (step in 2..8) moveTo(Offset(width * step / 10f, centerY))
                up()
            }
            waitForIdle()
            reports.size shouldBeGreaterThan 3
            finished shouldBe 1
        }

    @Test
    fun tabs_arrowKeys_roveFocusAndSelectionAndWrap() =
        forEverySkin { variant ->
            var selected by mutableStateOf("App")
            setSkinnedContent(variant) {
                BuilderTabs(listOf("App", "Components", "Roles"), selected, { selected = it }, { it })
            }

            tab("App").requestFocus()
            tab("App").performKeyInput { pressKey(Key.DirectionRight) }
            selected shouldBe "Components"
            tab("Components").assertIsFocused().assertIsSelected()
            tab("Components").performKeyInput { pressKey(Key.DirectionLeft) }
            tab("App").performKeyInput { pressKey(Key.DirectionLeft) }
            selected shouldBe "Roles"
            tab("Roles").assertIsFocused().assertIsSelected()
        }

    @Test
    fun hexField_enter_commitsOnceAndTidiesTheText() =
        forEverySkin { variant ->
            val commits = mutableListOf<Pair<Argb, Set<ParseNote>>>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(variant) {
                BuilderHexField(
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
    fun hexField_leavingTheField_commits() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            lateinit var focus: FocusManager
            setSkinnedContent(variant) {
                focus = LocalFocusManager.current
                BuilderHexField(Argb(0x6750A4), { argb, _ -> commits += argb }, "Seed", Modifier.testTag(Field))
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("#00ff00")
            runOnIdle { focus.clearFocus() }
            waitForIdle()
            commits.last() shouldBe Argb(0x00FF00)
            commits.distinct() shouldBe listOf(Argb(0x00FF00))
        }

    @Test
    fun hexField_pause_commitsFourHundredMillisAfterTheLastValidKeystroke() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            setSkinnedContent(variant) {
                BuilderHexField(Argb(0x6750A4), { argb, _ -> commits += argb }, "Seed", Modifier.testTag(Field))
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
        }

    @Test
    fun hexField_invalidText_showsTheErrorAndNeverCommits() =
        forEverySkin { variant ->
            val commits = mutableListOf<Argb>()
            lateinit var focus: FocusManager
            setSkinnedContent(variant) {
                focus = LocalFocusManager.current
                BuilderHexField(Argb(0x6750A4), { argb, _ -> commits += argb }, "Seed", Modifier.testTag(Field))
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
                BuilderHexField(Argb(0x6750A4), { argb, _ -> commits += argb }, "Seed", Modifier.testTag(Field))
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
    fun hexField_alphaText_commitsTheNoteAndShowsIt() =
        runComposeUiTest {
            val commits = mutableListOf<Pair<Argb, Set<ParseNote>>>()
            var seed by mutableStateOf(Argb(0x6750A4))
            setSkinnedContent(SkinVariant.Custom) {
                Column {
                    BuilderHexField(
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
            commits.last() shouldBe (Argb(0xFF0000) to setOf(ParseNote.AlphaDropped))
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
}

@Composable
private fun ContrastSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    BuilderSlider(
        value = value,
        onValueChange = onValueChange,
        label = "Contrast",
        modifier = Modifier.testTag(Slider).width(400.dp),
        onValueChangeFinished = onValueChangeFinished,
        valueRange = -1f..1f,
        step = 0.01f,
        stops = ContrastStops,
        snapDistance = ContrastSnap,
    )
}
