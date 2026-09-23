package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher

// stub

/**
 * The projects drawer, open while `state.panel` is `Panel.Projects` (B-310).
 */
@Composable
internal fun ProjectsHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}
