package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.feature.export.ExportAction
import com.materialkolor.builder.feature.export.ExportModel
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.about_motion
import com.materialkolor.builder.generated.resources.about_motion_full
import com.materialkolor.builder.generated.resources.about_motion_reduce
import com.materialkolor.builder.generated.resources.about_motion_system
import com.materialkolor.builder.generated.resources.command_all_files_label
import com.materialkolor.builder.generated.resources.command_choice
import com.materialkolor.builder.generated.resources.command_copy_all
import com.materialkolor.builder.generated.resources.command_copy_link
import com.materialkolor.builder.generated.resources.command_next_appearance
import com.materialkolor.builder.generated.resources.command_save
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
import com.materialkolor.builder.generated.resources.poster_projects
import com.materialkolor.builder.generated.resources.poster_projects_named
import com.materialkolor.builder.generated.resources.projects_new
import com.materialkolor.builder.generated.resources.share_copy
import com.materialkolor.builder.generated.resources.topbar_appearance_dark
import com.materialkolor.builder.generated.resources.topbar_appearance_light
import com.materialkolor.builder.generated.resources.topbar_appearance_system
import com.materialkolor.builder.generated.resources.topbar_export
import com.materialkolor.builder.generated.resources.topbar_more
import com.materialkolor.builder.generated.resources.topbar_share
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The registry's sections for the project, the export and the builder's own appearance and motion.
// See CommandSections.kt for why none of them restarts on its own.

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
