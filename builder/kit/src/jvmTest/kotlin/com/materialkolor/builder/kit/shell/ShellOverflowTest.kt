package com.materialkolor.builder.kit.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test

private const val ShellFrameHeight = 844
private val ShellFrameWidths = listOf(390, 1280)

@OptIn(ExperimentalTestApi::class)
class ShellOverflowTest {
    @Test
    fun emptyShell_everySkin_rendersAtPhoneAndDesktopWidths() {
        for ((name, expressive) in ShellFlavours) {
            for (width in ShellFrameWidths) {
                withClue("$name at $width dp") {
                    runSkikoComposeUiTest(size = Size(width.toFloat(), ShellFrameHeight.toFloat())) {
                        setContent {
                            ShellHarness(expressive) {
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
                    }
                }
            }
        }
    }
}
