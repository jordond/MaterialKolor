package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A line longer than the code view is wide, like the link back in an export's header, and one far
 * shorter, whose width leaves the least slack for rounding.
 */
private val Lines = listOf(
    "// Open this theme in the builder at https://materialkolor.com/t/AXuM_xMEABALQnVybnQgRW1iZXI",
    "val x = 1",
)

/**
 * A line runs on past the code view's edge for it to scroll to, and none is cut short.
 */
@OptIn(ExperimentalTestApi::class)
class CodeViewLinesTest {
    @Test
    fun codeView_longestLine_isLaidOutWhole() {
        for (line in Lines) {
            withClue(line) {
                forEachWidgetSkin { _, skin ->
                    setContent {
                        WidgetHarness(skin) {
                            CodeView(
                                listOf(listOf(Token(TokenKind.Plain, line))),
                                onCopy = null,
                                Modifier.size(320.dp, 160.dp),
                            )
                        }
                    }
                    val layouts = mutableListOf<TextLayoutResult>()
                    onNode(hasText(line), useUnmergedTree = true)
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get -> get(layouts) }
                    layouts.single().didOverflowWidth shouldBe false
                }
            }
        }
    }
}
