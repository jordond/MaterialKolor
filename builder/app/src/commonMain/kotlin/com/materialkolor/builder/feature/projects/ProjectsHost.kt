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
import com.materialkolor.builder.core.data.DeletedProject
import com.materialkolor.builder.feature.share.SharedLinkBanner
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.projects_deleted
import com.materialkolor.builder.generated.resources.projects_problem_create
import com.materialkolor.builder.generated.resources.projects_problem_delete
import com.materialkolor.builder.generated.resources.projects_problem_open
import com.materialkolor.builder.generated.resources.projects_problem_rename
import com.materialkolor.builder.generated.resources.projects_problem_restore
import com.materialkolor.builder.generated.resources.projects_undo
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.ToastDuration
import com.materialkolor.builder.kit.layout.MediumBreakpoint
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The projects drawer, open while `state.panel` is [Panel.Projects], and the banners about the open
 * project, which show whatever panel is open.
 *
 * Opening or starting a project closes the drawer. A delete raises an undo toast in the workspace's
 * [toasts] that stays until the undo runs out, and anything storage turns down raises a toast of its
 * own.
 */
@Composable
internal fun ProjectsHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    toasts: BuilderToastHostState,
    modifier: Modifier = Modifier,
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
    )
    ProjectBanners(projects, model::handle, modifier)
    projects.pendingDeletion?.let { deleted ->
        UndoToast(deleted, toasts) { model.handle(ProjectsAction.UndoDelete) }
    }
    projects.problem?.let { problem ->
        val message = stringResource(problemText(problem))
        LaunchedEffect(problem, message) {
            if (message.isEmpty()) return@LaunchedEffect
            toasts.show(message)
            model.handle(ProjectsAction.ProblemShown)
        }
    }
}

/**
 * The banners that hang off the open project, below the top bar. A clash with another tab comes
 * first, then a theme from a link that is not saved yet. Without storage there is nowhere to save
 * it, so that banner stays away and the drawer says why.
 */
@Composable
private fun ProjectBanners(
    state: ProjectsModel.State,
    onAction: (ProjectsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offerSave = state.transient && state.storageAvailable
    if (!state.conflict && !offerSave) return
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
        }
    }
}

/**
 * The undo toast for [deleted], up for as long as it stays in the composition. The model lets the
 * deletion go after [UNDO_WINDOW_MILLIS], which takes the toast down with it.
 */
@Composable
private fun UndoToast(
    deleted: DeletedProject,
    toasts: BuilderToastHostState,
    onUndo: () -> Unit,
) {
    val message = stringResource(Res.string.projects_deleted, deleted.meta.name)
    val undo = stringResource(Res.string.projects_undo)
    LaunchedEffect(deleted, toasts) {
        val toast = toasts.show(message, actionLabel = undo, duration = ToastDuration.Indefinite, onAction = onUndo)
        try {
            awaitCancellation()
        } finally {
            toasts.dismiss(toast)
        }
    }
}

private fun problemText(problem: ProjectsProblem): StringResource =
    when (problem) {
        ProjectsProblem.NotOpened -> Res.string.projects_problem_open
        ProjectsProblem.NotCreated -> Res.string.projects_problem_create
        ProjectsProblem.NotRenamed -> Res.string.projects_problem_rename
        ProjectsProblem.NotDeleted -> Res.string.projects_problem_delete
        ProjectsProblem.NotRestored -> Res.string.projects_problem_restore
    }
