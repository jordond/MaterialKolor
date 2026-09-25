package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderSelect
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.HostOverlays
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.widget.CodeView
import com.materialkolor.builder.kit.widget.SelectableText
import com.materialkolor.builder.kit.widget.widgetGoldenColorFile
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlin.test.Test

// b-228a

/**
 * Tags the field under the long press.
 */
private const val FieldTag = "field"

/**
 * D45. A touch long press on a field puts up no selection handle where overlays render in the page,
 * since the handles are popups that would take the web mirror over, and the page's text toolbar
 * still shows. With overlays in windows of their own the handles come up as ever, which shows the
 * test would see them.
 */
@OptIn(ExperimentalTestApi::class)
class SelectionHandlesTest {
    @Test
    fun textField_inTree_takesALongPressWithNoHandleAndShowsTheRow() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { BuilderTextField("Ocean", {}, "Project name", Tagged) } }
            longPressField()
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
            onNodeWithText("Copy").assertExists()
        }

    @Test
    fun textField_inWindows_putsUpTheHandles() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = false) { BuilderTextField("Ocean", {}, "Project name", Tagged) } }
            longPressField()
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
            onAllNodes(isPopup()).fetchSemanticsNodes().size shouldBeGreaterThan 0
        }

    @Test
    fun heroField_inTree_takesALongPressWithNoHandleAndShowsTheRow() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { HeroField() } }
            longPressField()
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
            onNodeWithText("Copy").assertExists()
        }

    @Test
    fun heroField_inWindows_putsUpTheHandles() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = false) { HeroField() } }
            longPressField()
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
        }

    // b-228aa
    // Foundation keeps the handles down until the finger lifts, and a lift on the field is a click
    // that opens the select and drops the selection. So the finger slides off the field first.
    @Test
    fun select_inTree_takesALongPressWithNoHandle() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { Select() } }
            longPressAndSlideOff()
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
        }

    @Test
    fun materialSelect_inWindows_putsUpTheHandles() =
        forEachSkin { _, skin ->
            if (skin.library != Library.Material3) return@forEachSkin
            setContent { Host(skin, inTree = false) { Select() } }
            longPressAndSlideOff()
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
        }

    @Test
    fun selectableLine_inTree_takesALongPressWithNoHandleAndShowsTheRow() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { SelectableText(Link, modifier = Tagged) } }
            longPressField()
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
            onNodeWithText("Copy").assertExists()
        }

    @Test
    fun selectableLines_inTree_takeNoSelectionFromAFinger() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { SelectableText(TwoLines, modifier = Tagged) } }
            longPressField()
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
        }

    @Test
    fun selectableText_inWindows_putsUpTheHandles() =
        forEachSkin { _, skin ->
            var text by mutableStateOf(Link)
            setContent { Host(skin, inTree = false) { SelectableText(text, modifier = Tagged) } }
            longPressField()
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
            text = TwoLines
            waitForIdle()
            longPressField()
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
        }

    @Test
    fun codeView_inTree_takesNoSelectionFromAFinger() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { Code() } }
            longPressField(CodeStart)
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
        }

    @Test
    fun codeView_inWindows_putsUpTheHandles() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = false) { Code() } }
            longPressField(CodeStart)
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
        }

    // b-228b
    @Test
    fun innerTextWithoutHandles_inTree_takesALongPressWithNoHandleAndShowsTheRow() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = true) { FieldFromParts() } }
            longPressField()
            onAllNodes(isPopup()).assertCountEquals(0)
            onAllNodes(isSelectionHandle()).assertCountEquals(0)
            onNodeWithText("Copy").assertExists()
        }

    @Test
    fun innerTextWithoutHandles_inWindows_letsTheHandlesUp() =
        forEachSkin { _, skin ->
            setContent { Host(skin, inTree = false) { FieldFromParts() } }
            longPressField()
            onAllNodes(isSelectionHandle()).fetchSemanticsNodes().size shouldBeGreaterThan 0
        }

    @Test
    fun modifierOff_letsTheHandlesUp_andOn_keepsThemDown() {
        for (enabled in listOf(false, true)) {
            runComposeUiTest {
                setContent {
                    var value by remember { mutableStateOf(TextFieldValue("Deep ocean")) }
                    BasicTextField(
                        value = value,
                        onValueChange = { next -> value = next },
                        modifier = Tagged.fillMaxWidth().padding(24.dp),
                        singleLine = true,
                        decorationBox = { text ->
                            Box(Modifier.withoutSelectionHandles(enabled), propagateMinConstraints = true) { text() }
                        },
                    )
                }
                longPressField()
                val handles = onAllNodes(isSelectionHandle())
                if (enabled) handles.assertCountEquals(0) else handles.fetchSemanticsNodes().size shouldBeGreaterThan 0
            }
        }
    }
}

