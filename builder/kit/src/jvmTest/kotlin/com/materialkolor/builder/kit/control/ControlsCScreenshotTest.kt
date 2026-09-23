package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on, and baselines are only ever recorded on the Linux runner.
 */
private const val ScreenshotDir = "src/jvmTest/screenshots/controls-c"
private const val SceneTag = "overlay-scene"
private const val TooltipAnchorTag = "tooltip-anchor"

/** Which overlay the scene has open on top of the page. */
private enum class Overlay {
    Menu,
    Dialog,
    SidePanel,
    Sheet,
    Page,
}

@OptIn(ExperimentalTestApi::class)
class ControlsCScreenshotTest {
    @Test
    fun overlays_everySkinBothModes_renderReadably() = forEachSkin { name, skin -> captureOverlays(name, skin) }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.captureOverlays(
    name: String,
    skin: Skin,
) {
    val unreadable = mutableListOf<String>()
    var isDark by mutableStateOf(false)
    var overlay by mutableStateOf(Overlay.Menu)
    var seen: Pair<OverlayStyle, BuilderTokens>? = null
    val toasts = BuilderToastHostState()
    toasts.show("Theme saved", duration = ToastDuration.Indefinite)
    toasts.show("Pin removed", actionLabel = "Undo", duration = ToastDuration.Indefinite) {}
    setContent {
        ControlsHarness(skin, isDark) {
            seen = overlayStyle(LocalSkin.current.library) to LocalBuilderTokens.current
            OverlayScene(overlay, toasts)
        }
    }

    for (dark in listOf(false, true)) {
        isDark = dark
        val mode = if (dark) "dark" else "light"
        for (step in Overlay.entries) {
            overlay = step
            waitForIdle()
            if (step == Overlay.Page) {
                onNodeWithTag(TooltipAnchorTag, useUnmergedTree = true).requestFocus()
                waitForIdle()
            }
            onNodeWithTag(SceneTag).captureRoboImage("$ScreenshotDir/$name-$mode-${step.name.lowercase()}.png")
        }
        val (style, tokens) = assertNotNull(seen)
        unreadable += style.inkPairs(tokens).shortfalls(mode)
    }
    unreadable.shouldBeEmpty()
}

/** Every control of the batch, with [overlay] open over the page. */
@Composable
private fun OverlayScene(
    overlay: Overlay,
    toasts: BuilderToastHostState,
) {
    val tokens = LocalBuilderTokens.current
    Box(Modifier.testTag(SceneTag).fillMaxSize().background(tokens.canvas)) {
        Row(
            modifier = Modifier.padding(tokens.spacing.large),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large),
        ) {
            Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                BuilderSelect("Style", listOf("Tonal spot", "Vibrant"), "Tonal spot", {})
                BuilderSelect("Spec", listOf("2021"), "2021", {}, enabled = false)
                BuilderMenu(
                    expanded = overlay == Overlay.Menu,
                    onDismissRequest = {},
                    items = listOf(
                        BuilderMenuItem("Duplicate", {}, IconId.Copy),
                        BuilderMenuItem("Archive", {}, IconId.Folder, enabled = false),
                        BuilderMenuItem("Delete", {}, IconId.Trash, Emphasis.Danger),
                    ),
                ) { BuilderText("Project menu", style = BuilderTextStyle.Label) }
                BuilderTooltip("Undo last edit") {
                    Box(
                        modifier = Modifier.testTag(TooltipAnchorTag).size(40.dp).focusable(),
                        contentAlignment = Alignment.Center,
                    ) { BuilderIcon(IconId.Undo, contentDescription = "Undo") }
                }
                BuilderScrollArea(Modifier.height(160.dp).background(tokens.panel)) {
                    repeat(20) { index -> BuilderText("Row $index") }
                }
            }
            Box(Modifier.width(360.dp).fillMaxHeight()) {
                BuilderBottomSheet(rememberBottomSheetState(BottomSheetDetent.Half), label = "Poster") {
                    BuilderText("#6750A4", Modifier.padding(tokens.spacing.large), style = BuilderTextStyle.PosterHero)
                }
            }
        }
        BuilderToastHost(toasts)
        BuilderDialog(
            visible = overlay == Overlay.Dialog,
            onDismissRequest = {},
            title = "Delete project",
            actions = {
                BuilderText("Cancel", style = BuilderTextStyle.Label)
                BuilderText("Delete", style = BuilderTextStyle.Label, emphasis = Emphasis.Danger)
            },
        ) { BuilderText("This removes the project from this browser.") }
        BuilderSidePanel(overlay == Overlay.SidePanel, {}, "Projects") { BuilderText("Sunset") }
        BuilderSheet(overlay == Overlay.Sheet, {}, "Export", SheetPresentation.of(LocalLayout.current)) {
            BuilderText("Kotlin")
        }
    }
}

/**
 * Every ink the overlays draw on every ground they draw it on. Text keeps to WCAG AA at 4.5, the
 * focus ring and the scrollbar thumb are marks and need 3.
 */
private fun OverlayStyle.inkPairs(tokens: BuilderTokens): List<InkPair> =
    listOf(
        InkPair("content on surface", content, surface, 4.5),
        InkPair("muted on surface", muted, surface, 4.5),
        InkPair("danger on surface", tokens.danger, surface, 4.5),
        InkPair("content on highlight", content, highlight.compositeOver(surface), 4.5),
        InkPair("content on selected", content, selected.compositeOver(surface), 4.5),
        InkPair("muted on field", muted, field, 4.5),
        InkPair("content on field", content, field, 4.5),
        InkPair("tooltip ink", tooltipContent, tooltip, 4.5),
        InkPair("toast ink", toastContent, toast, 4.5),
        InkPair("focus on surface", focus, surface, 3.0),
        InkPair("thumb on surface", thumb, surface, 3.0),
    )
