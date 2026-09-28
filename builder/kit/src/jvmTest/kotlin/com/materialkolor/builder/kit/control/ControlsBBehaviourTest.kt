package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.SliderRules
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val Slider = "slider"
private const val LeftToRight = "slider-ltr"
private const val RightToLeft = "slider-rtl"

/**
 * The contrast control's stops and snap distance.
 */
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
    fun tabs_rightToLeft_arrowsFollowTheReadingOrder() =
        forEverySkin { variant ->
            var selected by mutableStateOf("App")
            setSkinnedContent(variant) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BuilderTabs(listOf("App", "Components", "Roles"), selected, { selected = it }, { it })
                }
            }

            tab("App").requestFocus()
            tab("App").performKeyInput { pressKey(Key.DirectionLeft) }
            selected shouldBe "Components"
            tab("Components").assertIsFocused().assertIsSelected()
            tab("Components").performKeyInput { pressKey(Key.DirectionRight) }
            tab("App").performKeyInput { pressKey(Key.DirectionRight) }
            selected shouldBe "Roles"
            tab("Roles").assertIsFocused().assertIsSelected()
        }

    @Test
    fun tabs_tooManyForTheRow_scrollTheFocusedTabIntoView() =
        forEverySkin { variant ->
            val tabs = listOf("App", "Components", "Roles", "Palettes", "Contrast", "Export")
            var selected by mutableStateOf("App")
            setSkinnedContent(variant) {
                BuilderTabs(tabs, selected, { selected = it }, { it }, Modifier.width(160.dp))
            }

            tab("Export").assertIsNotDisplayed()
            tab("App").requestFocus()
            tab("App").performKeyInput { pressKey(Key.MoveEnd) }
            selected shouldBe "Export"
            tab("Export").assertIsFocused().assertIsDisplayed()
        }

    @Test
    fun slider_keyThatMovesNothing_finishesNothing() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(-1f)
            var finished = 0
            setSkinnedContent(variant) {
                ContrastSlider(value, { value = it }, { finished++ })
            }

            onNodeWithTag(Slider).requestFocus()
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.MoveHome) }
            value shouldBe -1f
            finished shouldBe 0
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.MoveEnd) }
            value shouldBe 1f
            finished shouldBe 1
            onNodeWithTag(Slider).performKeyInput { pressKey(Key.DirectionRight) }
            value shouldBe 1f
            finished shouldBe 1
        }

    @Test
    fun slider_setProgress_clampsWithoutSnappingAndFinishesOnlyOnAChange() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(0.5f)
            var finished = 0
            setSkinnedContent(variant) {
                ContrastSlider(value, { value = it }, { finished++ })
            }

            onNodeWithTag(Slider).performSemanticsAction(SemanticsActions.SetProgress) { it(0.47f) }
            value shouldBe 0.47f
            finished shouldBe 1
            onNodeWithTag(Slider).performSemanticsAction(SemanticsActions.SetProgress) { it(0.47f) }
            finished shouldBe 1
            onNodeWithTag(Slider).performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
            value shouldBe 1f
            finished shouldBe 2
        }

    @Test
    fun slider_disabled_turnsSetProgressDown() =
        forEverySkin { variant ->
            var value by mutableFloatStateOf(0.5f)
            setSkinnedContent(variant) {
                BuilderSlider(value, { value = it }, "Chroma", Modifier.testTag(Slider).width(320.dp), enabled = false)
            }

            onNodeWithTag(Slider).performSemanticsAction(SemanticsActions.SetProgress) { it(0.2f) }
            value shouldBe 0.5f
        }

    @Test
    fun slider_rightToLeft_mirrorsTheTrackWithTheThumb() =
        forEverySkin { variant ->
            setSkinnedContent(variant) {
                Column {
                    ContrastSlider(0.5f, {}, {}, tag = LeftToRight)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ContrastSlider(0.5f, {}, {}, tag = RightToLeft)
                    }
                }
            }

            val ltr = onNodeWithTag(LeftToRight).captureToImage().toPixelMap()
            val rtl = onNodeWithTag(RightToLeft).captureToImage().toPixelMap()
            val y = ltr.height / 2
            val start = (ltr.width * 0.1f).toInt()
            val end = (ltr.width * 0.9f).toInt()
            // Three quarters along, the start is on the active track and the end on the rest.
            ltr[start, y] shouldNotBe ltr[end, y]
            rtl[ltr.width - 1 - start, y] shouldBe ltr[start, y]
            rtl[ltr.width - 1 - end, y] shouldBe ltr[end, y]
        }
}

@Composable
private fun ContrastSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    tag: String = Slider,
) {
    BuilderSlider(
        value = value,
        onValueChange = onValueChange,
        label = "Contrast",
        modifier = Modifier.testTag(tag).width(400.dp),
        onValueChangeFinished = onValueChangeFinished,
        valueRange = -1f..1f,
        step = 0.01f,
        stops = ContrastStops,
        snapDistance = ContrastSnap,
    )
}
