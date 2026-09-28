package com.materialkolor.builder.kit.a11y

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val TargetTag = "focus-target"

@OptIn(ExperimentalTestApi::class)
class FocusVisibilityTest {
    @Test
    fun focusVisibility_keyAndPointerPresses_turnTheRingsOnAndOff() =
        runComposeUiTest {
            val visibility = FocusVisibility()
            setContent {
                Box(Modifier.fillMaxSize().trackFocusVisibility(visibility)) {
                    Box(Modifier.size(40.dp).testTag(TargetTag).focusable())
                }
            }
            val target = onNodeWithTag(TargetTag)
            target.requestFocus()
            visibility.isVisible shouldBe true

            target.performMouseInput { click() }
            waitForIdle()
            visibility.isVisible shouldBe false

            target.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
            visibility.isVisible shouldBe false

            target.performKeyInput { pressKey(Key.Tab) }
            visibility.isVisible shouldBe true
        }
}
