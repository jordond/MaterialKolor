package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.canvas_mode_dark
import com.materialkolor.builder.generated.resources.canvas_mode_light
import com.materialkolor.builder.generated.resources.canvas_tab_app
import com.materialkolor.builder.generated.resources.canvas_tab_components
import com.materialkolor.builder.generated.resources.canvas_tab_contrast
import com.materialkolor.builder.generated.resources.canvas_tab_palettes
import com.materialkolor.builder.generated.resources.canvas_tab_roles
import com.materialkolor.builder.kit.control.BuilderTabs
import com.materialkolor.builder.kit.widget.screenWidth
import com.materialkolor.builder.preview.canvas.AppTab
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitPreview
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The smallest an app screen shrinks to fit the canvas before the canvas scrolls it instead. */
internal const val MIN_SCREEN_SCALE = 0.6f

/** Tags the scrolling frame each app screen sits in, for tests to read its scroll range. */
internal const val DEVICE_SCREEN_TAG: String = "canvas-device-screen"

/**
 * Told the tab each time the canvas composes that tab's body, from inside the body, or null, which
 * it always is outside tests. Tests provide it to prove only the visible tab composes.
 */
internal val LocalCanvasProbe: ProvidableCompositionLocal<((tab: PreviewTab) -> Unit)?> =
    staticCompositionLocalOf { null }

// pf-1

/**
 * Told the name of each swatch or ramp a data tab composes, each time it composes it, or null, which
 * it always is outside tests. Tests provide it to prove an edit composes again only what it changed.
 */
internal val LocalTileProbe: ProvidableCompositionLocal<((name: String) -> Unit)?> =
    staticCompositionLocalOf { null }

/**
 * The light and dark panes of the canvas, drawn through the same vision filter.
 *
 * @property[light] The pane wearing the light scheme, the start copy of a split.
 * @property[dark] The pane wearing the dark scheme.
 * @property[filter] The vision filter both wear, or null for none.
 */
@Immutable
internal class PaneSpecs(
    val light: PaneSpec,
    val dark: PaneSpec,
    val filter: ColorMatrix?,
)

/** The panes for the frame's theme result seen through [vision]. Each gets a fresh matrix. */
@Composable
internal fun rememberPaneSpecs(vision: VisionSimulation): PaneSpecs {
    val result = LocalThemeResult.current
    val light = stringResource(Res.string.canvas_mode_light)
    val dark = stringResource(Res.string.canvas_mode_dark)
    return remember(result, vision, light, dark) {
        PaneSpecs(
            light = PaneSpec(result, isDark = false, label = light, filter = vision.matrix()),
            dark = PaneSpec(result, isDark = true, label = dark, filter = vision.matrix()),
            filter = vision.matrix(),
        )
    }
}

/**
 * The canvas tabs App, Components, Roles, Palettes and Contrast. A click or the arrow keys pick one.
 */
