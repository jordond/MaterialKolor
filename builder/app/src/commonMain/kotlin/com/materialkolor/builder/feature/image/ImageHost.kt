package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_seeded
import com.materialkolor.builder.generated.resources.image_seeded_named
import com.materialkolor.builder.generated.resources.image_undo
import com.materialkolor.builder.generated.resources.image_unsupported
import com.materialkolor.builder.kit.control.ToastDuration
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.getString

/**
 * Taking a seed from an image, whether picked, dropped or pasted, and the drop overlay (F-08).
 *
 * It is always composed, so what [model] makes of an image reaches the workspace even with no
 * panel open. A seed lands behind a crossfade as one undo entry, with a toast that can undo it,
 * and a file that is not an image says so in a toast and leaves the theme alone.
 */
@Composable
internal fun ImageHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    model: ImageSeedModel = metroViewModel(), // b-311
) {
    ImageHostContent(
        model = model,
        dispatcher = dispatcher,
        picking = state.panel == Panel.Picker,
        project = state.projectGeneration,
        document = state.document,
        modifier = modifier,
    )
}

/**
 * [ImageHost] with what it reads of the workspace passed in.
 *
 * The Undo on a seed's toast only ever undoes that seed. It does nothing once the document has
 * moved on, and the toast goes as soon as it does. A seed that changes nothing, such as the same
 * image landing twice, makes no undo entry and so offers no Undo.
 *
 * @param[picking] Whether the color picker is open, which leaves drops and pastes alone.
 * @param[project] The open project's generation, so a new project drops the image in memory.
 * @param[document] The open document, which tells the skeleton when to go and the Undo when to stop.
 */
@Composable
internal fun ImageHostContent(
    model: ImageSeedModel,
    dispatcher: Dispatcher<WorkspaceAction>,
    picking: Boolean,
    project: Int,
    document: ThemeDocument,
    modifier: Modifier = Modifier,
) {
    val undo = remember { UndoSlot() }
    SideEffect {
        model.follow(picking, project)
        undo.current?.follow(document, project)
    }
    val seeds by model.collectAsState()
    val arriving = seeds.arriving
    val seedSource = document.seedSource
    LaunchedEffect(arriving, seedSource) {
        if (arriving?.lands != null && arriving.lands == seedSource) model.landed(arriving)
    }
    val workspace by rememberUpdatedState(Workspace(dispatcher, document, project))
    LaunchedEffect(model) {
        model.results.collect { result -> dispatchResult(result, undo) { workspace } }
    }
    val dragging by model.images.dragging.collectAsState()
    // b-311a
    // The picker owns the window while it is open, and a drop then goes nowhere.
    if (dragging && !picking) DropOverlay(modifier)
}

/**
 * Sends the workspace what [result] asks for, the seed and its undo toast or the error toast. The
 * seed's Undo goes in [undo], and [workspace] reads the workspace as it is by the time it acts.
 */
private suspend fun dispatchResult(
    result: ImageSeedResult,
    undo: UndoSlot,
    workspace: () -> Workspace,
) {
    val dispatcher = workspace().dispatcher
    when (result) {
        is ImageSeedResult.Seeded -> {
            val before = workspace()
            val made = result.change.apply(before.document)
            // A seed that changes nothing makes no undo entry, and an Undo would take another one.
            // b-311c
            val seedUndo = if (made == before.document) null else SeedUndo(before.document, made, before.project)
            if (seedUndo != null) {
                undo.current?.end()
                undo.current = seedUndo
            }
            dispatcher.dispatch(WorkspaceAction.EditWithReveal(result.change, origin = null))
            val name = result.source.name
            val message = if (name.isBlank()) {
                getString(Res.string.image_seeded)
            } else {
                getString(Res.string.image_seeded_named, name)
            }
            if (seedUndo == null) {
                dispatcher.dispatch(WorkspaceAction.ShowToast(message))
                return
            }
            val toast = WorkspaceAction.ShowToast(
                message = message,
                actionLabel = getString(Res.string.image_undo),
                duration = ToastDuration.Long,
                onAction = {
                    val now = workspace()
                    if (seedUndo.holds(now.document, now.project)) now.dispatcher.dispatch(WorkspaceAction.Undo)
                    seedUndo.end()
                },
            )
            dispatcher.dispatch(WorkspaceAction.ShowWithdrawableToast(toast, onShown = seedUndo::shown))
        }
        ImageSeedResult.Unsupported -> {
            dispatcher.dispatch(WorkspaceAction.ShowToast(getString(Res.string.image_unsupported)))
        }
    }
}

/**
 * What the host reads of the workspace, kept current for the collector and the toast's Undo.
 */
private class Workspace(
    val dispatcher: Dispatcher<WorkspaceAction>,
    val document: ThemeDocument,
    val project: Int,
)

/**
 * The Undo of the newest seed's toast, or null once there is none.
 */
private class UndoSlot {
    var current: SeedUndo? = null
}
