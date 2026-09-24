package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.BottomSheetState
import com.materialkolor.builder.kit.control.rememberBottomSheetState
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test

/** The regions in the order AR-01 wants Tab to reach them. */
private val ShellReadingOrder = listOf(ShellTopBarTag, ShellPosterTag, ShellCanvasTag, ShellDockTag)

/** Enough presses to pass every stop the skins' own regions add, such as the sheet's handle. */
private const val ShellTabPresses = 12

@OptIn(ExperimentalTestApi::class)
class ShellFocusTest {
    @Test
    fun tab_compactMediumAndExpanded_walksTopBarPosterCanvasThenDock() {
        // The narrow Medium case keeps its rail shut, the way it starts.
        val windows = listOf(390 to false, 600 to true, 840 to false, 1280 to false)
        for ((width, collapsed) in windows) {
            withClue("$width dp") {
                runSkikoComposeUiTest(size = Size(width.toFloat(), 900f)) {
                    setContent {
                        ShellHarness(Skin(Library.Material3, expressive = false)) {
                            WorkspaceShell(
                                posterColors = ShellPosterColors,
                                posterCollapsed = collapsed,
                                poster = { ShellStop(ShellPosterTag) },
                                topBar = { TopBarRegion { ShellStop(ShellTopBarTag) } },
                                canvas = { ShellStop(ShellCanvasTag) },
                                dock = { DockRegion { ShellStop(ShellDockTag) } },
                            )
                        }
                    }
                    waitForIdle()

                    shellTabFrom(ShellTopBarTag) shouldBe ShellReadingOrder
                }
            }
        }
    }

    @Test
    fun tab_posterSheetAtHalfAndFull_skipsWhatTheSheetCoversUntilItSinksToPeek() {
        // A phone held upright, and one on its side with the short peek (D38).
        val windows = listOf(Triple(412, 900, false), Triple(844, 390, true))
        // Tab from the poster, in the order it reaches each stop the first time.
        val reachable = mapOf(
            BottomSheetDetent.Peek to listOf(ShellPosterTag, ShellCanvasTag, ShellDockTag, ShellTopBarTag),
            BottomSheetDetent.Half to listOf(ShellPosterTag, ShellTopBarTag),
            BottomSheetDetent.Full to listOf(ShellPosterTag),
        )
        for ((width, height, coarse) in windows) {
            withClue("${width}x$height dp") {
                runSkikoComposeUiTest(size = Size(width.toFloat(), height.toFloat())) {
                    lateinit var sheet: BottomSheetState
                    lateinit var scope: CoroutineScope
                    setContent {
                        ShellHarness(Skin(Library.Material3, expressive = false), coarsePointer = coarse) {
                            sheet = rememberBottomSheetState()
                            scope = rememberCoroutineScope()
                            WorkspaceShell(
                                posterColors = ShellPosterColors,
                                posterCollapsed = false,
                                poster = { ShellStop(ShellPosterTag) },
                                topBar = { TopBarRegion { ShellStop(ShellTopBarTag) } },
                                canvas = { ShellStop(ShellCanvasTag) },
                                dock = { DockRegion { ShellStop(ShellDockTag) } },
                                sheetState = sheet,
                            )
                        }
                    }
                    waitForIdle()

                    val detents = listOf(
                        BottomSheetDetent.Peek,
                        BottomSheetDetent.Half,
                        BottomSheetDetent.Full,
                        BottomSheetDetent.Peek,
                    )
                    for (detent in detents) {
                        withClue(detent) {
                            scope.launch { sheet.snapTo(detent) }
                            waitForIdle()
                            shellTabFrom(ShellPosterTag) shouldBe reachable.getValue(detent)
                            onNodeWithTag(ShellCanvasTag, useUnmergedTree = true).assertExists()
                            onNodeWithTag(ShellDockTag, useUnmergedTree = true).assertExists()
                        }
                    }
                }
            }
        }
    }
}

/** Focuses [start] and presses Tab, and gives the stops in the order it first reached each. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.shellTabFrom(start: String): List<String> {
    onNodeWithTag(start, useUnmergedTree = true).requestFocus()
    waitForIdle()
    shellFocusedStop() shouldBe start
    val reached = mutableListOf(start)
    repeat(ShellTabPresses) {
        onNodeWithTag(ShellRootTag).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        val focused = shellFocusedStop()
        if (focused != null && focused !in reached) reached += focused
    }
    return reached
}

/** A focus stop filling a slot's corner. */
@Composable
private fun ShellStop(tag: String) {
    Box(Modifier.size(40.dp).testTag(tag).focusable())
}

/** Which of the four stops holds focus, if any. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.shellFocusedStop(): String? =
    ShellReadingOrder.firstOrNull { tag ->
        val node = onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        node.config.getOrNull(SemanticsProperties.Focused) == true
    }
