package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.ShortcutKey
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.widget.CodeScrollTag
import com.materialkolor.builder.kit.widget.CodeView
import com.materialkolor.builder.kit.widget.SelectableText
import com.materialkolor.builder.kit.widget.WidgetHarness
import com.materialkolor.builder.kit.widget.forEachWidgetSkin
import com.materialkolor.builder.kit.widget.widgetGoldenColorFile
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test

/**
 * Where overlays render in the page the code view and text over several lines swap their selection
 * container out while a finger is in use and back for a mouse or a key, and focus stays where it was
 * through the swap.
 */
@OptIn(ExperimentalTestApi::class)
class TouchlessSelectionTest {
    @Test
    fun codeView_focused_keepsFocusWhenThePointerChanges() =
        forEachWidgetSkin { _, skin ->
            setContent { Host(skin, coarsePointer = false, clipboard = remember { CopiedText() }) }
            val code = onNodeWithTag(CodeScrollTag)
            code.requestFocus()
            waitForIdle()
            code.assertIsFocused()

            code.performTouchInput { click(center) }
            waitForIdle()
            code.assertIsFocused()

            code.performMouseInput { moveTo(center) }
            waitForIdle()
            code.assertIsFocused()
        }

    @Test
    fun codeView_onATouchScreen_keepsFocusWhenAKeyBringsSelectionBack_thenCopiesWhatAMouseSelects() =
        forEachWidgetSkin { _, skin ->
            val file = widgetGoldenColorFile()
            val text = file.lines.joinToString("\n") { line -> line.joinToString("") { token -> token.text } }
            val clipboard = CopiedText()
            setContent { Host(skin, coarsePointer = true, clipboard = clipboard) }
            val code = onNodeWithTag(CodeScrollTag)
            code.requestFocus()
            waitForIdle()

            code.performKeyInput { pressKey(Key.ShiftLeft) }
            waitForIdle()
            code.assertIsFocused()

            code.performKeyInput { withKeyDown(ShortcutKey) { pressKey(Key.C) } }
            waitForIdle()
            clipboard.text.shouldBeNull()
            code.performMouseInput { dragAndDrop(center, centerRight - Offset(1f, 0f)) }
            waitForIdle()
            code.assertIsFocused()
            code.performKeyInput { withKeyDown(ShortcutKey) { pressKey(Key.C) } }
            waitForIdle()
            val copied = clipboard.text.shouldNotBeNull()
            copied.shouldNotBeBlank()
            text.filterNot(Char::isWhitespace) shouldContain copied.filterNot(Char::isWhitespace)
        }

    @Test
    fun selectableLines_onATouchScreen_getTheirSelectionBackOnAKey() =
        forEachWidgetSkin { _, skin ->
            setContent { LinesHost(skin) }
            val lines = onNodeWithTag(LinesTag)
            // The box that stands in for the container is focusable and says so, the container does not.
            lines.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
            lines.requestFocus()
            waitForIdle()

            lines.performKeyInput { pressKey(Key.ShiftLeft) }
            waitForIdle()
            lines.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        }

    @Test
    fun codeView_onATouchScreen_letsTabTakeFocusOn() =
        forEachWidgetSkin { _, skin ->
            setContent { Host(skin, coarsePointer = true, clipboard = remember { CopiedText() }) }
            val code = onNodeWithTag(CodeScrollTag)
            code.requestFocus()
            waitForIdle()

            code.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            code.assertIsNotFocused()
        }
}

/**
 * A code view in [skin] with overlays in the page, copying to [clipboard].
 */
@Composable
private fun Host(
    skin: Skin,
    coarsePointer: Boolean,
    clipboard: Clipboard,
) {
    val file = remember { widgetGoldenColorFile() }
    CompositionLocalProvider(LocalOverlaysInTree provides true, LocalClipboard provides clipboard) {
        WidgetHarness(skin, coarsePointer = coarsePointer) {
            CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp))
        }
    }
}

/**
 * Two lines of selectable text in [skin] on a touch screen, with overlays in the page.
 */
@Composable
private fun LinesHost(skin: Skin) {
    CompositionLocalProvider(LocalOverlaysInTree provides true) {
        WidgetHarness(skin, coarsePointer = true) {
            SelectableText(Lines, Modifier.testTag(LinesTag))
        }
    }
}

private const val LinesTag = "lines"

private const val Lines = "val primary = Color(0xFF6750A4)\nval onPrimary = Color(0xFFFFFFFF)"

/**
 * A clipboard that keeps the last text written to it.
 */
private class CopiedText : Clipboard {
    var text: String? = null

    override suspend fun getClipEntry(): ClipEntry? = null

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        text = clipEntry?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor) as? String
    }
}
