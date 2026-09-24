package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.headless.PageTextToolbar
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.custom.CustomPaneTheme
import io.kotest.matchers.shouldBe
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.test.Test

private const val Pasted = "#6750A4"

@OptIn(ExperimentalTestApi::class)
class PageTextToolbarTest {
    @Test
    fun touchLongPressOnAFieldWithText_inTree_showsTheRow() =
        forEachSkin { _, skin ->
            setContent { ToolbarHost(skin) { BuilderTextField("Ocean", {}, "Project name") } }
            longPressField()
            onNodeWithText("Paste").assertExists()
        }

    @Test
    fun eachButton_inTree_runsItsCallbackInItsOwnClickAndLeavesTheFieldAlone() =
        forEachSkin { _, skin ->
            var commits = 0
            var toolbar: PageTextToolbar? = null
            setContent {
                ToolbarHost(skin) {
                    toolbar = LocalTextToolbar.current as? PageTextToolbar
                    Column {
                        Spacer(Modifier.height(120.dp))
                        BuilderTextField("Ocean", { commits++ }, "Project name")
                    }
                }
            }
            val field = onNode(hasSetTextAction())
            field.requestFocus()
            field.performTextReplacement("Deep ocean")
            waitForIdle()
            val calls = mutableListOf<String>()
            val bounds = field.getBoundsInRoot()
            val rect = with(density) {
                Rect(bounds.left.toPx(), bounds.top.toPx(), bounds.right.toPx(), bounds.bottom.toPx())
            }
            mainClock.autoAdvance = false
            checkNotNull(toolbar).showMenu(
                rect = rect,
                onCopyRequested = { calls += "copy" },
                onPasteRequested = { calls += "paste" },
                onCutRequested = { calls += "cut" },
                onSelectAllRequested = { calls += "select all" },
            )
            mainClock.advanceTimeByFrame()
            val buttons = listOf("Cut" to "cut", "Copy" to "copy", "Paste" to "paste", "Select all" to "select all")
            for ((label, call) in buttons) {
                tap(label)
                calls.lastOrNull() shouldBe call
            }
            calls.size shouldBe 4
            field.assertIsFocused()
            commits shouldBe 0
            mainClock.autoAdvance = true
        }

    @Test
    fun paste_inTree_putsTheClipboardTextInTheField() =
        forEachSkin { _, skin ->
            setContent { ToolbarHost(skin) { BuilderTextField("Ocean", {}, "Project name") } }
            longPressField()
            tap("Paste")
            val pasted = hasSetTextAction() and hasText(Pasted, substring = true)
            waitUntil { onAllNodes(pasted).fetchSemanticsNodes().isNotEmpty() }
            waitForIdle()
            onAllNodes(hasText("Paste")).assertCountEquals(0)
        }

    @Test
    fun row_inTree_goesWhenTheSelectionCollapsesOrTheFieldBlurs() =
        forEachSkin { _, skin ->
            setContent {
                ToolbarHost(skin) {
                    Column {
                        BuilderTextField("Ocean", {}, "Project name")
                        OverlayTestButton("other")
                    }
                }
            }
            longPressField()
            onNodeWithText("Paste").assertExists()
            onNode(hasSetTextAction()).performTextInputSelection(TextRange(2))
            waitForIdle()
            onAllNodes(hasText("Paste")).assertCountEquals(0)

            longPressField()
            onNodeWithText("Paste").assertExists()
            onNodeWithTag("other").requestFocus()
            waitForIdle()
            onAllNodes(hasText("Paste")).assertCountEquals(0)
        }

    @Test
    fun fieldInAnOpenDialog_inTree_getsTheRowOverTheDialog() =
        forEachSkin { _, skin ->
            setContent {
                ToolbarHost(skin) {
                    BuilderDialog(visible = true, onDismissRequest = {}, title = "Rename") {
                        BuilderTextField("Deep ocean", {}, "Project name")
                    }
                }
            }
            waitForIdle()
            longPressField()
            tap("Select all")
            waitForIdle()
            onNode(hasSetTextAction()).assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, 10)),
            )
        }

    @Test
    fun fieldInACustomPane_inTree_getsTheRowAtTheField() =
        forEachSkin { _, skin ->
            val slots = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))).customSlots
            setContent {
                ToolbarHost(skin) {
                    Box(Modifier.fillMaxSize().padding(start = 160.dp, top = 240.dp)) {
                        Box(
                            Modifier
                                .size(width = 360.dp, height = 200.dp)
                                .graphicsLayer {
                                    scaleX = 0.75f
                                    scaleY = 0.75f
                                },
                        ) {
                            CustomPaneTheme(slots, isDark = false, reducedMotion = false) {
                                Box(Modifier.fillMaxSize()) { BuilderTextField("Ocean", {}, "Project name") }
                            }
                        }
                    }
                }
            }
            longPressField()
            val field = onNode(hasSetTextAction()).getBoundsInRoot()
            val paste = onNodeWithText("Paste").assertExists().getBoundsInRoot()
            (paste.right > field.left && paste.left < field.right) shouldBe true
            (paste.bottom > field.top - 120.dp && paste.top < field.bottom + 120.dp) shouldBe true
        }

    @Test
    fun longPress_inWindows_leavesTheToolbarToThePlatform() =
        forEachSkin { _, skin ->
            var toolbar: TextToolbar? = null
            setContent {
                ToolbarHost(skin, inTree = false) {
                    toolbar = LocalTextToolbar.current
                    BuilderTextField("Ocean", {}, "Project name")
                }
            }
            longPressField()
            (toolbar is PageTextToolbar) shouldBe false
            onAllNodes(hasText("Paste")).assertCountEquals(0)
            onAllNodes(hasText("Select all")).assertCountEquals(0)
        }
}

/** Where a press lands on the field's text, past the start padding of every skin. */
private val TextStart = 24.dp

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.longPressField() {
    onNode(hasSetTextAction()).performTouchInput { longClick(Offset(TextStart.toPx(), centerY)) }
    waitForIdle()
}

/** Taps a button of the row with a finger, the only way the row is ever pressed. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tap(label: String) {
    onNodeWithText(label).performTouchInput { click() }
}

/** [HostOverlays] with a clipboard holding [Pasted], so no test reads the machine's own. */
@Composable
private fun ToolbarHost(
    skin: Skin,
    inTree: Boolean = true,
    content: @Composable () -> Unit,
) {
    HostOverlays(skin, inTree) {
        val clipboard = remember { FakeClipboard(Pasted) }
        CompositionLocalProvider(LocalClipboard provides clipboard, content = content)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class FakeClipboard(
    var text: String?,
) : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = text?.let { ClipEntry(StringSelection(it)) }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        text = clipEntry?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor) as? String
    }
}
