package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.capability.Reason
import com.materialkolor.builder.feature.export.ExportAction
import com.materialkolor.builder.feature.export.ExportModel
import com.materialkolor.builder.feature.export.ExportOutcome
import com.materialkolor.builder.feature.export.launchZip
import com.materialkolor.builder.feature.poster.reasonText
import com.materialkolor.builder.feature.projects.ProjectsAction
import com.materialkolor.builder.feature.projects.ProjectsModel
import com.materialkolor.builder.feature.share.ShareController
import com.materialkolor.builder.feature.topbar.TopBarControl
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.command_category_appearance
import com.materialkolor.builder.generated.resources.command_category_export
import com.materialkolor.builder.generated.resources.command_category_general
import com.materialkolor.builder.generated.resources.command_category_history
import com.materialkolor.builder.generated.resources.command_category_library
import com.materialkolor.builder.generated.resources.command_category_motion
import com.materialkolor.builder.generated.resources.command_category_preview
import com.materialkolor.builder.generated.resources.command_category_project
import com.materialkolor.builder.generated.resources.command_category_seed
import com.materialkolor.builder.generated.resources.command_category_share
import com.materialkolor.builder.generated.resources.command_category_style
import com.materialkolor.builder.generated.resources.command_category_target
import com.materialkolor.builder.generated.resources.command_category_vision
import com.materialkolor.builder.generated.resources.command_link_label
import com.materialkolor.builder.generated.resources.command_saved
import com.materialkolor.builder.generated.resources.export_save_failed
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Something the builder can do, the one place both the shortcuts and the command palette run it
 * from (F-33, P6).
 *
 * @property[id] Stays the same across languages and compositions, for recents and tests.
 * @property[category] The group the palette lists it under.
 * @property[label] What it does, in words. Undo and Redo name the change.
 * @property[shortcut] The keys that run it, or null when only the palette and its control do.
 * @property[state] Whether it runs now, and why not when it does not.
 * @property[site] Where its control sits at the current window size, or null for the few that only
 * keys and the palette reach.
 * @property[selected] Whether the option it picks, or the switch it flips, is on now. Null for one
 * that is neither.
 * @property[run] Does it, straight away. Copies, saves and shares start their platform call before
 * this returns (R-B-302), so call it from inside the key press or the click.
 */
internal class Command(
    val id: String,
    val category: CommandCategory,
    val label: String,
    val shortcut: Shortcut?,
    val state: CommandState,
    val site: ControlSite?,
    val selected: Boolean?,
    val run: () -> Unit,
)

/** Whether a [Command] runs now. */
internal sealed interface CommandState {
    /** It runs. */
    data object Enabled : CommandState

    /** It does not, for [reason], which the palette shows in its place. */
    data class Disabled(
        val reason: String,
    ) : CommandState
}

/** The palette's groups, in the order it lists them. */
internal enum class CommandCategory(
    val title: StringResource,
) {
    General(Res.string.command_category_general),
    History(Res.string.command_category_history),
    Seed(Res.string.command_category_seed),
    Library(Res.string.command_category_library),
    Style(Res.string.command_category_style),
    Target(Res.string.command_category_target),
    Preview(Res.string.command_category_preview),
    Vision(Res.string.command_category_vision),
    Project(Res.string.command_category_project),
    Share(Res.string.command_category_share),
    Export(Res.string.command_category_export),
    Appearance(Res.string.command_category_appearance),
    Motion(Res.string.command_category_motion),
}

/** The part of the workspace a control sits in. */
internal enum class Region {
    TopBar,
    Poster,
    Canvas,
    Dock,
    Panel,
}

/**
 * Where the control that does the same as a [Command] sits, found by its accessible name (P6).
 */
internal sealed interface ControlSite {
    val region: Region

    /** A control named [name] in [region], inside the disclosure titled [opener] when there is one. */
    data class Direct(
        override val region: Region,
        val name: String,
        val opener: String? = null,
    ) : ControlSite

    /** The row [item] of the menu the button named [menu] opens. */
    data class MenuItem(
        override val region: Region,
        val menu: String,
        val item: String,
    ) : ControlSite

    /** A control named [name] in [panel], inside the disclosure titled [opener] when there is one. */
    data class InPanel(
        val panel: Panel,
        val name: String,
        val opener: String? = null,
    ) : ControlSite {
        override val region: Region
            get() = Region.Panel
    }
}

/**
 * Collects the registry's commands for one composition, a small builder so each section reads as
 * a list.
 *
 * @property[librarySegmented] Whether the top bar shows the libraries as a segmented row rather than
 * in its dropdown, the form the switcher measured last.
 * @property[switcherOrigin] The middle of the library switcher in root coordinates, read when a
 * library command runs so the new skin reveals from it, or null before the switcher has shown.
 */
