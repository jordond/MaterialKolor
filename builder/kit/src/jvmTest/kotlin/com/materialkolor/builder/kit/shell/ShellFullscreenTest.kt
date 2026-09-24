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
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val FullscreenExitTag = "shell-fullscreen-exit"
private const val FullscreenHeight = 900
private val FullscreenSkin = Skin(Library.Material3, expressive = false)

@OptIn(ExperimentalTestApi::class)
class ShellFullscreenTest {
    @Test
    fun fullscreen_everyPosterMode_hidesThePosterAndTopBarAndKeepsTheDockAndTheExit() {
        val windows = listOf(390 to PosterMode.Sheet, 600 to PosterMode.Rail72, 1280 to PosterMode.Docked400)
        for ((width, expected) in windows) {
            withClue("$width dp") {
                runSkikoComposeUiTest(size = Size(width.toFloat(), FullscreenHeight.toFloat())) {
                    var mode: PosterMode? = null
                    setContent {
                        ShellHarness(FullscreenSkin) {
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
                    canvas.top shouldBeLessThan 16.dp
                    canvas.left shouldBeLessThan 16.dp
                    canvas.right shouldBe width.dp - 12.dp
                    exit.top shouldBeLessThan dock.top
                    exit.right shouldBeLessThan canvas.right
                    dock.bottom shouldBeLessThan canvas.bottom
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
                ShellHarness(FullscreenSkin) {
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

/** The shell with tagged slots, telling [onCanvas] what its canvas remembered each time it composes. */
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
