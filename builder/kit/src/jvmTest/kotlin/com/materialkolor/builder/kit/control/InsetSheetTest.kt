package com.materialkolor.builder.kit.control

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.shell.InversePosterSurface
import com.materialkolor.builder.kit.shell.PosterSurface
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class InsetSheetTest {
    @Test
    fun escape_inTheInversePoster_closesTheSheetAndHandsFocusBackToItsTrigger() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(false)
            setContent {
                ControlsHarness(skin) {
                    val trigger = remember { FocusRequester() }
                    PosterSurface(remember { PosterColors.of(Argb(0xD9653B)) }) {
                        BuilderInsetSheetHost(
                            modifier = Modifier.size(400.dp, 800.dp),
                            sheet = {
                                InversePosterSurface {
                                    BuilderInsetSheet(
                                        open = open,
                                        onDismiss = { open = false },
                                        title = "Fine-tune",
                                        returnFocusTo = trigger,
                                        maxHeight = 600.dp,
                                    ) {
                                        OverlayTestButton("inside")
                                    }
                                }
                            },
                        ) {
                            Box(Modifier.testTag("trigger").size(40.dp).focusRequester(trigger).focusable())
                        }
                    }
                }
            }
            onNodeWithTag("trigger").requestFocus()
            open = true
            waitForIdle()
            val focusedInside = onNode(isFocused() and hasAnyAncestor(hasOverlayPaneTitle("Fine-tune")))
            focusedInside.assertExists()

            focusedInside.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            open shouldBe false
            onNodeWithTag("trigger").assertIsFocused()
        }
}
