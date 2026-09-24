package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.UriHandler
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.about.GITHUB_URL
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.export.ExportAction
import com.materialkolor.builder.feature.export.ExportModel
import com.materialkolor.builder.feature.poster.shufflesNothing
import com.materialkolor.builder.feature.poster.styleName
import com.materialkolor.builder.feature.poster.styleTooltip
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.topbar.redoText
import com.materialkolor.builder.feature.topbar.undoText
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.about_motion
import com.materialkolor.builder.generated.resources.about_motion_full
import com.materialkolor.builder.generated.resources.about_motion_reduce
import com.materialkolor.builder.generated.resources.about_motion_system
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
import com.materialkolor.builder.generated.resources.command_add_image
import com.materialkolor.builder.generated.resources.command_all_files_label
import com.materialkolor.builder.generated.resources.command_choice
import com.materialkolor.builder.generated.resources.command_copy_all
import com.materialkolor.builder.generated.resources.command_copy_link
import com.materialkolor.builder.generated.resources.command_copy_seed
import com.materialkolor.builder.generated.resources.command_next_appearance
import com.materialkolor.builder.generated.resources.command_next_device_width
import com.materialkolor.builder.generated.resources.command_next_preview_mode
import com.materialkolor.builder.generated.resources.command_next_tab
import com.materialkolor.builder.generated.resources.command_nothing_to_redo
import com.materialkolor.builder.generated.resources.command_nothing_to_undo
import com.materialkolor.builder.generated.resources.command_previous_tab
import com.materialkolor.builder.generated.resources.command_save
import com.materialkolor.builder.generated.resources.command_show_tab
import com.materialkolor.builder.generated.resources.command_single_keys
import com.materialkolor.builder.generated.resources.command_use_library
import com.materialkolor.builder.generated.resources.command_use_style
import com.materialkolor.builder.generated.resources.export_animate
import com.materialkolor.builder.generated.resources.export_copy_all
import com.materialkolor.builder.generated.resources.export_download
import com.materialkolor.builder.generated.resources.export_dynamic_color
import com.materialkolor.builder.generated.resources.export_mode
import com.materialkolor.builder.generated.resources.export_mode_dynamic
import com.materialkolor.builder.generated.resources.export_mode_frozen
import com.materialkolor.builder.generated.resources.export_options
import com.materialkolor.builder.generated.resources.export_project
import com.materialkolor.builder.generated.resources.export_project_android
import com.materialkolor.builder.generated.resources.export_project_multiplatform
import com.materialkolor.builder.generated.resources.export_variants
import com.materialkolor.builder.generated.resources.export_variants_all
import com.materialkolor.builder.generated.resources.export_variants_standard
import com.materialkolor.builder.generated.resources.export_version_catalog
import com.materialkolor.builder.generated.resources.extras_amoled
import com.materialkolor.builder.generated.resources.extras_motion_expressive
import com.materialkolor.builder.generated.resources.extras_motion_label
import com.materialkolor.builder.generated.resources.extras_motion_standard
import com.materialkolor.builder.generated.resources.extras_title
import com.materialkolor.builder.generated.resources.poster_all_locked
import com.materialkolor.builder.generated.resources.poster_collapse
import com.materialkolor.builder.generated.resources.poster_copied_hex
import com.materialkolor.builder.generated.resources.poster_copy_hex
import com.materialkolor.builder.generated.resources.poster_expand
import com.materialkolor.builder.generated.resources.poster_image
import com.materialkolor.builder.generated.resources.poster_lock_hue
import com.materialkolor.builder.generated.resources.poster_lock_seed
import com.materialkolor.builder.generated.resources.poster_lock_style
import com.materialkolor.builder.generated.resources.poster_projects
import com.materialkolor.builder.generated.resources.poster_projects_named
import com.materialkolor.builder.generated.resources.poster_shuffle
import com.materialkolor.builder.generated.resources.projects_new
import com.materialkolor.builder.generated.resources.share_copy
import com.materialkolor.builder.generated.resources.style_chip
import com.materialkolor.builder.generated.resources.topbar_about
import com.materialkolor.builder.generated.resources.topbar_appearance_dark
import com.materialkolor.builder.generated.resources.topbar_appearance_light
import com.materialkolor.builder.generated.resources.topbar_appearance_system
import com.materialkolor.builder.generated.resources.topbar_commands
import com.materialkolor.builder.generated.resources.topbar_export
import com.materialkolor.builder.generated.resources.topbar_github
import com.materialkolor.builder.generated.resources.topbar_help
import com.materialkolor.builder.generated.resources.topbar_library
import com.materialkolor.builder.generated.resources.topbar_library_custom
import com.materialkolor.builder.generated.resources.topbar_library_expressive
import com.materialkolor.builder.generated.resources.topbar_library_fluent
import com.materialkolor.builder.generated.resources.topbar_library_m3
import com.materialkolor.builder.generated.resources.topbar_library_unstyled
import com.materialkolor.builder.generated.resources.topbar_more
import com.materialkolor.builder.generated.resources.topbar_share
import com.materialkolor.builder.generated.resources.topbar_shortcuts
import com.materialkolor.builder.kit.layout.WindowClass
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The registry's sections, one per part of the builder, each adding its commands in the order the
// palette lists them. None restarts on its own, since a section that did would add its commands a
// second time to the list the registry already handed out. A state one reads rebuilds the whole
// registry instead.