@Composable
internal fun CanvasTabs(
    selected: PreviewTab,
    onSelect: (PreviewTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val names = PreviewTab.entries.associateWith { tab -> stringResource(tab.title) }
    BuilderTabs(
        tabs = PreviewTab.entries,
        selected = selected,
        onSelect = onSelect,
        label = { tab -> names.getValue(tab) },
        modifier = modifier,
    )
}

/**
 * The body of the visible [tab], the only one that composes.
 *
 * App and Components wipe light over dark with the split handle, or show one copy in Light and
 * Dark. The data tabs lay their own light and dark columns out for [mode] and send what they are
 * asked to do through [dispatcher]. Palettes picks out [rampHighlight] while it belongs to the
 * project [generation] counts.
 */
@Composable
internal fun CanvasTabBody(
    tab: PreviewTab,
    mode: PreviewMode,
    preview: PreviewSplit,
    specs: PaneSpecs,
    appState: DemoAppState,
    componentsState: DemoAppState,
    deviceWidth: DeviceWidth,
    dispatcher: Dispatcher<WorkspaceAction>,
    rampHighlight: RampHighlight?,
    generation: Int,
    modifier: Modifier = Modifier,
) {
    val probe = LocalCanvasProbe.current
    val result = specs.light.result
    when (tab) {
        PreviewTab.App -> {
            probe?.invoke(PreviewTab.App)
            // One scroll for both copies, so a screen wider than the canvas scrolls as one.
            val scroll = rememberScrollState()
            PreviewCopies(preview, specs, modifier) { spec ->
                DeviceScreen(deviceWidth, scroll) { AppTab(spec, appState, deviceWidth) }
            }
        }
        PreviewTab.Components -> {
            probe?.invoke(PreviewTab.Components)
            PreviewCopies(preview, specs, modifier) { spec -> ComponentsTab(spec, componentsState) }
        }
        PreviewTab.Roles -> {
            probe?.invoke(PreviewTab.Roles)
            RolesTab(result, mode, specs.filter, dispatcher, modifier)
        }
        PreviewTab.Palettes -> {
            probe?.invoke(PreviewTab.Palettes)
            PalettesTab(result, mode, specs.filter, rampHighlight, generation, dispatcher, modifier)
        }
        PreviewTab.Contrast -> {
            probe?.invoke(PreviewTab.Contrast)
            ContrastTab(result, mode, specs.filter, modifier)
        }
    }
}

/** Both copies of [screen] with the handle between them, or the one copy [preview] shows. */
@Composable
private fun PreviewCopies(
    preview: PreviewSplit,
    specs: PaneSpecs,
    modifier: Modifier,
    screen: @Composable (spec: PaneSpec) -> Unit,
) {
    when (preview.shown) {
        PreviewMode.Split -> {
            SplitPreview(specs.light, specs.dark, preview.split, modifier.fillMaxSize(), screen = screen)
        }
        PreviewMode.Light -> {
            PreviewPane(specs.light, modifier.fillMaxSize()) { screen(specs.light) }
        }
        PreviewMode.Dark -> {
            PreviewPane(specs.dark, modifier.fillMaxSize()) { screen(specs.dark) }
        }
    }
}

/**
 * An app screen laid out at [width]'s screen width and scaled to fit the canvas, down to 0.6, with
 * [scroll] taking over past that. It stays centred while it fits.
 *
 * It measures the room itself rather than through `BoxWithConstraints`, so a library switch that
 * moves the workspace has no subcomposition per copy to carry along.
 */
@Composable
private fun DeviceScreen(
    width: DeviceWidth,
    scroll: ScrollState,
    content: @Composable () -> Unit,
) {
    val room = remember { ScreenRoom() }
    Box(Modifier.fillMaxSize().measureRoom(room), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .fillMaxHeight()
                .testTag(DEVICE_SCREEN_TAG)
                .horizontalScroll(scroll)
                .scaledScreen(width.screenWidth, room),
        ) { content() }
    }
}

/**
 * The width the canvas offers a screen, in pixels. The scroll in between hands the screen unbounded
 * width, so the screen reads it here, written a moment earlier in the same measure pass.
 */
private class ScreenRoom {
    var width: Int = 0
}

/** Notes the widest the content may be in [room], and otherwise measures as it would. */
private fun Modifier.measureRoom(room: ScreenRoom): Modifier =
    layout { measurable, constraints ->
        room.width = constraints.maxWidth
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * Lays the content out [width] wide and as tall as the space over its scale, then draws it at that
 * scale from its top start corner. The scale fits [width] into the [room], down to 0.6. Pointer
 * input follows the scale.
 */
private fun Modifier.scaledScreen(
    width: Dp,
    room: ScreenRoom,
): Modifier =
    layout { measurable, constraints ->
        val screenWidth = width.roundToPx()
        val scale = (room.width.toFloat() / screenWidth).coerceIn(MIN_SCREEN_SCALE, 1f)
        val child = if (constraints.hasBoundedHeight) {
            Constraints.fixed(screenWidth, (constraints.maxHeight / scale).roundToInt())
        } else {
            Constraints.fixedWidth(screenWidth)
        }
        val placeable = measurable.measure(child)
        layout((placeable.width * scale).roundToInt(), (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }

/** What the tab is called on its tab. */
private val PreviewTab.title: StringResource
    get() = when (this) {
        PreviewTab.App -> Res.string.canvas_tab_app
        PreviewTab.Components -> Res.string.canvas_tab_components
        PreviewTab.Roles -> Res.string.canvas_tab_roles
        PreviewTab.Palettes -> Res.string.canvas_tab_palettes
        PreviewTab.Contrast -> Res.string.canvas_tab_contrast
    }
