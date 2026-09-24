package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.SeedSource
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
        seedSource = state.document.seedSource,
        modifier = modifier,
    )
}

/**
 * [ImageHost] with what it reads of the workspace passed in.
 *
 * @param[picking] Whether the color picker is open, which leaves drops and pastes alone.
 * @param[project] The open project's generation, so a new project drops the image in memory.
 * @param[seedSource] Where the document's seed came from, which tells the skeleton when to go.
 */
@Composable
internal fun ImageHostContent(
    model: ImageSeedModel,
    dispatcher: Dispatcher<WorkspaceAction>,
    picking: Boolean,
    project: Int,
    seedSource: SeedSource,
    modifier: Modifier = Modifier,
) {
    SideEffect { model.follow(picking, project) }
    val seeds by model.collectAsState()
    val arriving = seeds.arriving
    LaunchedEffect(arriving, seedSource) {
        if (arriving?.lands != null && arriving.lands == seedSource) model.landed(arriving)
    }
    val current by rememberUpdatedState(dispatcher)
    LaunchedEffect(model) {
        model.results.collect { result -> current.dispatchResult(result) }
    }
    val dragging by model.images.dragging.collectAsState()
    if (dragging) DropOverlay(modifier)
}

/** Sends the workspace what [result] asks for, the seed and its undo toast or the error toast. */
private suspend fun Dispatcher<WorkspaceAction>.dispatchResult(result: ImageSeedResult) {
    when (result) {
        is ImageSeedResult.Seeded -> {
            dispatch(WorkspaceAction.EditWithReveal(result.change, origin = null))
            val name = result.source.name
            val message = if (name.isBlank()) {
                getString(Res.string.image_seeded)
            } else {
                getString(Res.string.image_seeded_named, name)
            }
            val toast = WorkspaceAction.ShowToast(
                message = message,
                actionLabel = getString(Res.string.image_undo),
                duration = ToastDuration.Long,
                onAction = { dispatch(WorkspaceAction.Undo) },
            )
            dispatch(toast)
        }
        ImageSeedResult.Unsupported -> {
            dispatch(WorkspaceAction.ShowToast(getString(Res.string.image_unsupported)))
        }
    }
}