@Composable
@NonRestartableComposable
internal fun generalCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    uriHandler: UriHandler,
    shortcuts: ShortcutsModel,
) {
    val more = stringResource(Res.string.topbar_more)
    val palette = stringResource(Res.string.topbar_commands)
    val paletteSite = if (list.windowClass == WindowClass.Compact) {
        ControlSite.MenuItem(Region.TopBar, more, palette)
    } else {
        ControlSite.Direct(Region.TopBar, palette)
    }
    list.add("palette", CommandCategory.General, palette, paletteSite, shortcut = Shortcut.Palette) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Palette))
    }
    val sheet = stringResource(Res.string.topbar_shortcuts)
    val sheetSite = ControlSite.MenuItem(Region.TopBar, more, sheet)
    list.add("cheatSheet", CommandCategory.General, sheet, sheetSite, shortcut = Shortcut.CheatSheet) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.CheatSheet))
    }
    val singleKeys = stringResource(Res.string.command_single_keys)
    val on = state.preferences.singleKeyShortcuts
    val singleKeysSite = ControlSite.InPanel(Panel.CheatSheet, singleKeys)
    list.add("singleKeys", CommandCategory.General, singleKeys, singleKeysSite, selected = on) {
        shortcuts.setSingleKeys(!on)
    }
    val help = stringResource(Res.string.topbar_help)
    list.add("help", CommandCategory.General, help, ControlSite.MenuItem(Region.TopBar, more, help)) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Help))
    }
    val about = stringResource(Res.string.topbar_about)
    list.add("about", CommandCategory.General, about, ControlSite.MenuItem(Region.TopBar, more, about)) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.About))
    }
    val github = stringResource(Res.string.topbar_github)
    list.add("github", CommandCategory.General, github, ControlSite.MenuItem(Region.TopBar, more, github)) {
        uriHandler.openUri(GITHUB_URL)
    }
}

@Composable
@NonRestartableComposable
internal fun historyCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val undo = undoText(state.history, state.document)
    val redo = redoText(state.history, state.document)
    val nothingToUndo = stringResource(Res.string.command_nothing_to_undo)
    val nothingToRedo = stringResource(Res.string.command_nothing_to_redo)
    list.add(
        id = "undo",
        category = CommandCategory.History,
        label = undo,
        site = topBarSite(list, undo),
        shortcut = Shortcut.Undo,
        disabledBecause = nothingToUndo.takeUnless { state.history.canUndo },
    ) { dispatcher.dispatch(WorkspaceAction.Undo) }
    list.add(
        id = "redo",
        category = CommandCategory.History,
        label = redo,
        site = topBarSite(list, redo),
        shortcut = Shortcut.Redo,
        disabledBecause = nothingToRedo.takeUnless { state.history.canRedo },
    ) { dispatcher.dispatch(WorkspaceAction.Redo) }
}

