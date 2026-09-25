package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorInput
import com.materialkolor.builder.domain.color.ParseResult
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.ControlSkins
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.hct.Hct
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The color every picker test starts from.
 */
internal val PickerSeed: Argb = Argb(0x6750A4)

/**
 * Wide enough for the tracks and their value fields side by side.
 */
internal val PickerWidth: Dp = 440.dp

/**
 * Every skin the picker is drawn in, named for the screenshots.
 */
internal val PickerSkins: List<Pair<String, Skin>> = ControlSkins

/**
 * Runs [block] once per skin in a fresh test, with the skin's name as the clue.
 */
@OptIn(ExperimentalTestApi::class)
internal fun pickerForEachSkin(block: suspend ComposeUiTest.(name: String, skin: Skin) -> Unit) {
    for ((name, skin) in PickerSkins) {
        withClue(name) { runComposeUiTest { block(name, skin) } }
    }
}

/**
 * A skin over a resolved document, a measured layout and frozen motion.
 */
@Composable
internal fun PickerHarness(
    skin: Skin,
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(ThemeDocument(seed = PickerSeed)) }
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        BuilderTheme(skin, result, isDark, reducedMotion = false) {
            ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}

/**
 * The track named [name], told apart from its value field by the range it reports.
 */
internal fun pickerTrack(name: String): SemanticsMatcher =
    hasContentDescription(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

/**
 * Counts how often the picker's body composes.
 */
private class PickerCompositions {
    var count: Int = 0
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pickerTrackValue(name: String): Float =
    onNode(pickerTrack(name)).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

private val PickerUnstyled: Skin = Skin(Library.Unstyled, expressive = false)

/**
 * The value a picker under test was handed last, and every phase it reported.
 */
private class PickerReports(
    start: Argb,
) {
    var value: Argb by mutableStateOf(start)
    val phases: MutableList<EditPhase> = mutableListOf()
}

/**
 * A track as the web reads it, named by its text since it has no role there.
 */
private fun pickerTrackText(name: String): SemanticsMatcher =
    hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

/**
 * Shows a picker at [start], unstyled unless [skin] says otherwise, that is handed back every color it reports.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pickerShow(
    start: Argb = PickerSeed,
    direction: LayoutDirection = LayoutDirection.Ltr,
    foldsState: Boolean = false,
    skin: Skin = PickerUnstyled,
): PickerReports {
    val reports = PickerReports(start)
    setContent {
        PickerHarness(skin) {
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalFoldsStateIntoName provides foldsState,
            ) {
                HctPicker(
                    value = reports.value,
                    onChange = { argb, phase ->
                        reports.value = argb
                        reports.phases += phase
                    },
                    modifier = Modifier.width(PickerWidth),
                )
            }
        }
    }
    return reports
}

private fun pickerChromaOf(color: Argb): Double = Hct.fromInt(color.value).chroma

@OptIn(ExperimentalTestApi::class)
class HctPickerTest {
    @Test
    fun toneTrack_scriptedDrag_reportsDraggingThenOneReleasedWithoutRecomposingTheBody() =
        pickerForEachSkin { _, skin ->
            var value by mutableStateOf(PickerSeed)
            val phases = mutableListOf<EditPhase>()
            val compositions = PickerCompositions()
            setContent {
                PickerHarness(skin) {
                    HctPicker(
                        value = value,
                        onChange = { argb, phase ->
                            value = argb
                            phases += phase
                        },
                        modifier = Modifier.width(PickerWidth),
                        onBodyComposed = { compositions.count++ },
                    )
                }
            }
            waitForIdle()
            val composedBefore = compositions.count

            onNode(pickerTrack("Tone")).performTouchInput {
                down(Offset(width * 0.2f, centerY))
                for (step in 1..5) moveTo(Offset(width * (0.2f + step * 0.1f), centerY))
                up()
            }
            waitForIdle()

            phases.count { phase -> phase == EditPhase.Dragging } shouldBeGreaterThan 2
            phases.dropLast(1).all { phase -> phase == EditPhase.Dragging } shouldBe true
            phases.last() shouldBe EditPhase.Released
            phases.count { phase -> phase == EditPhase.Released } shouldBe 1
            value shouldNotBe PickerSeed
            compositions.count shouldBe composedBefore
        }

    @Test
    fun tracks_keys_stepByOneAndTenAndJumpToTheEnds() =
        pickerForEachSkin { _, skin ->
            var value by mutableStateOf(PickerSeed)
            val phases = mutableListOf<EditPhase>()
            setContent {
                PickerHarness(skin) {
                    HctPicker(
                        value = value,
                        onChange = { argb, phase ->
                            value = argb
                            phases += phase
                        },
                        modifier = Modifier.width(PickerWidth),
                    )
                }
            }
            val hue = onNode(pickerTrack("Hue"))
            hue.requestFocus()
            val start = pickerTrackValue("Hue")

            hue.performKeyInput { pressKey(Key.DirectionRight) }
            assertEquals(start + 1f, pickerTrackValue("Hue"), absoluteTolerance = 1e-3f)
            hue.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) } }
            assertEquals(start + 11f, pickerTrackValue("Hue"), absoluteTolerance = 1e-3f)
            hue.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionLeft) } }
            assertEquals(start + 1f, pickerTrackValue("Hue"), absoluteTolerance = 1e-3f)

            val tone = onNode(pickerTrack("Tone"))
            tone.requestFocus()
            tone.performKeyInput { pressKey(Key.MoveEnd) }
            assertEquals(100f, pickerTrackValue("Tone"))
            value shouldBe Argb(0xFFFFFF)
            tone.performKeyInput { pressKey(Key.MoveHome) }
            assertEquals(0f, pickerTrackValue("Tone"))
            value shouldBe Argb(0x000000)

            phases shouldBe List(5) { EditPhase.Discrete }
        }

    @Test
    fun hueTrack_rtl_mirrorsTheSideArrowsButNotUpAndDown() =
        runComposeUiTest {
            val reports = pickerShow(direction = LayoutDirection.Rtl)
            val hue = onNode(pickerTrack("Hue"))
            hue.requestFocus()
            val start = pickerTrackValue("Hue")

            hue.performKeyInput { pressKey(Key.DirectionRight) }
            pickerTrackValue("Hue") shouldBe start - 1f
            hue.performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
            }
            pickerTrackValue("Hue") shouldBe start + 1f
            hue.performKeyInput { pressKey(Key.DirectionUp) }
            pickerTrackValue("Hue") shouldBe start + 2f

            reports.phases shouldBe List(4) { EditPhase.Discrete }
        }

    @Test
    fun toneTrack_pageUpAndPageDown_stepByTen() =
        runComposeUiTest {
            val reports = pickerShow()
            val tone = onNode(pickerTrack("Tone"))
            tone.requestFocus()
            val start = pickerTrackValue("Tone")

            tone.performKeyInput { pressKey(Key.PageUp) }
            pickerTrackValue("Tone") shouldBe start + 10f
            tone.performKeyInput {
                pressKey(Key.PageDown)
                pressKey(Key.PageDown)
            }
            pickerTrackValue("Tone") shouldBe start - 10f

            reports.phases shouldBe List(3) { EditPhase.Discrete }
        }

    @Test
    fun chromaTrack_keys_stepTheChromaAndHomeTurnsGrey() =
        runComposeUiTest {
            val reports = pickerShow()
            val chroma = onNode(pickerTrack("Chroma"))
            chroma.requestFocus()
            val start = pickerTrackValue("Chroma")

            chroma.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionDown) } }
            assertEquals(start - 10f, pickerTrackValue("Chroma"), absoluteTolerance = 1f)
            chroma.performKeyInput { pressKey(Key.MoveHome) }
            waitForIdle()

            // A grey still has a little chroma in HCT, so the track reads a small number rather than 0.
            reports.value.green shouldBe reports.value.red
            reports.value.blue shouldBe reports.value.red
            reports.phases shouldBe List(2) { EditPhase.Discrete }
        }

    @Test
    fun chromaTrack_endOnTheSrgbEdge_keepsTheChroma() =
        runComposeUiTest {
            val green = Argb(0x00FF00)
            val reports = pickerShow(start = green)
            val chroma = onNode(pickerTrack("Chroma"))
            chroma.requestFocus()

            chroma.performKeyInput { pressKey(Key.MoveEnd) }
            waitForIdle()

            pickerChromaOf(reports.value) shouldBeGreaterThan pickerChromaOf(green) - 0.5
        }

    @Test
    fun chromaTrack_arrowUpAtAFractionalTone_keepsTheColorOffGrey() =
        runComposeUiTest {
            val navy = Argb(0x000011)
            val reports = pickerShow(start = navy)
            val chroma = onNode(pickerTrack("Chroma"))
            chroma.requestFocus()

            chroma.performKeyInput { pressKey(Key.DirectionUp) }
            waitForIdle()

            reports.value.blue shouldBeGreaterThan reports.value.red
            pickerChromaOf(reports.value) shouldBeGreaterThan pickerChromaOf(navy) - 0.5
        }

    @Test
    fun tracks_foldsStateIntoName_everySkin_carryTheirValueInTheirText() =
        pickerForEachSkin { _, skin ->
            // The seed #6750A4 sits at hue 298.98.
            pickerShow(foldsState = true, skin = skin)
            val hue = onNode(pickerTrackText("Hue, slider, 299"))
            hue.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            hue.requestFocus()

            hue.performKeyInput { pressKey(Key.DirectionRight) }

            onNode(pickerTrackText("Hue, slider, 300")).assertExists()
        }

    @Test
    fun toneTrack_pickerRemovedMidDrag_reportsNoRelease() =
        runComposeUiTest {
            var shown by mutableStateOf(true)
            val phases = mutableListOf<EditPhase>()
            setContent {
                PickerHarness(PickerUnstyled) {
                    if (shown) {
                        HctPicker(
                            value = PickerSeed,
                            onChange = { _, phase -> phases += phase },
                            modifier = Modifier.width(PickerWidth),
                        )
                    }
                }
            }

            onNode(pickerTrack("Tone")).performTouchInput {
                down(Offset(width * 0.2f, centerY))
                moveTo(Offset(width * 0.5f, centerY))
            }
            waitForIdle()
            shown = false
            waitForIdle()

            phases shouldContain EditPhase.Dragging
            phases shouldNotContain EditPhase.Released
        }

    @Test
    fun formatField_clampedColorInEachFormat_saysItWasPulledInsideSrgb() =
        runComposeUiTest {
            pickerShow()
            val clamped = listOf(
                "RGB" to "rgb(300 0 0)",
                "HSL" to "hsl(0 150% 50%)",
                "OKLCH" to "oklch(0.9 0.4 150)",
            )

            for ((format, text) in clamped) {
                withClue(format) {
                    onNodeWithText(format).performClick()
                    waitForIdle()
                    onNodeWithText("Pulled inside sRGB.", useUnmergedTree = true).assertDoesNotExist()
                    val field = onNode(hasSetTextAction() and hasContentDescription("Color"))
                    field.performTextReplacement(text)
                    field.performKeyInput { pressKey(Key.Enter) }
                    waitForIdle()

                    onNodeWithText("Pulled inside sRGB.", useUnmergedTree = true).assertExists()
                }
            }
        }

    @Test
    fun hueField_typedValue_movesTheTrackAsOneDiscreteEdit() =
        runComposeUiTest {
            var value by mutableStateOf(PickerSeed)
            val phases = mutableListOf<EditPhase>()
            setContent {
                PickerHarness(Skin(Library.Unstyled, expressive = false)) {
                    HctPicker(
                        value = value,
                        onChange = { argb, phase ->
                            value = argb
                            phases += phase
                        },
                        modifier = Modifier.width(PickerWidth),
                    )
                }
            }
            val field = onNode(hasSetTextAction() and hasContentDescription("Hue"))

            field.performTextReplacement("120")
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            pickerTrackValue("Hue") shouldBe 120f
            phases shouldBe listOf(EditPhase.Discrete)
        }

    @Test
    fun formatSwitch_rgb_showsTheColorAsRgbAndTakesAnyTypedColor() =
        runComposeUiTest {
            var value by mutableStateOf(PickerSeed)
            val phases = mutableListOf<EditPhase>()
            setContent {
                PickerHarness(Skin(Library.Unstyled, expressive = false)) {
                    HctPicker(
                        value = value,
                        onChange = { argb, phase ->
                            value = argb
                            phases += phase
                        },
                        modifier = Modifier.width(PickerWidth),
                    )
                }
            }

            onNodeWithText("RGB").performClick()
            waitForIdle()
            onNode(hasSetTextAction() and hasText("rgb(103 80 164)")).assertExists()
            val field = onNode(hasSetTextAction() and hasContentDescription("Color"))
            field.performTextReplacement("oklch(0.7 0.1 150)")
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            value shouldBe (ColorInput.parse("oklch(0.7 0.1 150)") as ParseResult.Ok).argb
            phases shouldBe listOf(EditPhase.Discrete)
            onNode(hasSetTextAction() and hasText("rgb(${value.red} ${value.green} ${value.blue})")).assertExists()
        }
}
