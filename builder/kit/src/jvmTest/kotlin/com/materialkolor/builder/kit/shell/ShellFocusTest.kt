package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
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

                    onNodeWithTag(ShellTopBarTag, useUnmergedTree = true).requestFocus()
                    waitForIdle()
                    shellFocusedStop() shouldBe ShellTopBarTag
                    val reached = mutableListOf(ShellTopBarTag)
                    repeat(ShellTabPresses) {
                        onNodeWithTag(ShellRootTag).performKeyInput { pressKey(Key.Tab) }
                        waitForIdle()
                        val focused = shellFocusedStop()
                        if (focused != null && focused !in reached) reached += focused
                    }

                    reached shouldBe ShellReadingOrder
                }
            }
        }
    }
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
