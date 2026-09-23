package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.workspace_redo
import com.materialkolor.builder.generated.resources.workspace_undo
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.shell.TopBarRegion
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

// stub

/**
 * The top bar. For now it holds undo and redo only. The library switcher, the Expressive
 * suggestion, share, export and the overflow menu come with B-216b.
 */
@Composable
internal fun TopBarContent(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    TopBarRegion(modifier) {
        BuilderIconButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.Undo) },
            icon = IconId.Undo,
            contentDescription = stringResource(Res.string.workspace_undo),
            enabled = state.history.canUndo,
        )
        BuilderIconButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.Redo) },
            icon = IconId.Redo,
            contentDescription = stringResource(Res.string.workspace_redo),
            enabled = state.history.canRedo,
        )
    }
}
