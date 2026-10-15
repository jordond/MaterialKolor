package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WidgetsWebNameTest {
    private val list = SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)

    @Test
    fun codeView_flagOn_everySkin_namesItsLines() =
        forEachWidgetSkin { _, skin ->
            val file = widgetGoldenColorFile()
            setContent {
                WidgetHarness(skin) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides true) {
                        CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp), label = "Color.kt")
                    }
                }
            }

            onNode(list and hasContentDescription("Color.kt"), useUnmergedTree = true).assertExists()
        }

    @Test
    fun codeView_flagOff_everySkin_leavesItsLinesAsTheyAre() =
        forEachWidgetSkin { _, skin ->
            val file = widgetGoldenColorFile()
            setContent {
                WidgetHarness(skin) { CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp)) }
            }

            onNode(list, useUnmergedTree = true)
                .assertExists()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        }
}
