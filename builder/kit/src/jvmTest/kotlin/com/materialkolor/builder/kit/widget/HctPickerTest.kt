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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorInput
import com.materialkolor.builder.domain.color.ParseResult
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertEquals

/** The color every picker test starts from. */
internal val PickerSeed: Argb = Argb(0x6750A4)

/** Wide enough for the tracks and their value fields side by side. */
internal val PickerWidth: Dp = 440.dp

/** Every skin the picker is drawn in, named for the screenshots. */
internal val PickerSkins: List<Pair<String, Skin>> = listOf(
    "material3" to Skin(Library.Material3, expressive = false),
    "expressive" to Skin(Library.Material3, expressive = true),
    "unstyled" to Skin(Library.Unstyled, expressive = false),
    "custom" to Skin(Library.Custom, expressive = false),
    "fluent" to Skin(Library.Fluent, expressive = false),
)

/** Runs [block] once per skin in a fresh test, with the skin's name as the clue. */
@OptIn(ExperimentalTestApi::class)
internal fun pickerForEachSkin(block: suspend ComposeUiTest.(name: String, skin: Skin) -> Unit) {
    for ((name, skin) in PickerSkins) {
        withClue(name) { runComposeUiTest { block(name, skin) } }
    }
}

/** A skin over a resolved document, a measured layout and frozen motion. */
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

/** The track named [name], told apart from its value field by the range it reports. */
internal fun pickerTrack(name: String): SemanticsMatcher =
    hasContentDescription(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

/** Counts how often the picker's body composes. */
private class PickerCompositions {
    var count: Int = 0
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pickerTrackValue(name: String): Float =
    onNode(pickerTrack(name)).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

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