/** A top bar button that phones move into the overflow menu. */
@Composable
private fun topBarSite(
    list: CommandList,
    name: String,
): ControlSite =
    if (list.windowClass == WindowClass.Compact) {
        ControlSite.MenuItem(Region.TopBar, stringResource(Res.string.topbar_more), name)
    } else {
        ControlSite.Direct(Region.TopBar, name)
    }

@Composable
@NonRestartableComposable
internal fun seedCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val seedEntry = state.capabilities[Control.SeedEntryPoints]
    val shuffle = stringResource(Res.string.poster_shuffle)
    list.add(
        id = "shuffle",
        category = CommandCategory.Seed,
        label = shuffle,
        site = ControlSite.Direct(Region.Poster, shuffle),
        control = seedEntry,
        shortcut = Shortcut.Shuffle,
        disabledBecause = stringResource(Res.string.poster_all_locked).takeIf { state.preferences.shufflesNothing() },
    ) { dispatcher.dispatch(WorkspaceAction.Shuffle(origin = null)) }
    ShuffleLock.entries.forEach { lock ->
        val name = stringResource(lockName(lock))
        val on = when (lock) {
            ShuffleLock.Hue -> state.preferences.hueLock
            ShuffleLock.Style -> state.preferences.styleLock
            ShuffleLock.Seed -> state.preferences.seedLock
        }
        val shortcut = when (lock) {
            ShuffleLock.Hue -> Shortcut.HueLock
            ShuffleLock.Style -> Shortcut.StyleLock
            ShuffleLock.Seed -> null
        }
        val site = ControlSite.Direct(Region.Poster, name)
        list.add("lock.${lock.name}", CommandCategory.Seed, name, site, shortcut = shortcut, selected = on) {
            dispatcher.dispatch(WorkspaceAction.SetLock(lock, !on))
        }
    }
    val hexLabel = stringResource(Res.string.poster_copied_hex)
    val copySite = ControlSite.Direct(Region.Poster, stringResource(Res.string.poster_copy_hex))
    val copy = stringResource(Res.string.command_copy_seed)
    list.add("copySeed", CommandCategory.Seed, copy, copySite, shortcut = Shortcut.CopySeed) {
        dispatcher.dispatch(WorkspaceAction.CopyText(state.document.seed.toHex(), hexLabel))
    }
    val imageSite = ControlSite.Direct(Region.Poster, stringResource(Res.string.poster_image))
    val image = stringResource(Res.string.command_add_image)
    list.add("addImage", CommandCategory.Seed, image, imageSite, seedEntry, shortcut = Shortcut.AddImage) {
        dispatcher.dispatch(WorkspaceAction.OpenImagePicker)
    }
}

private fun lockName(lock: ShuffleLock): StringResource =
    when (lock) {
        ShuffleLock.Hue -> Res.string.poster_lock_hue
        ShuffleLock.Style -> Res.string.poster_lock_style
        ShuffleLock.Seed -> Res.string.poster_lock_seed
    }

