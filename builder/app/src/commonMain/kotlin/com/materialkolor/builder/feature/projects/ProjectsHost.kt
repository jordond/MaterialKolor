package com.materialkolor.builder.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.core.data.DeletedProject
import com.materialkolor.builder.feature.share.SharedLinkBanner
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
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
import com.materialkolor.builder.kit.layout.MediumBreakpoint
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The projects drawer, open while `state.panel` is [Panel.Projects], and the banners about the open
 * project, which show whatever panel is open.
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
        returnFocusTo = returnFocusTo, // b-221f
    )
    ProjectBanners(projects, model::handle, modifier)
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

/**
 * The banners that hang off the open project, below the top bar. A clash with another tab comes
 * first, then a theme that is not saved yet, then data a newer build saved. Without storage there is
 * nowhere to save the theme, so that banner stays away and the drawer says why.
 */
@Composable
internal fun ProjectBanners(
    state: ProjectsModel.State,
    onAction: (ProjectsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offerSave = state.transient && state.storageAvailable
    if (!state.conflict && !offerSave && !state.newerData) return
    val spacing = LocalBuilderTokens.current.spacing
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Two section gaps clear the top bar.
                .padding(top = spacing.section + spacing.section, start = spacing.medium, end = spacing.medium)
                .widthIn(max = MediumBreakpoint)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (state.conflict) {
                ConflictBanner(onResolve = { keepMine -> onAction(ProjectsAction.ResolveConflict(keepMine)) })
            }
            if (offerSave) SharedLinkBanner(onSave = { onAction(ProjectsAction.SaveShared) })
            if (state.newerData) NewerDataBanner()
        }
    }
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
