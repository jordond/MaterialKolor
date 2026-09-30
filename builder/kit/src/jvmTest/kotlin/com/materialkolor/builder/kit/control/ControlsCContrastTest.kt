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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
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
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlin.test.Test

private const val TooltipAnchorTag = "tooltip-anchor"

/**
 * Which overlay the scene has open on top of the page.
 */
private enum class Overlay {
    Menu,
    Dialog,
    SidePanel,
    Sheet,
    Page,
}

@OptIn(ExperimentalTestApi::class)
class ControlsCContrastTest {
    @Test
    fun overlayTokens_everySkinBothModes_meetTheirContrastMinimums() {
        readInEverySkin { overlayStyle(LocalSkin.current.library).inkPairs(LocalBuilderTokens.current) }
            .shortfalls()
            .shouldBeEmpty()
    }

    @Test
    fun overlays_everySkin_openEachInTurnOverThePage() = forEachSkin { _, skin -> openOverlays(skin) }
}

/**
 * What each overlay shows once it is open.
 */
private val OverlayContent: Map<Overlay, String> = mapOf(
    Overlay.Menu to "Duplicate",
    Overlay.Dialog to "This removes the project from this browser.",
    Overlay.SidePanel to "Sunset",
    Overlay.Sheet to "Kotlin",
)

/**
 * Opens each overlay in turn over the page and checks it shows its content, then closes them all
 * and focuses the tooltip's anchor.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.openOverlays(skin: Skin) {
    var overlay by mutableStateOf(Overlay.Menu)
    val toasts = BuilderToastHostState()
    toasts.show("Theme saved", duration = ToastDuration.Indefinite)
    toasts.show("Pin removed", actionLabel = "Undo", duration = ToastDuration.Indefinite) {}
    setContent { ControlsHarness(skin) { OverlayScene(overlay, toasts) } }

    for (step in Overlay.entries) {
        overlay = step
        waitForIdle()
        val content = OverlayContent[step] ?: continue
        withClue(step) { onAllNodes(hasText(content), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty() }
    }
    onNodeWithTag(TooltipAnchorTag, useUnmergedTree = true).requestFocus()
    onNodeWithTag(TooltipAnchorTag, useUnmergedTree = true).assertIsFocused()
}

/**
 * Every control of the batch, with [overlay] open over the page.
 */
@Composable
private fun OverlayScene(
    overlay: Overlay,
    toasts: BuilderToastHostState,
) {
    val tokens = LocalBuilderTokens.current
    Box(Modifier.fillMaxSize().background(tokens.canvas)) {
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
                TestBottomSheet(rememberBottomSheetState(BottomSheetDetent.Half), label = "Poster") {
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
        InkPair("focus on surface", focus, surface, 3.0),
        InkPair("thumb on surface", thumb, surface, 3.0),
    )
