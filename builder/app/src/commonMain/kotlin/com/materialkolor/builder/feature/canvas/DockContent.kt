package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.feature.poster.handFocusTo
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
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.shell.DockRegion
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The dock's tools in the skin's `DockRegion`, Light, Split and Dark, the device width, Inspect,
 * Vision and Fullscreen (F-19).
 *
 * A phone has no device width, its preview is always a phone (F-46). Its dock keeps to one row, so
 * the mode switch drops the glyphs and Inspect shows as a glyph alone. Fullscreen leaves through the
 * floating exit instead of the dock. While the keyboard
 * is in use, focus comes back to the Fullscreen button when fullscreen ends, since the exit that held
 * it is gone, and to the Inspect toggle when Inspect ends, since the preview that held it lets go. A
 * pointer leaves the focus alone.
 */
@Composable
internal fun DockContent(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val compact = LocalLayout.current.windowClass == WindowClass.Compact
    val modes = PreviewMode.entries.associateWith { mode -> stringResource(mode.title) }
    val inputModes = LocalInputModeManager.current
    val fullscreenButton = remember { FocusRequester() }
    val wasFullscreen = remember { mutableStateOf(state.fullscreen) }
    LaunchedEffect(state.fullscreen) {
        val ended = wasFullscreen.value && !state.fullscreen
        if (ended) inputModes.handFocusTo(fullscreenButton) // b-315b
        wasFullscreen.value = state.fullscreen
    }
    val inspectToggle = remember { FocusRequester() }
    val wasInspecting = remember { mutableStateOf(state.inspect) }
    LaunchedEffect(state.inspect) {
        val ended = wasInspecting.value && !state.inspect
        if (ended) inputModes.handFocusTo(inspectToggle) // b-315b
        wasInspecting.value = state.inspect
    }
    DockRegion(modifier) {
        BuilderSegmented(
            options = PreviewMode.entries,
            selected = state.view.mode,
            onSelect = { mode -> dispatcher.dispatch(WorkspaceAction.SetPreviewMode(mode, origin = null)) },
            label = stringResource(Res.string.canvas_mode_label),
            // b-512
            // As wide as its options, so a row that shares its width out evenly leaves room for the tools.
            modifier = Modifier.width(IntrinsicSize.Max),
            optionIcon = { mode -> if (compact) null else mode.icon },
            // Arrowing along the modes should not slide the handle at every stop.
            selectOnFocus = false,
            optionLabel = { mode -> modes.getValue(mode) },
        )
        if (!compact) {
            DeviceWidthMenu(
                width = state.view.deviceWidth,
                onPick = { width -> dispatcher.dispatch(WorkspaceAction.SetDeviceWidth(width)) },
            )
        }
        val inspect = stringResource(Res.string.canvas_inspect)
        if (compact) {
            // b-512
            // A phone's dock is one row of glyphs, so Inspect shows its state by its fill.
            BuilderIconButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.SetInspect(!state.inspect)) },
                icon = IconId.Inspect,
                contentDescription = inspect,
                modifier = Modifier
                    .focusRequester(inspectToggle)
                    .semantics { toggleableState = ToggleableState(state.inspect) },
                emphasis = if (state.inspect) Emphasis.Primary else Emphasis.Subtle,
            )
        } else {
            BuilderToggleButton(
                checked = state.inspect,
                onCheckedChange = { on -> dispatcher.dispatch(WorkspaceAction.SetInspect(on)) },
                label = inspect,
                modifier = Modifier.focusRequester(inspectToggle),
                icon = IconId.Inspect,
            )
        }
        VisionMenu(
            vision = state.vision,
            onPick = { vision -> dispatcher.dispatch(WorkspaceAction.SetVision(vision)) },
            // b-315c
            open = state.visionMenuOpen,
            onOpenChange = { open -> dispatcher.dispatch(WorkspaceAction.SetVisionMenuOpen(open)) },
            held = state.grayscaleHeld,
        )
        if (!state.fullscreen) {
            BuilderIconButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.ToggleFullscreen) },
                icon = IconId.Fullscreen,
                contentDescription = stringResource(Res.string.canvas_fullscreen),
                modifier = Modifier.focusRequester(fullscreenButton),
            )
        }
    }
}

/**
 * The floating pill that leaves fullscreen and brings the poster and the top bar back (F-19). While
 * the keyboard is in use it takes focus as it arrives, since the Fullscreen button that had it is
 * gone.
 */
@Composable
internal fun FullscreenExit(
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val inputModes = LocalInputModeManager.current
    val pill = remember { FocusRequester() }
    LaunchedEffect(pill) { inputModes.handFocusTo(pill) } // b-315b
    BuilderButton(
        onClick = { dispatcher.dispatch(WorkspaceAction.ToggleFullscreen) },
        label = stringResource(Res.string.canvas_fullscreen_exit),
        modifier = modifier.focusRequester(pill),
        emphasis = Emphasis.Secondary,
        icon = IconId.Close,
    )
}

/** The device width button and its menu of Phone, Tablet and Desktop (F-46). */
@Composable
private fun DeviceWidthMenu(
    width: DeviceWidth,
    onPick: (DeviceWidth) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val names = DeviceWidth.entries.associateWith { option -> stringResource(option.title) }
    val items = DeviceWidth.entries.map { option ->
        BuilderMenuItem(
            label = names.getValue(option),
            onClick = { onPick(option) },
            icon = if (option == width) IconId.Check else option.icon,
        )
    }
    BuilderMenu(expanded = open, onDismissRequest = { open = false }, items = items) {
        BuilderIconButton(
            onClick = { open = true },
            icon = width.icon,
            contentDescription = stringResource(Res.string.canvas_device_button, names.getValue(width)),
        )
    }
}

private val PreviewMode.title: StringResource
    get() = when (this) {
        PreviewMode.Light -> Res.string.canvas_mode_light
        PreviewMode.Split -> Res.string.canvas_mode_split
        PreviewMode.Dark -> Res.string.canvas_mode_dark
    }

private val PreviewMode.icon: IconId
    get() = when (this) {
        PreviewMode.Light -> IconId.Sun
        PreviewMode.Split -> IconId.Split
        PreviewMode.Dark -> IconId.Moon
    }

private val DeviceWidth.title: StringResource
    get() = when (this) {
        DeviceWidth.Phone -> Res.string.canvas_device_phone
        DeviceWidth.Tablet -> Res.string.canvas_device_tablet
        DeviceWidth.Desktop -> Res.string.canvas_device_desktop
    }

private val DeviceWidth.icon: IconId
    get() = when (this) {
        DeviceWidth.Phone -> IconId.Phone
        DeviceWidth.Tablet -> IconId.Tablet
        DeviceWidth.Desktop -> IconId.Desktop
    }