@Composable
@NonRestartableComposable
internal fun libraryAndStyleCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val current = LibraryChoice.of(state.document)
    val switcher = stringResource(Res.string.topbar_library)
    LibraryChoice.entries.forEachIndexed { index, choice ->
        val name = stringResource(libraryName(choice))
        val site = if (list.windowClass == WindowClass.Expanded) {
            ControlSite.Direct(Region.TopBar, name)
        } else {
            ControlSite.MenuItem(Region.TopBar, switcher, name)
        }
        list.add(
            id = "library.${choice.name}",
            category = CommandCategory.Library,
            label = stringResource(Res.string.command_use_library, name),
            site = site,
            shortcut = LIBRARY_KEYS[index],
            selected = choice == current,
        ) {
            if (choice != current) dispatcher.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin = null))
        }
    }
    val styleState = state.capabilities[Control.Style]
    Style.entries.forEach { style ->
        val name = stringResource(styleName(style))
        val selected = style == state.document.style
        val chip = stringResource(Res.string.style_chip, name, stringResource(styleTooltip(style)))
        list.add(
            id = "style.${style.name}",
            category = CommandCategory.Style,
            label = stringResource(Res.string.command_use_style, name),
            site = ControlSite.Direct(Region.Poster, chip),
            control = styleState,
            selected = selected,
        ) {
            if (!selected) {
                dispatcher.dispatch(WorkspaceAction.EditWithReveal(DocumentChange.SetStyle(style), origin = null))
            }
        }
    }
}

private val LIBRARY_KEYS = listOf(
    Shortcut.Library1,
    Shortcut.Library2,
    Shortcut.Library3,
    Shortcut.Library4,
    Shortcut.Library5,
)

private fun libraryName(choice: LibraryChoice): StringResource =
    when (choice) {
        LibraryChoice.M3 -> Res.string.topbar_library_m3
        LibraryChoice.Expressive -> Res.string.topbar_library_expressive
        LibraryChoice.Unstyled -> Res.string.topbar_library_unstyled
        LibraryChoice.Fluent -> Res.string.topbar_library_fluent
        LibraryChoice.Custom -> Res.string.topbar_library_custom
    }

@Composable
@NonRestartableComposable
internal fun targetCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val extras = stringResource(Res.string.extras_title)
    val amoled = stringResource(Res.string.extras_amoled)
    val on = state.document.amoled
    list.add(
        id = "amoled",
        category = CommandCategory.Target,
        label = amoled,
        site = ControlSite.Direct(Region.Poster, amoled, opener = extras),
        control = state.capabilities[Control.AmoledDark],
        selected = on,
    ) { dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetAmoled(!on), EditPhase.Discrete)) }
    val motion = stringResource(Res.string.extras_motion_label)
    MotionSchemeChoice.entries.forEach { choice ->
        val name = stringResource(motionSchemeName(choice))
        val selected = choice == state.document.motionScheme
        list.add(
            id = "motionScheme.${choice.name}",
            category = CommandCategory.Target,
            label = stringResource(Res.string.command_choice, motion, name),
            site = ControlSite.Direct(Region.Poster, name, opener = extras),
            control = state.capabilities[Control.MotionScheme],
            selected = selected,
        ) {
            if (!selected) {
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetMotionScheme(choice), EditPhase.Discrete))
            }
        }
    }
}

private fun motionSchemeName(choice: MotionSchemeChoice): StringResource =
    when (choice) {
        MotionSchemeChoice.Standard -> Res.string.extras_motion_standard
        MotionSchemeChoice.Expressive -> Res.string.extras_motion_expressive
    }

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
    val collapsed = state.preferences.posterCollapsed
    val poster = stringResource(if (collapsed) Res.string.poster_expand else Res.string.poster_collapse)
    list.add(
        id = "poster",
        category = CommandCategory.Preview,
        label = poster,
        site = ControlSite.Direct(Region.Poster, poster),
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

@Composable
@NonRestartableComposable
internal fun projectCommands(
    list: CommandList,
    projectName: String,
    dispatcher: Dispatcher<WorkspaceAction>,
    onSave: () -> Unit,
    onNew: () -> Unit,
    onCopyLink: () -> Unit,
) {
    val projects = stringResource(Res.string.poster_projects)
    // The poster's button reads the project's name once it has one.
    val projectsName = if (projectName.isBlank()) {
        projects
    } else {
        stringResource(Res.string.poster_projects_named, projectName)
    }
    val projectsSite = ControlSite.Direct(Region.Poster, projectsName)
    list.add("projects", CommandCategory.Project, projects, projectsSite, shortcut = Shortcut.Projects) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Projects))
    }
    val new = stringResource(Res.string.projects_new)
    val newSite = ControlSite.InPanel(Panel.Projects, new)
    list.add("newProject", CommandCategory.Project, new, newSite, shortcut = Shortcut.NewProject, run = onNew)
    val save = stringResource(Res.string.command_save)
    list.add("save", CommandCategory.Project, save, site = null, shortcut = Shortcut.Save, run = onSave)
    val share = stringResource(Res.string.topbar_share)
    list.add("share", CommandCategory.Share, share, ControlSite.Direct(Region.TopBar, share)) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Share))
    }
    val copyLink = stringResource(Res.string.command_copy_link)
    val copyLinkSite = ControlSite.InPanel(Panel.Share, stringResource(Res.string.share_copy))
    list.add("copyLink", CommandCategory.Share, copyLink, copyLinkSite, shortcut = Shortcut.CopyLink, run = onCopyLink)
}

