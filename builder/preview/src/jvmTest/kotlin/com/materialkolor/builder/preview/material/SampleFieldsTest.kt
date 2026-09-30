package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val MaterialTag = "material"
private const val SampleTag = "sample"

/**
 * Past the focus animations of the label and the indicator, and halfway through the caret's first
 * 500 ms off phase, so neither capture holds a caret and the compare leaves it out.
 */
private const val FocusSettleMillis = 750L

/**
 * The sample fields are Material3's own fields put together from their parts, so the handles can be
 * kept off on the web. Off the web they draw pixel for pixel what Material3's fields draw.
 */
@OptIn(ExperimentalTestApi::class)
class SampleFieldsTest {
    @Test
    fun sampleTextField_drawsWhatTextFieldDraws() {
        for ((enabled, focused) in FieldStates) {
            withClue("enabled $enabled, focused $focused") {
                runComposeUiTest {
                    sideBySide(
                        focused = focused,
                        material = { modifier ->
                            TextField(
                                value = "Lisbon",
                                onValueChange = {},
                                modifier = modifier,
                                enabled = enabled,
                                label = { Text("Destination") },
                                singleLine = true,
                            )
                        },
                        sample = { modifier ->
                            SampleTextField("Lisbon", {}, "Destination", modifier, enabled = enabled)
                        },
                    )
                }
            }
        }
    }

    @Test
    fun sampleOutlinedTextField_drawsWhatOutlinedTextFieldDraws() {
        for ((enabled, focused) in FieldStates) {
            withClue("enabled $enabled, focused $focused") {
                runComposeUiTest {
                    sideBySide(
                        focused = focused,
                        material = { modifier ->
                            OutlinedTextField(
                                value = "Lisbon",
                                onValueChange = {},
                                modifier = modifier,
                                enabled = enabled,
                                label = { Text("Destination") },
                                singleLine = true,
                            )
                        },
                        sample = { modifier ->
                            SampleOutlinedTextField("Lisbon", {}, "Destination", modifier, enabled = enabled)
                        },
                    )
                }
            }
        }
    }

    /**
     * Draws [material] and [sample] one above the other and checks their pixels match. When [focused]
     * each is focused in turn and drawn once its label and indicator have settled on their focused
     * colours, on a clock held still while both carets are off.
     */
    private fun ComposeUiTest.sideBySide(
        focused: Boolean,
        material: @Composable (Modifier) -> Unit,
        sample: @Composable (Modifier) -> Unit,
    ) {
        setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    material(Modifier.fillMaxWidth().testTag(MaterialTag))
                    sample(Modifier.fillMaxWidth().testTag(SampleTag))
                }
            }
        }
        waitForIdle()
        if (focused) mainClock.autoAdvance = false
        val expected = pixelsOf(MaterialTag, focused)
        val actual = pixelsOf(SampleTag, focused)
        actual.width shouldBe expected.width
        actual.height shouldBe expected.height
        actual.buffer.contentEquals(expected.buffer) shouldBe true
    }

    /**
     * The pixels of the field tagged [tag], focused first when [focused].
     */
    private fun ComposeUiTest.pixelsOf(
        tag: String,
        focused: Boolean,
    ): PixelMap {
        if (focused) {
            onNodeWithTag(tag).requestFocus()
            mainClock.advanceTimeBy(FocusSettleMillis)
        }
        return onNodeWithTag(tag).captureToImage().toPixelMap()
    }
}

/**
 * Enabled and unfocused, enabled and focused, and disabled, as pairs of enabled and focused.
 */
private val FieldStates: List<Pair<Boolean, Boolean>> = listOf(true to false, true to true, false to false)
