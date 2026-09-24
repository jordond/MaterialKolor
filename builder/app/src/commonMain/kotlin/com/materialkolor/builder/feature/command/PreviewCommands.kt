package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.canvas_device_button
import com.materialkolor.builder.generated.resources.canvas_device_desktop
import com.materialkolor.builder.generated.resources.canvas_device_phone
import com.materialkolor.builder.generated.resources.canvas_device_tablet
import com.materialkolor.builder.generated.resources.canvas_fullscreen
import com.materialkolor.builder.generated.resources.canvas_fullscreen_exit
import com.materialkolor.builder.generated.resources.canvas_inspect
import com.materialkolor.builder.generated.resources.canvas_mode_dark
import com.materialkolor.builder.generated.resources.canvas_mode_label
import com.materialkolor.builder.generated.resources.canvas_mode_light
import com.materialkolor.builder.generated.resources.canvas_mode_split
import com.materialkolor.builder.generated.resources.canvas_tab_app
import com.materialkolor.builder.generated.resources.canvas_tab_components
import com.materialkolor.builder.generated.resources.canvas_tab_contrast
import com.materialkolor.builder.generated.resources.canvas_tab_palettes
import com.materialkolor.builder.generated.resources.canvas_tab_roles
import com.materialkolor.builder.generated.resources.canvas_vision_achromatopsia
import com.materialkolor.builder.generated.resources.canvas_vision_button
import com.materialkolor.builder.generated.resources.canvas_vision_deuteranopia
import com.materialkolor.builder.generated.resources.canvas_vision_none
import com.materialkolor.builder.generated.resources.canvas_vision_protanopia
import com.materialkolor.builder.generated.resources.canvas_vision_tritanopia
import com.materialkolor.builder.generated.resources.command_choice
import com.materialkolor.builder.generated.resources.command_key_vision_menu
import com.materialkolor.builder.generated.resources.command_next_device_width
import com.materialkolor.builder.generated.resources.command_next_preview_mode
import com.materialkolor.builder.generated.resources.command_next_tab
import com.materialkolor.builder.generated.resources.command_previous_tab
import com.materialkolor.builder.generated.resources.command_show_tab
import com.materialkolor.builder.generated.resources.poster_collapse
import com.materialkolor.builder.generated.resources.poster_expand
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.layout.WindowClass
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The registry's sections for the preview, its modes, tabs, device widths and vision. See
// CommandSections.kt for why none of them restarts on its own.

@Composable
@NonRestartableComposable
internal fun previewCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val view = state.view
    val modes = state.capabilities[Control.PreviewModes]
    val modeLabel = stringResource(Res.string.canvas_mode_label)
    val nextMode = view.mode.next()
    list.add(
        id = "previewMode.next",
        category = CommandCategory.Preview,
        label = stringResource(Res.string.command_next_preview_mode),
        site = ControlSite.Direct(Region.Dock, stringResource(modeName(nextMode))),
        control = modes,
        shortcut = Shortcut.PreviewMode,
    ) { dispatcher.dispatch(WorkspaceAction.SetPreviewMode(nextMode, origin = null)) }
    PreviewMode.entries.forEach { mode ->
        val name = stringResource(modeName(mode))
        list.add(
            id = "previewMode.${mode.name}",
            category = CommandCategory.Preview,
            label = stringResource(Res.string.command_choice, modeLabel, name),
            site = ControlSite.Direct(Region.Dock, name),
            control = modes,
            selected = mode == view.mode,
        ) { dispatcher.dispatch(WorkspaceAction.SetPreviewMode(mode, origin = null)) }
    }
    val tabs = PreviewTab.entries
    val previous = tabs[(view.tab.ordinal - 1 + tabs.size) % tabs.size]
    val next = tabs[(view.tab.ordinal + 1) % tabs.size]
    list.add(
        id = "tab.previous",
        category = CommandCategory.Preview,
        label = stringResource(Res.string.command_previous_tab),
        site = ControlSite.Direct(Region.Canvas, stringResource(tabName(previous))),
        shortcut = Shortcut.PreviousTab,
    ) { dispatcher.dispatch(WorkspaceAction.SetPreviewTab(previous)) }
    list.add(
        id = "tab.next",
        category = CommandCategory.Preview,
        label = stringResource(Res.string.command_next_tab),
        site = ControlSite.Direct(Region.Canvas, stringResource(tabName(next))),
        shortcut = Shortcut.NextTab,
    ) { dispatcher.dispatch(WorkspaceAction.SetPreviewTab(next)) }
    tabs.forEach { tab ->
        val name = stringResource(tabName(tab))
        list.add(
            id = "tab.${tab.name}",
            category = CommandCategory.Preview,
            label = stringResource(Res.string.command_show_tab, name),
            site = ControlSite.Direct(Region.Canvas, name),
            selected = tab == view.tab,
        ) { dispatcher.dispatch(WorkspaceAction.SetPreviewTab(tab)) }
    }
    val inspect = stringResource(Res.string.canvas_inspect)
    list.add(
        id = "inspect",
        category = CommandCategory.Preview,
        label = inspect,
        site = ControlSite.Direct(Region.Dock, inspect),
        control = state.capabilities[Control.Inspect],
        shortcut = Shortcut.Inspect,
        selected = state.inspect,
    ) { dispatcher.dispatch(WorkspaceAction.SetInspect(!state.inspect)) }
    deviceWidthCommands(list, state, dispatcher)
    visionCommands(list, state, dispatcher)
    val fullscreen = stringResource(Res.string.canvas_fullscreen)
    val exit = stringResource(Res.string.canvas_fullscreen_exit)
    list.add(
        id = "fullscreen",
        category = CommandCategory.Preview,
        label = if (state.fullscreen) exit else fullscreen,
        site = if (state.fullscreen) {
            ControlSite.Direct(Region.Canvas, exit)
        } else {
            ControlSite.Direct(Region.Dock, fullscreen)
        },
        shortcut = Shortcut.Fullscreen,
        selected = state.fullscreen,
    ) { dispatcher.dispatch(WorkspaceAction.ToggleFullscreen) }
    // b-406g
    val mode = LocalLayout.current.posterMode
    val collapsed = state.posterCollapsed(mode)
    val poster = stringResource(if (collapsed) Res.string.poster_expand else Res.string.poster_collapse)
    // b-406
    // The phone's sheet has no collapse button, so there only the keys and the palette ask for it.
    val sheet = mode == PosterMode.Sheet
    list.add(
        id = "poster",
        category = CommandCategory.Preview,
        label = poster,
        site = if (sheet) null else ControlSite.Direct(Region.Poster, poster),
        shortcut = Shortcut.Poster,
    ) { dispatcher.dispatch(WorkspaceAction.SetPosterCollapsed(!collapsed)) }
}

