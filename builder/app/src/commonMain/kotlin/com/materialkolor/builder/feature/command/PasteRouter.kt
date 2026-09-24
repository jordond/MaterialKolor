package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.image.SeedUndo
import com.materialkolor.builder.feature.share.ShareController
import com.materialkolor.builder.feature.share.sharedNoticeText
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.command_pasted_link
import com.materialkolor.builder.generated.resources.command_pasted_open
import com.materialkolor.builder.generated.resources.command_pasted_seed
import com.materialkolor.builder.generated.resources.workspace_undo
import com.materialkolor.builder.kit.control.ToastDuration
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

// b-315c

/**
 * Text pasted while nothing editable has focus, read by [classify] (F-05, F-32).
 *
 * A color and a share link or code come out of [pasted] for the workspace to act on once. Anything
 * else is left alone, a style name included, and so is every paste while a panel or the dock's Vision
 * menu is open, since it owns the page then. Pasted files are the image seeding's.
 */
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class PasteRouter(
    pastes: PasteInput,
) : ViewModel() {
    private val routed = Channel<PastedText>(Channel.UNLIMITED)

    /** What each pasted color, link or code came to, for the workspace to act on once. */
    val pasted: Flow<PastedText> = routed.receiveAsFlow()

    private var panelOpen = false

    init {
        viewModelScope.launch {
            pastes.pastes.collect { paste ->
                if (paste is Paste.Text && !panelOpen) route(paste.text)
            }
        }
    }

    /** Keep up with the workspace, where an open panel or Vision menu leaves every paste alone. */
    fun follow(panelOpen: Boolean) {
        this.panelOpen = panelOpen
    }

    private fun route(text: String) {
        when (val read = classify(text)) {
            is PastedText.Color, is PastedText.Share -> routed.trySend(read)
            is PastedText.Style, null -> Unit
        }
    }
}

/**
 * Acts on what [PasteRouter] reads out of a paste. It is always composed beside the other overlays,
 * so a paste reaches the workspace with no panel open.
 *
 * A color sets the seed behind a crossfade as one undo entry, with a toast that can undo it (F-05). A
 * share link or code asks first, in a toast whose Open opens the theme it carries and toasts why when
 * it cannot (F-32).
 */
@Composable
internal fun PasteHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    model: PasteRouter = metroViewModel(),
    share: ShareController = metroViewModel(),
) {
    PasteHostContent(
        model = model,
        dispatcher = dispatcher,
        panelOpen = state.panel != null || state.visionMenuOpen, // b-315d
        project = state.projectGeneration,
        document = state.document,
        openShared = share::openShared,
    )
}

/**
 * [PasteHost] with what it reads of the workspace passed in.
 *
 * The Undo on a color's toast only ever undoes that color, the way an image seed's does. It does
 * nothing once the document has moved on, and the toast goes as soon as it does. A color that is
 * already the seed makes no undo entry and so offers no Undo.
 *
 * @param[panelOpen] Whether a panel or the Vision menu is open, which leaves every paste alone.
 * @param[project] The open project's generation, so the Undo stops at a project switch.
 * @param[document] The open document, which tells the Undo when to stop.
 * @param[openShared] Opens the theme a share code carries, and says why when it could not.
 */
@Composable
internal fun PasteHostContent(
    model: PasteRouter,
    dispatcher: Dispatcher<WorkspaceAction>,
    panelOpen: Boolean,
    project: Int,
    document: ThemeDocument,
    openShared: suspend (code: String) -> BootNotice?,
) {
    val undo = remember { PasteUndo() }
    SideEffect {
        model.follow(panelOpen)
        undo.current?.follow(document, project)
    }
    val scope = rememberCoroutineScope()
    val workspace by rememberUpdatedState(PasteWorkspace(dispatcher, document, project, openShared))
    LaunchedEffect(model) {
        model.pasted.collect { pasted ->
            when (pasted) {
                is PastedText.Color -> pasteSeed(pasted.argb, undo) { workspace }
                is PastedText.Share -> offerShared(pasted.code, scope) { workspace }
                is PastedText.Style -> Unit
            }
        }
    }
}

/** Sets the seed to [argb] and puts up its toast, with an Undo that [undo] keeps. */
private suspend fun pasteSeed(
    argb: Argb,
    undo: PasteUndo,
    workspace: () -> PasteWorkspace,
) {
    val before = workspace()
    val change = DocumentChange.SetSeed(argb, SeedSource.Typed)
    val made = change.apply(before.document)
    // A color that changes nothing makes no undo entry, and an Undo would take another one.
    val seedUndo = if (made == before.document) null else SeedUndo(before.document, made, before.project)
    if (seedUndo != null) {
        undo.current?.end()
        undo.current = seedUndo
    }
    before.dispatcher.dispatch(WorkspaceAction.EditWithReveal(change, origin = null))
    val message = getString(Res.string.command_pasted_seed, argb.toHex())
    if (seedUndo == null) {
        before.dispatcher.dispatch(WorkspaceAction.ShowToast(message))
        return
    }
    val toast = WorkspaceAction.ShowToast(
        message = message,
        actionLabel = getString(Res.string.workspace_undo),
        duration = ToastDuration.Long,
        onAction = {
            val now = workspace()
            if (seedUndo.holds(now.document, now.project)) now.dispatcher.dispatch(WorkspaceAction.Undo)
            seedUndo.end()
        },
    )
    before.dispatcher.dispatch(WorkspaceAction.ShowWithdrawableToast(toast, onShown = seedUndo::shown))
}

/** Asks whether to open the theme [code] carries, and opens it in [scope] on Open. */
private suspend fun offerShared(
    code: String,
    scope: CoroutineScope,
    workspace: () -> PasteWorkspace,
) {
    val toast = WorkspaceAction.ShowToast(
        message = getString(Res.string.command_pasted_link),
        actionLabel = getString(Res.string.command_pasted_open),
        duration = ToastDuration.Long,
        onAction = {
            scope.launch {
                val notice = workspace().openShared(code) ?: return@launch
                workspace().dispatcher.dispatch(WorkspaceAction.ShowToast(sharedNoticeText(notice)))
            }
        },
    )
    workspace().dispatcher.dispatch(toast)
}

/** What the host reads of the workspace, kept current for the collector and the toasts' actions. */
private class PasteWorkspace(
    val dispatcher: Dispatcher<WorkspaceAction>,
    val document: ThemeDocument,
    val project: Int,
    val openShared: suspend (code: String) -> BootNotice?,
)

/** The Undo of the newest pasted color's toast, or null once there is none. */
private class PasteUndo {
    var current: SeedUndo? = null
}
