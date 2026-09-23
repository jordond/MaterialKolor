package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher

// stub

/**
 * The share sheet, open while `state.panel` is `Panel.Share` (B-310).
 */
@Composable
internal fun ShareHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}