@Composable
@NonRestartableComposable
internal fun exportCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    export: ExportModel,
    onCopyAll: (label: String) -> Unit,
    onDownload: () -> Unit,
) {
    val exportState by export.collectAsState()
    val prefs = exportState.prefs
    val caps = state.capabilities
    val openExport = stringResource(Res.string.topbar_export)
    val exportSite = ControlSite.Direct(Region.TopBar, openExport)
    list.add("export", CommandCategory.Export, openExport, exportSite, shortcut = Shortcut.Export) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Export))
    }
    val allFiles = stringResource(Res.string.command_all_files_label)
    val copyAllSite = ControlSite.InPanel(Panel.Export, stringResource(Res.string.export_copy_all))
    val copyAll = stringResource(Res.string.command_copy_all)
    list.add("export.copyAll", CommandCategory.Export, copyAll, copyAllSite, shortcut = Shortcut.CopyAll) {
        onCopyAll(allFiles)
    }
    val download = stringResource(Res.string.export_download)
    list.add("export.download", CommandCategory.Export, download, ControlSite.InPanel(Panel.Export, download)) {
        onDownload()
    }
    val options = stringResource(Res.string.export_options)
    val modeLabel = stringResource(Res.string.export_mode)
    ExportMode.entries.forEach { mode ->
        val name = stringResource(
            if (mode == ExportMode.Dynamic) Res.string.export_mode_dynamic else Res.string.export_mode_frozen,
        )
        list.add(
            id = "export.mode.${mode.name}",
            category = CommandCategory.Export,
            label = stringResource(Res.string.command_choice, modeLabel, name),
            site = ControlSite.InPanel(Panel.Export, name),
            control = if (mode == ExportMode.Frozen) caps[Control.FrozenExport] else null,
            selected = prefs.mode == mode,
        ) { export.handle(ExportAction.SetMode(mode)) }
    }
    val projectLabel = stringResource(Res.string.export_project)
    listOf(true, false).forEach { multiplatform ->
        val name = stringResource(
            if (multiplatform) Res.string.export_project_multiplatform else Res.string.export_project_android,
        )
        list.add(
            id = if (multiplatform) "export.project.Multiplatform" else "export.project.AndroidOnly",
            category = CommandCategory.Export,
            label = stringResource(Res.string.command_choice, projectLabel, name),
            site = ControlSite.InPanel(Panel.Export, name, opener = options),
            control = caps[Control.KmpOrAndroid],
            selected = prefs.multiplatform == multiplatform,
        ) { export.handle(ExportAction.SetMultiplatform(multiplatform)) }
    }
    val catalog = stringResource(Res.string.export_version_catalog)
    list.add(
        id = "export.versionCatalog",
        category = CommandCategory.Export,
        label = catalog,
        site = ControlSite.InPanel(Panel.Export, catalog, opener = options),
        control = caps[Control.VersionCatalog],
        selected = prefs.versionCatalog,
    ) { export.handle(ExportAction.SetVersionCatalog(!prefs.versionCatalog)) }
    val animate = stringResource(Res.string.export_animate)
    list.add(
        id = "export.animate",
        category = CommandCategory.Export,
        label = animate,
        site = ControlSite.InPanel(Panel.Export, animate, opener = options),
        control = caps[Control.ColorAnimation],
        selected = prefs.animate,
    ) { export.handle(ExportAction.SetAnimate(!prefs.animate)) }
    val variantsLabel = stringResource(Res.string.export_variants)
    FrozenVariants.entries.forEach { variants ->
        val standard = variants == FrozenVariants.StandardOnly
        val name = stringResource(if (standard) Res.string.export_variants_standard else Res.string.export_variants_all)
        list.add(
            id = "export.variants.${variants.name}",
            category = CommandCategory.Export,
            label = stringResource(Res.string.command_choice, variantsLabel, name),
            site = ControlSite.InPanel(Panel.Export, name, opener = options),
            control = caps[Control.FrozenExport],
            selected = prefs.frozenVariants == variants,
        ) { export.handle(ExportAction.SetFrozenVariants(variants)) }
    }
    // The export sheet offers the wallpaper colors only to an Android project of Material 3.
    if (exportState.target in MATERIAL3_TARGETS && !prefs.multiplatform) {
        val dynamic = stringResource(Res.string.export_dynamic_color)
        list.add(
            id = "export.dynamicColor",
            category = CommandCategory.Export,
            label = dynamic,
            site = ControlSite.InPanel(Panel.Export, dynamic, opener = options),
            selected = prefs.androidDynamicColor,
        ) { export.handle(ExportAction.SetAndroidDynamicColor(!prefs.androidDynamicColor)) }
    }
}

