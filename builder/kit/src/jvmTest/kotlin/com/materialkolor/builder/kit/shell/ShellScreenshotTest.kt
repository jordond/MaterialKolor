package com.materialkolor.builder.kit.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on, and baselines are only ever recorded on the Linux runner.
 */
private const val ShellScreenshotDir = "src/jvmTest/screenshots/shell"
private const val ShellScreenshotHeight = 844
private val ShellScreenshotWidths = listOf(390, 1280)

@OptIn(ExperimentalTestApi::class)
class ShellScreenshotTest {
    @Test
    fun emptyShell_everySkin_rendersAtPhoneAndDesktopWidths() {
        for ((name, skin) in ShellSkins) {
            for (width in ShellScreenshotWidths) {
                withClue("$name at $width dp") {
                    runSkikoComposeUiTest(size = Size(width.toFloat(), ShellScreenshotHeight.toFloat())) {
                        setContent {
                            ShellHarness(skin) {
                                WorkspaceShell(
                                    posterColors = ShellPosterColors,
                                    posterCollapsed = false,
                                    poster = {},
                                    topBar = { TopBarRegion(Modifier.testTag(ShellTopBarTag)) {} },
                                    canvas = {},
                                    dock = { DockRegion(Modifier.testTag(ShellDockTag)) {} },
                                )
                            }
                        }
                        waitForIdle()

                        val overflow = listOf(ShellTopBarTag, ShellDockTag).filter { tag ->
                            val bounds = shellBounds(tag)
                            bounds.left < 0.dp || bounds.right > width.dp
                        }
                        overflow.shouldBeEmpty()
                        onNodeWithTag(ShellRootTag).captureRoboImage("$ShellScreenshotDir/$name-$width.png")
                    }
                }
            }
        }
    }
}
