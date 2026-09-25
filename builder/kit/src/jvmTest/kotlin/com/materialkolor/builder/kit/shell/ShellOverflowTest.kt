package com.materialkolor.builder.kit.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.ToastDuration
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test

private const val ShellFrameHeight = 844
private val ShellFrameWidths = listOf(390, 1280)

@OptIn(ExperimentalTestApi::class)
class ShellOverflowTest {
    @Test
    fun emptyShell_everySkin_rendersAtPhoneAndDesktopWidths() {
        for ((name, skin) in ShellSkins) {
            for (width in ShellFrameWidths) {
                withClue("$name at $width dp") {
                    runSkikoComposeUiTest(size = Size(width.toFloat(), ShellFrameHeight.toFloat())) {
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
                    }
                }
            }
        }
    }

    /**
     * Fluent's own regions with something in them, the header's command bar with the library
     * switcher, the dock's command bar on its `Layer` and a toast as an `InfoBar`, in light and dark.
     */
    @Test
    fun fluentShell_lightAndDark_rendersItsRegionsAtPhoneAndDesktopWidths() {
        for (isDark in listOf(false, true)) {
            for (width in ShellFrameWidths) {
                val mode = if (isDark) "dark" else "light"
                withClue("$mode at $width dp") {
                    runSkikoComposeUiTest(size = Size(width.toFloat(), ShellFrameHeight.toFloat())) {
                        val toasts = BuilderToastHostState()
                        toasts.show("Theme saved", actionLabel = "Undo", duration = ToastDuration.Indefinite) {}
                        setContent {
                            ShellHarness(Skin(Library.Fluent, expressive = false), isDark = isDark) {
                                FluentShell(toasts)
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

private val ShellLibraries = listOf("Material 3", "Unstyled", "Fluent", "Custom")

@Composable
private fun FluentShell(toasts: BuilderToastHostState) {
    WorkspaceShell(
        posterColors = ShellPosterColors,
        posterCollapsed = false,
        poster = {},
        topBar = {
            TopBarRegion(Modifier.testTag(ShellTopBarTag)) {
                BuilderSegmented(
                    options = ShellLibraries,
                    selected = "Fluent",
                    onSelect = {},
                    label = "Library",
                    modifier = Modifier.weight(1f),
                    optionLabel = { library -> library },
                )
                BuilderButton(onClick = {}, label = "Export", emphasis = Emphasis.Primary)
            }
        },
        canvas = {},
        dock = {
            DockRegion(Modifier.testTag(ShellDockTag)) {
                BuilderToggleButton(checked = true, onCheckedChange = {}, label = "Inspect")
                BuilderButton(onClick = {}, label = "Fullscreen", emphasis = Emphasis.Subtle)
            }
        },
        overlays = { ToastRegion(toasts) },
    )
}
