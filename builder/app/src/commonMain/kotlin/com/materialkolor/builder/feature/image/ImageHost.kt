package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher

// stub

/**
 * Taking a seed from an image, whether picked, dropped or pasted, and the candidates it gives
 * (B-311).
 */
@Composable
internal fun ImageHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}
