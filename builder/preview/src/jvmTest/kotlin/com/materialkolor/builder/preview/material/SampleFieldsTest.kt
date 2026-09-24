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
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-228b

private const val MaterialTag = "material"
private const val SampleTag = "sample"

/**
 * The sample fields are Material3's own fields put together from their parts, so the handles can be
 * kept off on the web (D45). Off the web they draw pixel for pixel what Material3's fields draw.
 */
@OptIn(ExperimentalTestApi::class)
class SampleFieldsTest {
    @Test
    fun sampleTextField_drawsWhatTextFieldDraws() {
        for (enabled in listOf(true, false)) {
            withClue("enabled $enabled") {
                runComposeUiTest {
                    sideBySide(
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
        for (enabled in listOf(true, false)) {
            withClue("enabled $enabled") {
                runComposeUiTest {
                    sideBySide(
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

    /** Draws [material] and [sample] one above the other and checks their pixels match. */
    private fun ComposeUiTest.sideBySide(
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
        val expected = onNodeWithTag(MaterialTag).captureToImage().toPixelMap()
        val actual = onNodeWithTag(SampleTag).captureToImage().toPixelMap()
        actual.width shouldBe expected.width
        actual.height shouldBe expected.height
        actual.buffer.contentEquals(expected.buffer) shouldBe true
    }
}
