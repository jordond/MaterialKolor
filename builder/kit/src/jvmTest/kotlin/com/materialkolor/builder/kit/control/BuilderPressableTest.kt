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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
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
    fun pressable_everySkin_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderPressable(onClick = {}, label = PressableLabel) { Box(Modifier.size(46.dp)) }
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }
}
