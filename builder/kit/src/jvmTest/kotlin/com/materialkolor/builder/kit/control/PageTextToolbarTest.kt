package com.materialkolor.builder.kit.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
            mainClock.advanceTimeBy(doubleTapTimeout() + 100)
            onNode(hasSetTextAction()).performTouchInput { click(Offset(TextStart.toPx() + 8.dp.toPx(), centerY)) }
            waitForIdle()
            onNode(hasSetTextAction()).assert(hasCollapsedSelection())
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
    fun fieldInACustomPane_inTree_underAHostOffsetFromTheRoot_getsTheRowAtTheField() =
        forEachSkin { _, skin ->
            val slots = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))).customSlots
            setContent {
                Box(Modifier.fillMaxSize().padding(start = HostOffset, top = HostOffset)) {
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
            }
            longPressField()
            val field = onNode(hasSetTextAction()).getBoundsInRoot()
            val paste = onNodeWithText("Paste").assertExists().getBoundsInRoot()
            (paste.right > field.left && paste.left < field.right) shouldBe true
            // Above the selection inside the field. Left in the root's coordinates it would sit a
            // whole host offset lower, under the field.
            (paste.bottom <= field.bottom && paste.bottom > field.top - HostOffset / 2) shouldBe true
        }

    @Test
    fun pageButtonBesideTheShownRow_inTree_stillClicks() =
        forEachSkin { _, skin ->
            var clicks = 0
            var toolbar: PageTextToolbar? = null
            setContent {
                ToolbarHost(skin) {
                    toolbar = LocalTextToolbar.current as? PageTextToolbar
                    Box(
                        Modifier
                            .testTag("page")
                            .fillMaxWidth()
                            .height(240.dp)
                            .clickable(interactionSource = null, indication = null) { clicks++ },
                    )
                }
            }
            waitForIdle()
            val page = onNodeWithTag("page").getBoundsInRoot()
            val rect = with(density) {
                val middle = (page.left + page.right).toPx() / 2
                Rect(middle - 20.dp.toPx(), 160.dp.toPx(), middle + 20.dp.toPx(), 180.dp.toPx())
            }
            val calls = mutableListOf<String>()
            runOnIdle { checkNotNull(toolbar).showMenu(rect, { calls += "copy" }, { calls += "paste" }, null, null) }
            waitForIdle()
            val paste = onNodeWithText("Paste").assertExists().getBoundsInRoot()
            (paste.left > 16.dp) shouldBe true
            val beside = with(density) { Offset(8.dp.toPx(), (paste.top + paste.bottom).toPx() / 2) }
            onNodeWithTag("page").performTouchInput { click(beside) }
            waitForIdle()
            clicks shouldBe 1
            calls shouldBe emptyList()
            onNodeWithText("Paste").assertExists()
        }

    @Test
    fun fieldScrolledOutOfView_inTree_takesTheRowAwayAndItDoesNotComeBackPinned() =
        forEachSkin { _, skin ->
            val scroll = ScrollState(0)
            setContent {
                ToolbarHost(skin) {
                    Column(Modifier.testTag("scroll").fillMaxSize().verticalScroll(scroll)) {
                        Spacer(Modifier.height(120.dp))
                        BuilderTextField("Ocean", {}, "Project name")
                        Spacer(Modifier.height(2000.dp))
                    }
                }
            }
            longPressField()
            onNodeWithText("Paste").assertExists()
            val far = with(density) { 800.dp.toPx() }

            // A scroll the page runs itself never reaches the host, so the field shows its menu
            // again at a rect that has left the host.
            runOnIdle { scroll.dispatchRawDelta(far) }
            waitForIdle()
            onAllNodes(hasText("Paste")).assertCountEquals(0)
            runOnIdle { scroll.dispatchRawDelta(-far) }
            waitForIdle()
            onNodeWithText("Paste").assertExists()

            // A finger's scroll hides the row at its first step, the field still in view.
            mainClock.advanceTimeBy(doubleTapTimeout() + 100)
            onNodeWithTag("scroll").performTouchInput {
                down(Offset(centerX, bottom - 40.dp.toPx()))
                moveBy(Offset(0f, -viewConfiguration.touchSlop - 24.dp.toPx()))
            }
            waitForIdle()
            onAllNodes(hasText("Paste")).assertCountEquals(0)
            onNodeWithTag("scroll").performTouchInput {
                moveBy(Offset(0f, -600.dp.toPx()))
                up()
            }
            waitForIdle()
            (scroll.value > 0) shouldBe true
            onAllNodes(hasText("Paste")).assertCountEquals(0)
            onAllNodes(hasText("Copy")).assertCountEquals(0)
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

/**
 * Where a press lands on the field's text, past the start padding of every skin.
 */
private val TextStart = 24.dp

/**
 * How far the pane test moves the root host from the root's origin.
 */
private val HostOffset = 160.dp

/**
 * How long after a press on the field lets go a second press still counts as a double click.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.doubleTapTimeout(): Long =
    onNode(hasSetTextAction())
        .fetchSemanticsNode()
        .layoutInfo.viewConfiguration.doubleTapTimeoutMillis

/**
 * A text field whose selection is a caret.
 */
private fun hasCollapsedSelection(): SemanticsMatcher =
    SemanticsMatcher("has a collapsed selection") { node ->
        node.config.getOrNull(SemanticsProperties.TextSelectionRange)?.collapsed == true
    }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.longPressField() {
    onNode(hasSetTextAction()).performTouchInput { longClick(Offset(TextStart.toPx(), centerY)) }
    waitForIdle()
}

/**
 * Taps a button of the row with a finger, the only way the row is ever pressed.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tap(label: String) {
    onNodeWithText(label).performTouchInput { click() }
}

/**
 * [HostOverlays] with a clipboard holding [Pasted], so no test reads the machine's own.
 */
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