internal class CommandList(
    private val reasons: Map<Reason, String>,
    val windowClass: WindowClass,
    val librarySegmented: Boolean, // b-315d
    val overflowed: Set<TopBarControl> = emptySet(), // b-406
    val switcherOrigin: () -> Offset? = { null }, // b-503a
) {
    private val commands = mutableListOf<Command>()

    /** Everything added so far, in order. */
    val all: List<Command>
        get() = commands

    /** Add a command whose control state [control] decides. A hidden control adds nothing (P6). */
    fun add(
        id: String,
        category: CommandCategory,
        label: String,
        site: ControlSite?,
        control: ControlState? = null,
        shortcut: Shortcut? = null,
        selected: Boolean? = null,
        disabledBecause: String? = null,
        run: () -> Unit,
    ) {
        val state = when {
            control is ControlState.Hidden -> return
            control is ControlState.Disabled -> CommandState.Disabled(reasons.getValue(control.reason))
            disabledBecause != null -> CommandState.Disabled(disabledBecause)
            else -> CommandState.Enabled
        }
        commands += Command(id, category, label, shortcut, state, site, selected, run)
    }
}

/**
 * Every command the builder has right now, built from the workspace and the models of Projects,
 * Share and Export, so the shortcuts and the palette run the same code.
 *
 * It covers the keyboard map, every top bar and overflow item, the libraries, the styles, the
 * target options, the preview's modes, tabs, widths and vision, motion, and every export option
 * and action. A control the target hides adds no command, so nothing ever says "Not supported".
 *
 * @param[scope] Where a command's work that goes on after it returns runs, a download or the Saved
 * toast, so a registry that leaves composition early hands in a scope that outlives it.
 */
@Composable
internal fun actionRegistry(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    projects: ProjectsModel = metroViewModel(),
    share: ShareController = metroViewModel(),
    export: ExportModel = metroViewModel(),
    shortcuts: ShortcutsModel = metroViewModel(),
    scope: CoroutineScope = rememberCoroutineScope(), // b-315d
): List<Command> {
    LocalRegistryBuilds.current?.invoke() // b-315d
    val latest by rememberUpdatedState(state)
    val reasons = Reason.entries.associateWith { reason -> stringResource(reasonText(reason)) }
    val windowClass = LocalLayout.current.windowClass
    // b-315d
    val segmented = shortcuts.switcherForm.segmented ?: (windowClass == WindowClass.Expanded)
    // b-406, b-503a
    val list = CommandList(reasons, windowClass, segmented, shortcuts.switcherForm.overflowed) {
        shortcuts.switcherForm.origin
    }
    val uriHandler = LocalUriHandler.current
    val saved = stringResource(Res.string.command_saved)
    val saveFailed = stringResource(Res.string.export_save_failed)
    val linkLabel = stringResource(Res.string.command_link_label)

    generalCommands(list, state, dispatcher, uriHandler, shortcuts)
    historyCommands(list, state, dispatcher)
    seedCommands(list, state, dispatcher)
    libraryAndStyleCommands(list, state, dispatcher)
    targetCommands(list, state, dispatcher)
    previewCommands(list, state, dispatcher)
    projectCommands(
        list = list,
        projectName = state.projectName,
        dispatcher = dispatcher,
        onSave = {
            projects.handle(ProjectsAction.SaveShared)
            scope.announceSaved(saved, dispatcher) { latest.saveStatus }
        },
        onNew = { projects.handle(ProjectsAction.New(copyCurrent = false)) },
        onCopyLink = {
            val link = share.link(latest.document, latest.projectName)
            if (link == null) {
                dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Share))
            } else {
                dispatcher.dispatch(WorkspaceAction.CopyText(link, linkLabel))
            }
        },
    )
    exportCommands(
        list = list,
        state = state,
        dispatcher = dispatcher,
        export = export,
        onCopyAll = { label ->
            when (val outcome = export.outcome()) {
                is ExportOutcome.Ready -> dispatcher.dispatch(WorkspaceAction.CopyText(outcome.allText, label))
                is ExportOutcome.Blocked -> dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Export))
            }
        },
        onDownload = {
            when (val outcome = export.outcome()) {
                is ExportOutcome.Ready -> {
                    scope.launchZip(export.files, outcome.zip, share = false) { result ->
                        if (result.isSuccess) {
                            export.handle(ExportAction.Exported)
                        } else {
                            dispatcher.dispatch(WorkspaceAction.ShowToast(saveFailed))
                        }
                    }
                }
                is ExportOutcome.Blocked -> {
                    dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Export))
                }
            }
        },
    )
    appearanceCommands(list, state, dispatcher)
    return list.all
}

// b-315d

/**
 * Told each time a registry builds, or null, which it always is outside tests. Tests provide it to
 * count the builds a workspace change costs.
 */
internal val LocalRegistryBuilds: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

/** Toasts [saved] once the save [status] reports has landed, and nothing when it failed. */
private fun CoroutineScope.announceSaved(
    saved: String,
    dispatcher: Dispatcher<WorkspaceAction>,
    status: () -> SaveStatus,
) {
    launch {
        withFrameNanos { }
        val settled = snapshotFlow(status).first { now -> now !is SaveStatus.Pending }
        if (settled == SaveStatus.Idle) dispatcher.dispatch(WorkspaceAction.ShowToast(saved))
    }
}
