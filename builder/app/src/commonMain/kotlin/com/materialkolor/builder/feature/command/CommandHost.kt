package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher

// stub

/**
 * The command palette and the shortcut cheat sheet, open while `state.panel` is `Panel.Palette` or
 * `Panel.CheatSheet`, and the global shortcuts (B-315).
 */
@Composable
internal fun CommandHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}