/** The device widths, which phones never get, since their preview is always a phone (F-46). */
@Composable
@NonRestartableComposable
private fun deviceWidthCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    if (list.windowClass == WindowClass.Compact) return
    val control = state.capabilities[Control.DeviceWidth]
    val current = state.view.deviceWidth
    val menu = stringResource(Res.string.canvas_device_button, stringResource(widthName(current)))
    val next = DeviceWidth.entries[(current.ordinal + 1) % DeviceWidth.entries.size]
    list.add(
        id = "deviceWidth.next",
        category = CommandCategory.Preview,
        label = stringResource(Res.string.command_next_device_width),
        site = ControlSite.MenuItem(Region.Dock, menu, stringResource(widthName(next))),
        control = control,
        shortcut = Shortcut.DeviceWidth,
    ) { dispatcher.dispatch(WorkspaceAction.SetDeviceWidth(next)) }
    DeviceWidth.entries.forEach { width ->
        val name = stringResource(widthName(width))
        list.add(
            id = "deviceWidth.${width.name}",
            category = CommandCategory.Preview,
            label = stringResource(Res.string.canvas_device_button, name),
            site = ControlSite.MenuItem(Region.Dock, menu, name),
            control = control,
            selected = width == current,
        ) { dispatcher.dispatch(WorkspaceAction.SetDeviceWidth(width)) }
    }
}

@Composable
@NonRestartableComposable
private fun visionCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val menu = stringResource(Res.string.canvas_vision_button, stringResource(visionName(state.vision)))
    // b-315d
    // The held B has no command, since a toggle would only repeat the Achromatopsia row below.
    list.add(
        id = "visionMenu",
        category = CommandCategory.Vision,
        label = stringResource(Res.string.command_key_vision_menu),
        site = ControlSite.Direct(Region.Dock, menu),
        shortcut = Shortcut.VisionMenu,
    ) { dispatcher.dispatch(WorkspaceAction.SetVisionMenuOpen(open = true)) }
    VisionSimulation.entries.forEach { vision ->
        val name = stringResource(visionName(vision))
        list.add(
            id = "vision.${vision.name}",
            category = CommandCategory.Vision,
            label = stringResource(Res.string.canvas_vision_button, name),
            site = ControlSite.MenuItem(Region.Dock, menu, name),
            selected = vision == state.vision,
        ) { dispatcher.dispatch(WorkspaceAction.SetVision(vision)) }
    }
}

private fun PreviewMode.next(): PreviewMode = PreviewMode.entries[(ordinal + 1) % PreviewMode.entries.size]

private fun modeName(mode: PreviewMode): StringResource =
    when (mode) {
        PreviewMode.Light -> Res.string.canvas_mode_light
        PreviewMode.Split -> Res.string.canvas_mode_split
        PreviewMode.Dark -> Res.string.canvas_mode_dark
    }

private fun tabName(tab: PreviewTab): StringResource =
    when (tab) {
        PreviewTab.App -> Res.string.canvas_tab_app
        PreviewTab.Components -> Res.string.canvas_tab_components
        PreviewTab.Roles -> Res.string.canvas_tab_roles
        PreviewTab.Palettes -> Res.string.canvas_tab_palettes
        PreviewTab.Contrast -> Res.string.canvas_tab_contrast
    }

private fun widthName(width: DeviceWidth): StringResource =
    when (width) {
        DeviceWidth.Phone -> Res.string.canvas_device_phone
        DeviceWidth.Tablet -> Res.string.canvas_device_tablet
        DeviceWidth.Desktop -> Res.string.canvas_device_desktop
    }

private fun visionName(vision: VisionSimulation): StringResource =
    when (vision) {
        VisionSimulation.None -> Res.string.canvas_vision_none
        VisionSimulation.Protanopia -> Res.string.canvas_vision_protanopia
        VisionSimulation.Deuteranopia -> Res.string.canvas_vision_deuteranopia
        VisionSimulation.Tritanopia -> Res.string.canvas_vision_tritanopia
        VisionSimulation.Achromatopsia -> Res.string.canvas_vision_achromatopsia
    }