private val MATERIAL3_TARGETS = setOf(ExportTarget.Material3, ExportTarget.Material3Expressive)

@Composable
@NonRestartableComposable
internal fun appearanceCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val more = stringResource(Res.string.topbar_more)
    val current = state.preferences.appearance
    val next = Appearance.entries[(current.ordinal + 1) % Appearance.entries.size]
    list.add(
        id = "appearance.next",
        category = CommandCategory.Appearance,
        label = stringResource(Res.string.command_next_appearance),
        site = ControlSite.MenuItem(Region.TopBar, more, stringResource(appearanceName(next))),
        shortcut = Shortcut.Appearance,
    ) { dispatcher.dispatch(WorkspaceAction.SetAppearance(next)) }
    Appearance.entries.forEach { appearance ->
        val name = stringResource(appearanceName(appearance))
        list.add(
            id = "appearance.${appearance.name}",
            category = CommandCategory.Appearance,
            label = name,
            site = ControlSite.MenuItem(Region.TopBar, more, name),
            selected = appearance == current,
        ) { dispatcher.dispatch(WorkspaceAction.SetAppearance(appearance)) }
    }
    val motionLabel = stringResource(Res.string.about_motion)
    MotionOverride.entries.forEach { motion ->
        val name = stringResource(motionName(motion))
        list.add(
            id = "motion.${motion.name}",
            category = CommandCategory.Motion,
            label = stringResource(Res.string.command_choice, motionLabel, name),
            site = ControlSite.InPanel(Panel.About, name),
            selected = motion == state.preferences.motion,
        ) { dispatcher.dispatch(WorkspaceAction.SetMotionOverride(motion)) }
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

private fun appearanceName(appearance: Appearance): StringResource =
    when (appearance) {
        Appearance.System -> Res.string.topbar_appearance_system
        Appearance.Light -> Res.string.topbar_appearance_light
        Appearance.Dark -> Res.string.topbar_appearance_dark
    }

private fun motionName(motion: MotionOverride): StringResource =
    when (motion) {
        MotionOverride.System -> Res.string.about_motion_system
        MotionOverride.Reduce -> Res.string.about_motion_reduce
        MotionOverride.Full -> Res.string.about_motion_full
    }