private val Tagged = Modifier.testTag(FieldTag)

private const val Link = "https://materialkolor.com/t/AdllOwAAABALQnVybnQ"

private const val TwoLines = "val primary = Color(0xFF6750A4)\nval onPrimary = Color(0xFFFFFFFF)"

@Composable
private fun Code() {
    val file = remember { widgetGoldenColorFile() }
    CodeView(file.lines, onCopy = {}, Tagged.size(480.dp, 200.dp))
}

/**
 * A single line field the kit does not draw, built from its parts the way a preview sample field is.
 */
@Composable
private fun FieldFromParts() {
    var value by remember { mutableStateOf(TextFieldValue("Deep ocean")) }
    BasicTextField(
        value = value,
        onValueChange = { next -> value = next },
        modifier = Tagged.fillMaxWidth().padding(24.dp),
        singleLine = true,
        decorationBox = { text -> InnerTextWithoutHandles(text) },
    )
}

@Composable
private fun Select() {
    BuilderSelect("Mode", listOf("Ocean", "Forest"), "Ocean", {}, modifier = Tagged)
}

@Composable
private fun HeroField() {
    BuilderHexField(
        value = Argb(0x0B6E4F),
        onCommit = { _, _ -> },
        label = "Seed color",
        errorMessage = { reason -> reason.toString() },
        noteMessage = { notes -> notes.toString() },
        modifier = Tagged,
        large = true,
    )
}

/**
 * Foundation's selection handle, found by the semantics key its handle carries. The key is
 * foundation's own, so this only reads its name.
 */
private fun isSelectionHandle(): SemanticsMatcher =
    SemanticsMatcher("is a selection handle") { node ->
        node.config.any { (key, _) -> key.name == "SelectionHandleInfo" }
    }

/**
 * Holds a finger on the field's text, [start] in from its start edge, for a long press.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.longPressField(start: Dp = 24.dp) {
    onNodeWithTag(FieldTag).performTouchInput { longClick(Offset(start.toPx(), centerY)) }
    waitForIdle()
}

/**
 * Long presses the field's text, then slides the finger off the field before it lifts, so the lift
 * is no click on the field.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.longPressAndSlideOff(start: Dp = 24.dp) {
    onNodeWithTag(FieldTag).performTouchInput {
        down(Offset(start.toPx(), centerY))
        advanceEventTime(viewConfiguration.longPressTimeoutMillis * 2)
        move()
        moveBy(Offset(0f, height * 2f))
        up()
    }
    waitForIdle()
}

/**
 * How far in from the code view's start edge a press lands on code, past the line numbers.
 */
private val CodeStart = 160.dp

/**
 * [HostOverlays] with an empty clipboard, so no test reads the machine's own.
 */
@Composable
private fun Host(
    skin: Skin,
    inTree: Boolean,
    content: @Composable () -> Unit,
) {
    HostOverlays(skin, inTree) {
        val clipboard = remember { EmptyClipboard() }
        CompositionLocalProvider(LocalClipboard provides clipboard, content = content)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class EmptyClipboard : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {}
}
