package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test
import kotlin.test.assertNotNull

private val SheetLayout = LayoutInfo.of(widthDp = 1280.dp, heightDp = 800.dp)

/**
 * A phone, where every pressable control grows to a 48 dp footprint.
 */
private val CompactLayout = LayoutInfo.of(widthDp = 400.dp, heightDp = 800.dp)

@OptIn(ExperimentalTestApi::class)
class ControlsAContrastTest {
    @Test
    fun material3_bothModes_renderEveryControlEnabledAndDisabled() =
        runComposeUiTest { checkSheets(Skin(SkinLibrary.Material3, expressive = false)) }

    @Test
    fun material3Expressive_bothModes_renderEveryControlEnabledAndDisabled() =
        runComposeUiTest { checkSheets(Skin(SkinLibrary.Material3, expressive = true)) }

    @Test
    fun custom_bothModes_renderEveryControlEnabledAndDisabled() =
        runComposeUiTest { checkSheets(Skin(SkinLibrary.Custom, expressive = false)) }
}

/**
 * The pairs the headless actions add on top of the skin's own text pairs. Status badges put the
 * panel ink on a status fill, and the focus ring has to stand out from the panel by 3 to 1.
 */
private fun BuilderTokens.actionPairs(): List<InkPair> =
    listOf(
        InkPair("panel on success", panel, success, 4.5),
        InkPair("panel on warning", panel, warning, 4.5),
        InkPair("panel on danger", panel, danger, 4.5),
        InkPair("onAccent on accent", onAccent, accent, 4.5),
        InkPair("textStrong on panelRaised", textStrong, panelRaised, 4.5),
        InkPair("focus on panel", focus, panel, 3.0),
    )

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.checkSheets(skin: Skin) {
    val unreadable = mutableListOf<String>()
    var isDark by mutableStateOf(false)
    var compact by mutableStateOf(false)
    var tokens: BuilderTokens? = null
    setContent {
        val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
        CompositionLocalProvider(
            LocalMotionFrozen provides true,
            LocalLayout provides if (compact) CompactLayout else SheetLayout,
        ) {
            BuilderTheme(skin, result, isDark, reducedMotion = false) {
                tokens = LocalBuilderTokens.current
                if (compact) CompactSheet() else ControlSheet()
            }
        }
    }

    for (dark in listOf(false, true)) {
        isDark = dark
        waitForIdle()
        val mode = if (dark) "dark" else "light"
        unreadable += assertNotNull(tokens).actionPairs().shortfalls(mode)
    }
    compact = true
    for (dark in listOf(false, true)) {
        isDark = dark
        waitForIdle()
        // Focus Share so the compact pass draws the ring as well.
        onNodeWithText("Share").requestFocus()
        waitForIdle()
    }
    unreadable.shouldBeEmpty()
}

/**
 * Every control of the batch, enabled first and then the same controls disabled below them.
 */
@Composable
private fun ControlSheet() {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = Modifier
            .background(tokens.canvas)
            .padding(tokens.spacing.medium),
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .background(tokens.panel, RoundedCornerShape(tokens.radius.large))
                .padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        ) {
            for (enabled in listOf(true, false)) {
                SheetRow {
                    for (emphasis in Emphasis.entries) {
                        BuilderButton({}, emphasis.name, emphasis = emphasis, enabled = enabled)
                    }
                }
                SheetRow {
                    BuilderButton({}, "Export", emphasis = Emphasis.Primary, icon = IconId.Export, enabled = enabled)
                    for (emphasis in Emphasis.entries) {
                        BuilderIconButton({}, IconId.Undo, emphasis.name, emphasis = emphasis, enabled = enabled)
                    }
                }
                SheetRow {
                    BuilderToggleButton(true, {}, "Inspect", icon = IconId.Inspect, enabled = enabled)
                    BuilderToggleButton(false, {}, "Vision", icon = IconId.Vision, enabled = enabled)
                    BuilderFilterChip(true, {}, "Pinned", enabled = enabled)
                    BuilderFilterChip(false, {}, "Locked", icon = IconId.Lock, enabled = enabled)
                }
                BuilderSegmented(
                    options = listOf("Light", "Split", "Dark"),
                    selected = "Split",
                    onSelect = {},
                    label = "Preview mode",
                    enabled = enabled,
                    optionIcon = { mode -> if (mode == "Light") IconId.Sun else IconId.Moon },
                ) { it }
                BuilderChoiceChips(
                    options = ActionPaletteStyles,
                    selected = "Vibrant",
                    onSelect = {},
                    label = "Style",
                    enabled = enabled,
                ) { it }
                BuilderListRow(
                    headline = "Ocean",
                    supporting = "Edited today",
                    icon = IconId.Folder,
                    onClick = {},
                    selected = true,
                    enabled = enabled,
                )
                BuilderListRow(
                    headline = "Forest",
                    icon = IconId.Folder,
                    onClick = {},
                    selected = false,
                    enabled = enabled,
                )
                BuilderCard(onClick = {}, enabled = enabled) {
                    BuilderText("Kotlin", style = BuilderTextStyle.Title)
                    BuilderText("Compose Multiplatform theme", style = BuilderTextStyle.Body)
                }
                BuilderDivider()
            }
            SheetRow {
                for (status in BadgeStatus.entries) {
                    BuilderBadge(status.name, status = status, icon = IconId.Info.takeIf { status == BadgeStatus.Info })
                }
            }
            BuilderProgress("Exporting", progress = 0.4f)
            BuilderProgress("Generating candidates")
            BuilderCard { BuilderText("A card that only holds content") }
            Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                BuilderText("Before", style = BuilderTextStyle.Label)
                BuilderDivider(orientation = Orientation.Vertical)
                BuilderText("After", style = BuilderTextStyle.Label)
            }
        }
    }
}

/**
 * The pressable controls on a phone, the Share button focused so the sheet shows where the ring sits.
 */
@Composable
private fun CompactSheet() {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = Modifier
            .width(360.dp)
            .background(tokens.panel)
            .padding(tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        SheetRow {
            BuilderButton({}, "Share", emphasis = Emphasis.Primary)
            BuilderIconButton({}, IconId.Undo, "Undo")
            BuilderToggleButton(true, {}, "Inspect")
        }
        SheetRow {
            BuilderFilterChip(true, {}, "Pinned")
            BuilderFilterChip(false, {}, "Locked", icon = IconId.Lock)
        }
        BuilderSegmented(listOf("Light", "Split", "Dark"), "Split", onSelect = {}, label = "Preview mode") { it }
        BuilderChoiceChips(ActionPaletteStyles, "Vibrant", onSelect = {}, label = "Style") { it }
    }
}

@Composable
private fun SheetRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
