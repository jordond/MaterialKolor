package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.share.ShareController
import com.materialkolor.builder.feature.share.ShareDialog
import com.materialkolor.builder.feature.share.ShareOutcome
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_copied
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The share dialog, open while `state.panel` is [Panel.Share], with the link to the theme as it is
 * now. A copy that lands closes it with a toast, a share that lands closes it quietly.
 */
@Composable
internal fun ShareHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    controller: ShareController = metroViewModel(),
) {
    val link = remember(controller, state.document, state.projectName) {
        controller.link(state.document, state.projectName)
    }
    val copied = stringResource(Res.string.share_copied)
    ShareDialog(
        visible = state.panel == Panel.Share,
        link = link,
        sharesToSheet = controller.sharesToSheet,
        copy = controller::copy,
        share = { url -> controller.share(url, state.projectName) },
        onDone = { outcome ->
            dispatcher.dispatch(WorkspaceAction.ClosePanel)
            if (outcome == ShareOutcome.Copied) dispatcher.dispatch(WorkspaceAction.ShowToast(copied))
        },
        onDismissRequest = { dispatcher.dispatch(WorkspaceAction.ClosePanel) },
        modifier = modifier,
    )
}
