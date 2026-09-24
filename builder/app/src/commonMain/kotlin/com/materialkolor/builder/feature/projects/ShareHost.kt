package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.domain.model.ThemeDocument
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
 * now. A copy that lands closes it with a toast, a share that lands closes it quietly. The link is
 * only worked out while the dialog is open.
 *
 * @param[returnFocusTo] The button that opened the dialog, which gets focus back once it closes (AR-09).
 */
@Composable
internal fun ShareHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    controller: ShareController = metroViewModel(),
) {
    val visible = state.panel == Panel.Share
    // Worked out only while the dialog is open, so a drag with it closed encodes nothing. A closing
    // dialog keeps the link it showed.
    val shown = remember(controller) { ShownLink(controller) }
    val link = if (visible) shown.of(state.document, state.projectName) else shown.link
    val copied = stringResource(Res.string.share_copied)
    ShareDialog(
        visible = visible,
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
        returnFocusTo = returnFocusTo,
    )
}

/**
 * The link the dialog shows, worked out again only when the theme or its name changes.
 */
private class ShownLink(
    private val controller: ShareController,
) {
    private var document: ThemeDocument? = null
    private var projectName: String? = null

    /** The last link worked out, or null when there is none yet or the theme would not fit. */
    var link: String? = null
        private set

    fun of(
        document: ThemeDocument,
        projectName: String,
    ): String? {
        if (document == this.document && projectName == this.projectName) return link
        this.document = document
        this.projectName = projectName
        link = controller.link(document, projectName)
        return link
    }
}
