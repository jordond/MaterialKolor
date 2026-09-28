package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ImageBitmap
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.validate.projectNameProblem
import com.materialkolor.builder.feature.share.ShareCardLoader
import com.materialkolor.builder.feature.share.ShareController
import com.materialkolor.builder.feature.share.ShareDialog
import com.materialkolor.builder.feature.share.ShareName
import com.materialkolor.builder.feature.share.ShareOutcome
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_copied
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The share dialog, open while `state.panel` is [Panel.Share], with the link to the theme as it is
 * now. A copy that lands closes it with a toast, a share that lands closes it quietly. The link is
 * only worked out while the dialog is open.
 *
 * The name field keeps a draft while the dialog is open, and the link and its card carry the draft
 * as soon as it makes a good name, or else the last good one. The project takes the name when the
 * field commits it. Copy link and Share rename it to the name in the link they send and closing the
 * dialog to a good draft the field has not committed yet, so the project always ends up with the
 * name in the link it sent.
 *
 * @param[returnFocusTo] The button that opened the dialog, which gets focus back once it closes.
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
    val scope = rememberCoroutineScope()
    // Each opening starts from the saved name, as does a rename that lands while it is open.
    var draft by remember(visible, state.projectName) { mutableStateOf(state.projectName) }
    var linkName by remember(visible, state.projectName) { mutableStateOf(state.projectName) }
    var notSaved by remember(visible) { mutableStateOf(false) }
    val transient by controller.transient.collectAsState()

    // Worked out only while the dialog is open, so a drag with it closed encodes nothing. A closing
    // dialog keeps the link it showed.
    val shown = remember(controller) { ShownLink(controller::link) }
    val shownCard = remember(controller) { ShownLink(controller::cardLink) }
    val link = if (visible) shown.of(state.document, linkName) else shown.link
    val cardLink = if (visible) shownCard.of(state.document, linkName) else shownCard.link
    val cards = remember(controller, scope) { CardLoaders(scope, controller::card) }
    val loader = cards.of(visible)
    LaunchedEffect(loader, cardLink, visible) { if (visible && cardLink != null) loader.show(cardLink) }
    val card by loader.state.collectAsState()

    // The name a rename is on its way to, so a commit and a click right after it rename once.
    val renaming = remember(controller) { RenameInFlight() }

    fun rename(name: String) {
        if (name == renaming.name) return
        renaming.name = name
        notSaved = false
        scope.launch {
            val saved = controller.rename(name)
            renaming.name = null
            notSaved = !saved
        }
    }

    // Copy link and Share rename to the name in the link they send, and closing to a good draft.
    fun renameTo(name: String) {
        if (!transient) nameToRename(name, state.projectName)?.let(::rename)
    }

    val copied = stringResource(Res.string.share_copied)
    ShareDialog(
        visible = visible,
        link = link,
        sharesToSheet = controller.sharesToSheet,
        // The rename only launches, so the platform call is still the first thing to suspend.
        copy = { url ->
            renameTo(linkName)
            controller.copy(url)
        },
        share = { url ->
            renameTo(linkName)
            controller.share(url, linkName)
        },
        onDone = { outcome ->
            dispatcher.dispatch(WorkspaceAction.ClosePanel)
            if (outcome == ShareOutcome.Copied) dispatcher.dispatch(WorkspaceAction.ShowToast(copied))
        },
        onDismissRequest = {
            renameTo(draft)
            dispatcher.dispatch(WorkspaceAction.ClosePanel)
        },
        document = state.document,
        card = card,
        name = ShareName(
            value = state.projectName,
            transient = transient,
            notSaved = notSaved,
            onDraftChange = { text ->
                draft = text
                if (projectNameProblem(text) == null) linkName = text.trim()
            },
            onCommit = ::renameTo,
            onSaveToProjects = controller::saveToProjects,
        ),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
    )
}

/**
 * The name to rename the project called [current] to, given the field's [draft], or null when
 * the draft is not a good name or is the name the project already has.
 */
internal fun nameToRename(
    draft: String,
    current: String,
): String? {
    if (projectNameProblem(draft) != null) return null
    val name = draft.trim()
    return name.takeIf { it != current }
}

/**
 * A link the dialog shows, built by [build] again only when the theme or its name changes.
 */
private class ShownLink(
    private val build: (ThemeDocument, String) -> String?,
) {
    private var document: ThemeDocument? = null
    private var projectName: String? = null

    /**
     * The last link worked out, or null when there is none yet or the theme would not fit.
     */
    var link: String? = null
        private set

    fun of(
        document: ThemeDocument,
        projectName: String,
    ): String? {
        if (document == this.document && projectName == this.projectName) return link
        this.document = document
        this.projectName = projectName
        link = build(document, projectName)
        return link
    }
}

/**
 * The name a rename is on its way to, or null when none is.
 */
private class RenameInFlight {
    var name: String? = null
}

/**
 * A fresh [ShareCardLoader] for each time the dialog opens, kept while it closes so the closing
 * dialog still shows its card.
 */
private class CardLoaders(
    private val scope: CoroutineScope,
    private val load: suspend (String) -> ImageBitmap?,
) {
    private var open = false
    private var loader = ShareCardLoader(scope, load)

    fun of(visible: Boolean): ShareCardLoader {
        if (visible && !open) loader = ShareCardLoader(scope, load)
        open = visible
        return loader
    }
}
