package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.feature.about.AboutHost
import com.materialkolor.builder.feature.canvas.CanvasArea
import com.materialkolor.builder.feature.canvas.CanvasDock
import com.materialkolor.builder.feature.command.CommandHost
import com.materialkolor.builder.feature.export.ExportHost
import com.materialkolor.builder.feature.image.ImageHost
import com.materialkolor.builder.feature.picker.PickerHost
import com.materialkolor.builder.feature.poster.PosterPanel
import com.materialkolor.builder.feature.projects.ProjectsHost
import com.materialkolor.builder.feature.projects.ShareHost
import com.materialkolor.builder.feature.topbar.TopBarContent
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.workspace_copied
import com.materialkolor.builder.generated.resources.workspace_copy_failed
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.rememberBuilderToastHostState
import com.materialkolor.builder.kit.shell.ToastRegion
import com.materialkolor.builder.kit.shell.WorkspaceShell
import com.materialkolor.builder.kit.transition.RevealStyle
import com.materialkolor.builder.kit.transition.SkinTransition
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberDispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * The workspace, wired to its model.
 *
 * Discrete changes that repaint the theme play [transition]'s reveal, and everything a drag sends
 * goes straight through, since the dispatcher drops nothing.
 */
@Composable
internal fun WorkspaceScreen(
    transition: SkinTransition,
    modifier: Modifier = Modifier,
    model: WorkspaceModel = metroViewModel(),
) {
    val state by model.collectAsState()
    val scope = rememberCoroutineScope()
    val toasts = rememberBuilderToastHostState()

    // Plays the transition's reveal out of the origin, or a crossfade without one, around the change.
    fun reveal(
        origin: Offset?,
        change: () -> Unit,
    ) {
        scope.launch { transition.reveal(revealFrom(origin), change = change) }
    }

    val dispatcher = rememberDispatcher<WorkspaceAction> { action ->
        when (action) {
            is WorkspaceAction.Edit -> {
                model.edit(action.change, action.phase)
            }
            is WorkspaceAction.EditWithReveal -> {
                reveal(action.origin) { model.edit(action.change, EditPhase.Discrete) }
            }
            WorkspaceAction.Undo -> {
                model.undo()
            }
            WorkspaceAction.Redo -> {
                model.redo()
            }
            // A shuffle crossfades wherever it was pressed (MO-02).
            is WorkspaceAction.Shuffle -> {
                model.drawShuffle()?.let { shuffle -> reveal(origin = null) { model.applyShuffle(shuffle) } }
            }
            is WorkspaceAction.SetLock -> {
                model.setLock(action.lock, action.on)
            }
            is WorkspaceAction.OpenPicker -> {
                model.openPicker(action.target)
            }
            // B-311 opens the platform picker from here.
            WorkspaceAction.OpenImagePicker -> {
                Unit
            }
            is WorkspaceAction.SetPreviewTab -> {
                model.setPreviewTab(action.tab)
            }
            is WorkspaceAction.SetPreviewMode -> {
                reveal(action.origin) { model.setPreviewMode(action.mode) }
            }
            is WorkspaceAction.SetSplitFraction -> {
                model.setSplitFraction(action.fraction)
            }
            is WorkspaceAction.SetDeviceWidth -> {
                model.setDeviceWidth(action.width)
            }
            is WorkspaceAction.SetVision -> {
                model.setVision(action.vision)
            }
            is WorkspaceAction.SetInspect -> {
                model.setInspect(action.on)
            }
            WorkspaceAction.ToggleFullscreen -> {
                model.toggleFullscreen()
            }
            is WorkspaceAction.SetPosterCollapsed -> {
                model.setPosterCollapsed(action.collapsed)
            }
            is WorkspaceAction.SetFineTuneRowOpen -> {
                model.setFineTuneRowOpen(action.row, action.open)
            }
            is WorkspaceAction.OpenPanel -> {
                model.openPanel(action.panel)
            }
            WorkspaceAction.ClosePanel -> {
                model.closePanel()
            }
            is WorkspaceAction.SetExportPref -> {
                model.setExportPref(action.target, action.update)
            }
            is WorkspaceAction.CopyText -> {
                scope.launch {
                    val copied = model.copyText(action.text)
                    val message = if (copied) {
                        getString(Res.string.workspace_copied, action.label)
                    } else {
                        getString(Res.string.workspace_copy_failed)
                    }
                    toasts.show(message)
                }
            }
            is WorkspaceAction.ShowToast -> {
                toasts.show(action.message, action.actionLabel, action.duration, action.onAction)
            }
            is WorkspaceAction.SetAppearance -> {
                model.setAppearance(action.appearance)
            }
            is WorkspaceAction.SetMotionOverride -> {
                model.setMotionOverride(action.motion)
            }
            is WorkspaceAction.DismissHint -> {
                model.dismissHint(action.id)
            }
            WorkspaceAction.DismissExpressiveSuggestion -> {
                model.dismissExpressiveSuggestion()
            }
        }
    }

    WorkspaceScreen(
        state = state,
        posterColors = LocalThemeResult.current.poster,
        toasts = toasts,
        dispatcher = dispatcher,
        modifier = modifier,
    )
}

/**
 * The workspace laid out by the shell, the poster, the top bar, the canvas with its dock, and the
 * panels and toasts over them.
 */
@Composable
internal fun WorkspaceScreen(
    state: WorkspaceModel.State,
    posterColors: PosterColors,
    toasts: BuilderToastHostState,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    WorkspaceShell(
        posterColors = posterColors,
        posterCollapsed = state.preferences.posterCollapsed,
        poster = { rail -> PosterPanel(state, rail, dispatcher) },
        topBar = { TopBarContent(state, dispatcher) },
        canvas = { contentPadding -> CanvasArea(state, contentPadding, dispatcher) },
        dock = { CanvasDock(state, dispatcher) },
        modifier = modifier,
        overlays = {
            ExportHost(state, dispatcher)
            ProjectsHost(state, dispatcher)
            ShareHost(state, dispatcher)
            CommandHost(state, dispatcher)
            PickerHost(state, dispatcher)
            ImageHost(state, dispatcher)
            AboutHost(state, dispatcher)
            ToastRegion(toasts)
        },
    )
}

/** A circle out of [origin], or a crossfade in place when there is none. */
private fun revealFrom(origin: Offset?): RevealStyle =
    if (origin == null) {
        RevealStyle.Crossfade
    } else {
        RevealStyle.Circle(origin)
    }
