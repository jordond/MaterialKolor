package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.core.data.DeletedProject
import com.materialkolor.builder.feature.workspace.BannerAction
import com.materialkolor.builder.feature.workspace.BannerStack
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceBanner
import com.materialkolor.builder.feature.workspace.WorkspaceBanners
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.projects_deleted
import com.materialkolor.builder.generated.resources.projects_problem_create
import com.materialkolor.builder.generated.resources.projects_problem_delete
import com.materialkolor.builder.generated.resources.projects_problem_delete_newer
import com.materialkolor.builder.generated.resources.projects_problem_duplicate
import com.materialkolor.builder.generated.resources.projects_problem_open
import com.materialkolor.builder.generated.resources.projects_problem_rename
import com.materialkolor.builder.generated.resources.projects_problem_restore
import com.materialkolor.builder.generated.resources.projects_problem_set_aside
import com.materialkolor.builder.generated.resources.projects_undo
import com.materialkolor.builder.kit.control.ToastDuration
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The projects drawer, open while `state.panel` is [Panel.Projects]. The banners about the open
 * project join the one stack [WorkspaceBanners] draws.
 *
 * Opening or starting a project closes the drawer. A delete raises an undo toast through
 * [dispatcher], up for the kit's [ToastDuration.Long] and paused while it is hovered or focused, and
 * anything storage turns down raises a toast of its own.
 *
 * @param[returnFocusTo] The Projects button that opened the drawer, which gets focus back once it
 * closes (AR-09).
 */
@Composable
internal fun ProjectsHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null, // b-221f
    model: ProjectsModel = metroViewModel(),
) {
    val projects by model.collectAsState()
    val visible = state.panel == Panel.Projects
    val now = remember(visible, projects.projects) { model.nowMillis() }
    ProjectsDrawer(
        visible = visible,
        state = projects,
        now = now,
        onAction = { action ->
            model.handle(action)
            if (action is ProjectsAction.Open || action is ProjectsAction.New) {
                dispatcher.dispatch(WorkspaceAction.ClosePanel)
            }
        },
        onGetLink = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Share)) },
        onDismissRequest = { dispatcher.dispatch(WorkspaceAction.ClosePanel) },
        modifier = modifier, // b-314b
        returnFocusTo = returnFocusTo, // b-221f
    )
    projects.lastDeletion?.let { deleted ->
        UndoToast(deleted, dispatcher) { undone -> model.handle(ProjectsAction.UndoDelete(undone)) }
    }
    projects.problem?.let { problem ->
        val message = stringResource(problemText(problem))
        LaunchedEffect(problem, message) {
            if (message.isEmpty()) return@LaunchedEffect
            dispatcher.dispatch(WorkspaceAction.ShowToast(message))
            model.handle(ProjectsAction.ProblemShown)
        }
    }
}

// b-314b

/**
 * The banners [state] raises by itself, a clash with another tab, a theme that is not saved yet and
 * data a newer build saved, in the same stack and order as [WorkspaceBanners]. The workspace draws
 * the whole stack through that one, so these never show twice.
 *
 * @param[onReload] What the newer data banner's Reload does.
 */
@Composable
internal fun ProjectBanners(
    state: ProjectsModel.State,
    onAction: (ProjectsAction) -> Unit,
    modifier: Modifier = Modifier,
    onReload: () -> Unit = {},
) {
    val banners = listOfNotNull(
        WorkspaceBanner.Conflict.takeIf { state.conflict },
        WorkspaceBanner.UnsavedTheme.takeIf { state.transient && state.storageAvailable },
        WorkspaceBanner.NewerData.takeIf { state.newerData },
    )
    BannerStack(
        banners = banners,
        onAction = { action ->
            when (action) {
                is BannerAction.ResolveConflict -> onAction(ProjectsAction.ResolveConflict(action.keepMine))
                BannerAction.SaveTheme -> onAction(ProjectsAction.SaveShared)
                BannerAction.ReloadHome -> onReload()
                else -> Unit
            }
        },
        modifier = modifier,
    )
}

/**
 * Raises the undo toast for [deleted] once, through [dispatcher]. The kit keeps it up for
 * [ToastDuration.Long], and its Undo hands [onUndo] this deletion, even once another delete has
 * come after it.
 */
@Composable
internal fun UndoToast(
    deleted: DeletedProject,
    dispatcher: Dispatcher<WorkspaceAction>,
    onUndo: (DeletedProject) -> Unit,
) {
    val message = stringResource(Res.string.projects_deleted, deleted.meta.name)
    val undo = stringResource(Res.string.projects_undo)
    LaunchedEffect(deleted, message, undo) {
        if (message.isEmpty() || undo.isEmpty()) return@LaunchedEffect
        val toast = WorkspaceAction.ShowToast(
            message = message,
            actionLabel = undo,
            duration = ToastDuration.Long,
            onAction = { onUndo(deleted) },
        )
        dispatcher.dispatch(toast)
    }
}

private fun problemText(problem: ProjectsProblem): StringResource =
    when (problem) {
        ProjectsProblem.NotOpened -> Res.string.projects_problem_open
        ProjectsProblem.NotCreated -> Res.string.projects_problem_create
        ProjectsProblem.NotDuplicated -> Res.string.projects_problem_duplicate
        ProjectsProblem.NotRenamed -> Res.string.projects_problem_rename
        ProjectsProblem.NotDeleted -> Res.string.projects_problem_delete
        ProjectsProblem.NotDeletedNewer -> Res.string.projects_problem_delete_newer
        ProjectsProblem.NotRestored -> Res.string.projects_problem_restore
        ProjectsProblem.SetAside -> Res.string.projects_problem_set_aside
    }
