package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher

// stub

/**
 * The poster, the seed as the hero with the style, contrast and fine tune rows under it, or the
 * seed strip when [rail] is true (B-303). The shell already paints it in the poster's colors.
 */
@Composable
internal fun PosterPanel(
    state: WorkspaceModel.State,
    rail: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}
