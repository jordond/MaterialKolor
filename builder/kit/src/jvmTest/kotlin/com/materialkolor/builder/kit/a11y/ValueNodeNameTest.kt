package com.materialkolor.builder.kit.a11y

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val ValueTag = "value-node"

@OptIn(ExperimentalTestApi::class)
class ValueNodeNameTest {
    @Test
    fun valueNodeName_foldOnAndOff_namesTheNodeByTextOrDescriptionAndFollowsTheValue() {
        for (folds in listOf(true, false)) {
            withClue("folds $folds") {
                runComposeUiTest {
                    var percent by mutableFloatStateOf(50f)
                    setContent {
                        CompositionLocalProvider(LocalFoldsStateIntoName provides folds) {
                            val fold = foldsValueIntoName
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .testTag(ValueTag)
                                    .semantics { valueNodeName("Split", "${percent.toInt()}% Light", fold) },
                            )
                        }
                    }

                    valueNode() shouldBe ValueNode(
                        text = if (folds) "Split, 50% Light" else null,
                        description = if (folds) null else "Split",
                        state = "50% Light",
                    )

                    percent = 55f
                    waitForIdle()
                    valueNode().text shouldBe if (folds) "Split, 55% Light" else null
                    valueNode().state shouldBe "55% Light"
                }
            }
        }
    }

    @Test
    fun valueNodeName_withTheSliderWord_readsItBetweenTheNameAndTheValueOnlyOnTheWeb() {
        for (folds in listOf(true, false)) {
            withClue("folds $folds") {
                runComposeUiTest {
                    setContent {
                        CompositionLocalProvider(LocalFoldsStateIntoName provides folds) {
                            val fold = foldsValueIntoName
                            val slider = sliderRoleWord
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .testTag(ValueTag)
                                    .semantics { valueNodeName("Split", "50% Light", fold, roleWord = slider) },
                            )
                        }
                    }

                    valueNode() shouldBe ValueNode(
                        text = if (folds) "Split, slider, 50% Light" else null,
                        description = if (folds) null else "Split",
                        state = "50% Light",
                    )
                }
            }
        }
    }
}

/**
 * What a value node says, its text, its content description and its state description.
 */
private data class ValueNode(
    val text: String?,
    val description: String?,
    val state: String?,
)

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.valueNode(): ValueNode {
    val config = onNodeWithTag(ValueTag).fetchSemanticsNode().config
    return ValueNode(
        text = config.getOrNull(SemanticsProperties.Text)?.joinToString { text: AnnotatedString -> text.text },
        description = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
        state = config.getOrNull(SemanticsProperties.StateDescription),
    )
}
