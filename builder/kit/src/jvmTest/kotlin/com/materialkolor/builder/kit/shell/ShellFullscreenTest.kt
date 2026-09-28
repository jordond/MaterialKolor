package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val FullscreenExitTag = "shell-fullscreen-exit"
private const val FullscreenHeight = 900

@OptIn(ExperimentalTestApi::class)
class ShellFullscreenTest {
    @Test
    fun fullscreen_everyPosterMode_hidesThePosterAndTopBarAndKeepsTheDockAndTheExitClearOfTheCanvas() {
        val windows = listOf(390 to PosterMode.Sheet, 600 to PosterMode.Rail72, 1280 to PosterMode.Docked400)
        for ((width, expected) in windows) {
            withClue("$width dp") {
                runSkikoComposeUiTest(size = Size(width.toFloat(), FullscreenHeight.toFloat())) {
                    var mode: PosterMode? = null
                    setContent {
                        ShellHarness {
                            mode = LocalLayout.current.posterMode
                            FullscreenShell(fullscreen = true)
                        }
                    }
                    waitForIdle()

                    mode shouldBe expected
                    onNodeWithTag(ShellPosterTag, useUnmergedTree = true).assertDoesNotExist()
                    onNodeWithTag(ShellTopBarTag, useUnmergedTree = true).assertDoesNotExist()
                    val canvas = shellBounds(ShellCanvasTag)
                    val exit = shellBounds(FullscreenExitTag)
                    val dock = shellBounds(ShellDockTag)
                    exit.top shouldBeLessThan 16.dp
                    exit.bottom shouldBeLessThanOrEqualTo canvas.top
                    canvas.top shouldBeLessThan exit.bottom + 16.dp
                    canvas.left shouldBeLessThan 16.dp
                    canvas.right shouldBe width.dp - 12.dp
                    exit.right shouldBe canvas.right
                    if (expected == PosterMode.Sheet) {
                        // A phone floats the dock over the canvas.
                        dock.bottom shouldBeLessThan canvas.bottom
                    } else {
                        // Everywhere else it sits under the canvas and covers none of it.
                        dock.top shouldBeGreaterThanOrEqualTo canvas.bottom
                        dock.bottom shouldBeLessThan FullscreenHeight.dp
                    }
                }
            }
        }
    }

    @Test
    fun fullscreen_turnedOnAndOff_keepsTheCanvasAndBringsThePosterBack() =
        runSkikoComposeUiTest(size = Size(1280f, FullscreenHeight.toFloat())) {
            var fullscreen by mutableStateOf(false)
            val canvases = mutableListOf<Any>()
            setContent {
                ShellHarness {
                    FullscreenShell(fullscreen = fullscreen, onCanvas = { token -> canvases += token })
                }
            }
            waitForIdle()
            onNodeWithTag(FullscreenExitTag).assertDoesNotExist()

            fullscreen = true
            waitForIdle()
            onNodeWithTag(ShellTopBarTag, useUnmergedTree = true).assertDoesNotExist()
            onNodeWithTag(FullscreenExitTag).assertExists()

            fullscreen = false
            waitForIdle()
            onNodeWithTag(ShellTopBarTag, useUnmergedTree = true).assertExists()
            onNodeWithTag(ShellPosterTag, useUnmergedTree = true).assertExists()
            onNodeWithTag(FullscreenExitTag).assertDoesNotExist()
            canvases.distinct() shouldContainExactly listOf(canvases.first())
        }
}

/**
 * The shell with tagged slots, telling [onCanvas] what its canvas remembered each time it composes.
 */
@Composable
private fun FullscreenShell(
    fullscreen: Boolean,
    onCanvas: (Any) -> Unit = {},
) {
    WorkspaceShell(
        posterColors = ShellPosterColors,
        posterCollapsed = false,
        poster = { ShellSlot(ShellPosterTag) },
        topBar = { TopBarRegion(Modifier.testTag(ShellTopBarTag)) {} },
        canvas = {
            onCanvas(remember { Any() })
            ShellSlot(ShellCanvasTag)
        },
        dock = { Box(Modifier.size(240.dp, 64.dp).testTag(ShellDockTag)) },
        fullscreen = fullscreen,
        fullscreenExit = { Box(Modifier.size(120.dp, 40.dp).testTag(FullscreenExitTag)) },
    )
}
