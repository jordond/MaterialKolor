package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val PressableTag = "pressable"

private const val PressableLabel = "Open the eyedropper"

@OptIn(ExperimentalTestApi::class)
class BuilderPressableTest {
    @Test
    fun pressable_foldOnAndOff_everySkin_readsAsOneButtonNamedByItsLabel() {
        for (folds in listOf(true, false)) {
            withClue("folds $folds") {
                forEachSkin { _, skin ->
                    var presses = 0
                    var enabled by mutableStateOf(true)
                    setContent {
                        ControlsHarness(skin) {
                            CompositionLocalProvider(LocalFoldsStateIntoName provides folds) {
                                BuilderPressable(
                                    onClick = { presses += 1 },
                                    label = PressableLabel,
                                    modifier = Modifier.testTag(PressableTag),
                                    enabled = enabled,
                                ) {
                                    Box(Modifier.size(46.dp).semantics { contentDescription = "Beach" })
                                    BuilderText("Beach")
                                }
                            }
                        }
                    }
                    val pressable = onNodeWithTag(PressableTag)
                        .assert(hasRole(Role.Button))
                        .assert(hasContentDescriptionExactly(PressableLabel))
                        .assert(!hasText("Beach"))
                        .assertIsEnabled()

                    pressable.performClick()
                    waitForIdle()
                    presses shouldBe 1

                    enabled = false
                    waitForIdle()
                    val disabledName = if (folds) "$PressableLabel, disabled" else PressableLabel
                    pressable.assertIsNotEnabled().assert(hasContentDescriptionExactly(disabledName))
                    pressable.performClick()
                    waitForIdle()
                    presses shouldBe 1
                }
            }
        }
    }

    @Test
    fun pressable_selectedOrNot_foldOn_saysSelectedInItsNameOnlyWhenSelected() {
        var selected by mutableStateOf(true)
        runComposeUiTest {
            setContent {
                ControlsHarness(ControlSkins.first().second) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides true) {
                        BuilderPressable(
                            onClick = {},
                            label = PressableLabel,
                            modifier = Modifier.testTag(PressableTag),
                            selected = selected,
                        ) { Box(Modifier.size(46.dp)) }
                    }
                }
            }
            onNodeWithTag(PressableTag)
                .assert(hasContentDescriptionExactly("$PressableLabel, selected"))
                .assertIsSelected()

            selected = false
            waitForIdle()
            onNodeWithTag(PressableTag)
                .assert(hasContentDescriptionExactly(PressableLabel))
                .assert(!isSelected())
        }
    }

    /**
     * On a phone the primary touch target, 48 dp, stands above the least one, 44 dp, so a pressable
     * that takes the least one would show here.
     */
    @Test
    fun pressable_smallContentOnAPhone_everySkin_growsToThePrimaryTouchTarget() =
        forEachSkin { _, skin ->
            val phone = LayoutInfo.of(widthDp = 400.dp, heightDp = 800.dp)
            phone.primaryTouchTarget shouldBeGreaterThan phone.minTouchTarget
            setContent {
                ControlsHarness(skin) {
                    CompositionLocalProvider(LocalLayout provides phone) {
                        BuilderPressable(
                            onClick = {},
                            label = PressableLabel,
                            modifier = Modifier.testTag(PressableTag),
                        ) { Box(Modifier.size(16.dp)) }
                    }
                }
            }
            onNodeWithTag(PressableTag)
                .assertWidthIsEqualTo(phone.primaryTouchTarget)
                .assertHeightIsEqualTo(phone.primaryTouchTarget)
        }

    @Test
    fun pressable_everySkin_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderPressable(onClick = {}, label = PressableLabel) { Box(Modifier.size(46.dp)) }
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }
}
