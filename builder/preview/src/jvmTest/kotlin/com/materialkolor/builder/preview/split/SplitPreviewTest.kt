package com.materialkolor.builder.preview.split

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val SPLIT = "split"
private const val WIFI = "wifi"

@OptIn(ExperimentalTestApi::class)
class SplitPreviewTest {
    @Test
    fun click_onEitherSide_landsOnTheVisibleCopyAndFlipsBoth() = clickEitherSide(LayoutDirection.Ltr)

    @Test
    fun click_onEitherSideRightToLeft_landsOnTheVisibleCopyAndFlipsBoth() = clickEitherSide(LayoutDirection.Rtl)

    /** Click the end side then the start side, wherever [direction] puts them. */
    private fun clickEitherSide(direction: LayoutDirection) =
        runComposeUiTest {
            val state = DemoAppState()
            val handled = mutableListOf<String>()
            val seen = mutableMapOf<String, Boolean>()
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Chrome {
                        SplitPreview(
                            LightSpec,
                            DarkSpec,
                            SplitState(),
                            Modifier.size(400.dp, 300.dp).testTag(SPLIT),
                        ) { spec ->
                            val on = state.isOn(WIFI)
                            seen[spec.label] = on
                            Box(
                                Modifier.fillMaxSize().testTag(WIFI).toggleable(value = on) { checked ->
                                    state.setOn(WIFI, checked)
                                    handled += spec.label
                                },
                            )
                        }
                    }
                }
            }
            val (startX, endX) = if (direction == LayoutDirection.Ltr) 0.1f to 0.9f else 0.9f to 0.1f

            onNodeWithTag(SPLIT).performTouchInput { click(Offset(width * endX, height / 2f)) }
            waitForIdle()
            handled shouldContainExactly listOf("Dark")
            seen shouldBe mapOf("Light" to true, "Dark" to true)

            onNodeWithTag(SPLIT).performTouchInput { click(Offset(width * startX, height / 2f)) }
            waitForIdle()
            handled shouldContainExactly listOf("Dark", "Light")
            seen shouldBe mapOf("Light" to false, "Dark" to false)
        }

    @Test
    fun semantics_withBothCopiesComposed_holdOneApp() =
        runComposeUiTest {
            setContent {
                Chrome {
                    SplitPreview(LightSpec, DarkSpec, SplitState(), Modifier.size(400.dp, 300.dp)) {
                        Box(Modifier.fillMaxSize().testTag(WIFI).toggleable(value = false) { })
                    }
                }
            }

            // Assistive tech reads the merged tree, where the cleared copy has no children left.
            onAllNodesWithTag(WIFI).assertCountEquals(1)
        }

    @Test
    fun focusTraversal_throughTheWholePreview_neverEntersTheClippedCopy() =
        runComposeUiTest {
            val focused = mutableSetOf<String>()
            lateinit var focusManager: FocusManager
            setContent {
                focusManager = LocalFocusManager.current
                Chrome {
                    SplitPreview(LightSpec, DarkSpec, SplitState(), Modifier.size(400.dp, 300.dp)) { spec ->
                        Column {
                            repeat(3) { index ->
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .onFocusChanged { focus ->
                                            if (focus.isFocused) {
                                                focused +=
                                                    "${spec.label}/$index"
                                            }
                                        }.focusable(),
                                )
                            }
                        }
                    }
                }
            }

            repeat(12) {
                runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
            }
            waitForIdle()

            focused shouldBe setOf("Light/0", "Light/1", "Light/2")
        }

    @Test
    fun tap_onATextFieldOnTheEndSide_focusesItAndTakesKeysButTabStillSkipsTheEndCopy() =
        runComposeUiTest {
            val focused = mutableSetOf<String>()
            val keys = mutableListOf<String>()
            lateinit var focusManager: FocusManager
            setContent {
                focusManager = LocalFocusManager.current
                Chrome {
                    SplitPreview(
                        LightSpec,
                        DarkSpec,
                        SplitState(),
                        Modifier.size(400.dp, 300.dp).testTag(SPLIT),
                    ) { spec ->
                        BasicTextField(
                            state = rememberTextFieldState(),
                            modifier = Modifier
                                .fillMaxSize()
                                .onFocusChanged { focus -> if (focus.isFocused) focused += spec.label }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) keys += "${spec.label}/${event.key}"
                                    false
                                },
                        )
                    }
                }
            }

            onNodeWithTag(SPLIT).performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
            waitForIdle()
            focused shouldBe setOf("Dark")

            onNodeWithTag(SPLIT).performKeyInput { pressKey(Key.A) }
            keys shouldBe listOf("Dark/${Key.A}")

            runOnIdle { focusManager.clearFocus() }
            focused.clear()
            repeat(4) {
                runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
            }
            waitForIdle()
            focused shouldBe setOf("Light")
        }

    @Test
    fun drag_acrossTheWholeRange_sweepsTheHandleWithoutRecomposingOrRedrawingEitherCopy() =
        runComposeUiTest {
            val split = SplitState()
            val compositions = mutableMapOf<String, Int>()
            val count = { where: String -> compositions[where] = (compositions[where] ?: 0) + 1 }
            val draws = mutableMapOf<String, Int>()
            setContent {
                Chrome {
                    CompositionLocalProvider(LocalCompositionProbe provides count) {
                        SplitPreview(
                            start = LightSpec,
                            end = DarkSpec,
                            split = split,
                            modifier = Modifier.padding(100.dp).size(400.dp, 300.dp).testTag(SPLIT),
                        ) { spec ->
                            count(spec.label)
                            Box(Modifier.fillMaxSize().drawBehind { draws[spec.label] = (draws[spec.label] ?: 0) + 1 })
                        }
                    }
                }
            }
            waitForIdle()
            val before = compositions.toMap()
            val drawnBefore = draws.toMap()

            val step = onNodeWithTag(SPLIT).fetchSemanticsNode().size.width / 30f
            val samples = mutableListOf(split.fraction)
            val handle = onNodeWithContentDescription("Split")
            handle.performTouchInput { down(center) }
            repeat(20) {
                handle.performTouchInput { moveBy(Offset(-step, 0f)) }
                samples += split.fraction
            }
            repeat(45) {
                handle.performTouchInput { moveBy(Offset(step, 0f)) }
                samples += split.fraction
            }
            handle.performTouchInput { up() }
            waitForIdle()

            samples.min() shouldBe 0f
            samples.max() shouldBe 1f
            samples.distinct() shouldHaveAtLeastSize 30
            before.keys shouldBe setOf("Light", "Dark", SPLIT_PREVIEW, "PreviewPane/Light", "PreviewPane/Dark")
            compositions shouldBe before
            drawnBefore.keys shouldBe setOf("Light", "Dark")
            draws shouldBe drawnBefore
        }

    @Test
    fun handleKeys_arrowsHomeEndAndEnter_moveAndResetTheSplit() =
        runComposeUiTest {
            val split = SplitState()
            setContent {
                Chrome {
                    SplitPreview(LightSpec, DarkSpec, split, Modifier.size(400.dp, 300.dp)) { }
                }
            }
            val handle = onNodeWithContentDescription("Split")
            handle.requestFocus()

            handle.performKeyInput { pressKey(Key.DirectionRight) }
            split.fraction shouldBe (0.55f plusOrMinus 0.001f)
            handle.assert(hasStateDescription("55% Light"))

            handle.performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
            }
            split.fraction shouldBe (0.45f plusOrMinus 0.001f)

            handle.performKeyInput { pressKey(Key.MoveHome) }
            split.fraction shouldBe 0f
            handle.performKeyInput { pressKey(Key.MoveEnd) }
            split.fraction shouldBe 1f
            handle.performKeyInput { pressKey(Key.Enter) }
            split.fraction shouldBe 0.5f
        }

    @Test
    fun handleKeys_rightToLeft_mirrorTheArrows() =
        runComposeUiTest {
            val split = SplitState()
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Chrome {
                        SplitPreview(LightSpec, DarkSpec, split, Modifier.size(400.dp, 300.dp)) { }
                    }
                }
            }
            val handle = onNodeWithContentDescription("Split")
            handle.requestFocus()

            handle.performKeyInput { pressKey(Key.DirectionRight) }

            split.fraction shouldBe (0.45f plusOrMinus 0.001f)
        }

    @Test
    fun handleSemantics_setProgressAndDoubleClick_moveAndResetTheSplit() =
        runComposeUiTest {
            val split = SplitState()
            setContent {
                Chrome {
                    SplitPreview(LightSpec, DarkSpec, split, Modifier.size(400.dp, 300.dp)) { }
                }
            }
            val handle = onNodeWithContentDescription("Split")
            handle.assert(hasStateDescription("50% Light"))
            // Off the web the value stays out of the name, which only the web's text carries.
            handle.assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            handle.assertRangeInfoEquals(ProgressBarRangeInfo(current = 0.5f, range = 0f..1f))

            handle.performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.3f) }
            split.fraction shouldBe 0.3f
            handle.assert(hasStateDescription("30% Light"))

            handle.performTouchInput { doubleClick() }
            waitForIdle()
            split.fraction shouldBe 0.5f
        }

    @Test
    fun fraction_setPastEitherEnd_isHeldAtThatEnd() {
        val split = SplitState()

        split.fraction = 1.4f
        split.fraction shouldBe 1f
        split.fraction = -0.2f
        split.fraction shouldBe 0f
    }

    @Test
    fun splitShape_eachDirectionAndAxis_keepsThePartPastTheHandle() {
        val size = Size(200f, 100f)
        val density = Density(1f)

        SplitShape(0.25f).rect(size, LayoutDirection.Ltr, density) shouldBe Rect(50f, 0f, 200f, 100f)
        SplitShape(0.25f).rect(size, LayoutDirection.Rtl, density) shouldBe Rect(0f, 0f, 150f, 100f)
        SplitShape(0.25f, Orientation.Vertical).rect(size, LayoutDirection.Rtl, density) shouldBe
            Rect(0f, 25f, 200f, 100f)
    }

    @Test
    fun splitShape_fractionBetweenPixels_putsTheEdgeOnAWholePixel() {
        val size = Size(201f, 101f)
        val density = Density(1f)

        SplitShape(0.3f).rect(size, LayoutDirection.Ltr, density) shouldBe Rect(60f, 0f, 201f, 101f)
        SplitShape(0.3f).rect(size, LayoutDirection.Rtl, density) shouldBe Rect(0f, 0f, 141f, 101f)
        SplitShape(0.3f, Orientation.Vertical).rect(size, LayoutDirection.Ltr, density) shouldBe
            Rect(0f, 30f, 201f, 101f)
    }

    private fun SplitShape.rect(
        size: Size,
        direction: LayoutDirection,
        density: Density,
    ): Rect = (createOutline(size, direction, density) as Outline.Rectangle).rect
}
