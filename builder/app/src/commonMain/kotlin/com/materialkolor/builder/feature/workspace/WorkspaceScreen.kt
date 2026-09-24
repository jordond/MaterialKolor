package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.feature.about.AboutHost
import com.materialkolor.builder.feature.canvas.CanvasArea
import com.materialkolor.builder.feature.canvas.CanvasDock
import com.materialkolor.builder.feature.canvas.FullscreenExit
import com.materialkolor.builder.feature.command.CommandHost
import com.materialkolor.builder.feature.export.ExportHost
import com.materialkolor.builder.feature.export.launchCopy
import com.materialkolor.builder.feature.image.ImageHost
import com.materialkolor.builder.feature.picker.PickerHost
import com.materialkolor.builder.feature.poster.ExplainerHost
import com.materialkolor.builder.feature.poster.PosterFocus
import com.materialkolor.builder.feature.poster.PosterPanel
import com.materialkolor.builder.feature.poster.shareReturn
import com.materialkolor.builder.feature.projects.ProjectsHost
import com.materialkolor.builder.feature.projects.ShareHost
import com.materialkolor.builder.feature.topbar.TopBarContent
import com.materialkolor.builder.feature.topbar.TopBarControl
import com.materialkolor.builder.feature.topbar.rememberTopBarFocus
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.workspace_copied
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.rememberBuilderToastHostState
import com.materialkolor.builder.kit.shell.ToastRegion
import com.materialkolor.builder.kit.shell.WorkspaceShell
import com.materialkolor.builder.kit.transition.RevealStyle
import com.materialkolor.builder.kit.transition.SkinTransition
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberDispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * The workspace, wired to its model.
 *
 * Discrete changes that repaint the theme play [transition]'s reveal, and everything a drag sends
 * goes straight through, since the dispatcher drops nothing. A copy from the poster writes the
 * clipboard inside the click, then says Copied in a toast or, when the browser refused, opens the
 * text to copy by hand (F-26).
 *
 * @param[state] The model's state, collected once at the root, which resolves the theme from it.
 */
@Composable
internal fun WorkspaceScreen(
    // b-221c
    state: WorkspaceModel.State,
    transition: SkinTransition,
    modifier: Modifier = Modifier,
    model: WorkspaceModel = metroViewModel(),
) {
    val scope = rememberCoroutineScope()
    val toasts = rememberBuilderToastHostState()
    // b-221c
    var manualCopyText by remember { mutableStateOf("") }
    var manualCopyOpen by remember { mutableStateOf(false) }
    var manualCopyFrom by remember { mutableStateOf<FocusRequester?>(null) } // b-221f

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
            WorkspaceAction.OpenImagePicker -> {
                // b-221c
                // Nothing yet. B-311 opens the platform picker from here.
            }
            is WorkspaceAction.SetPreviewTab -> {
                model.setPreviewTab(action.tab)
            }
            // b-217aa
            // The dock's switch only slides the handle (MO-03). A switch with an origin still reveals.
            is WorkspaceAction.SetPreviewMode -> {
                val origin = action.origin
                if (origin == null) {
                    model.setPreviewMode(action.mode)
                } else {
                    reveal(origin) { model.setPreviewMode(action.mode) }
                }
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
            // b-221c
            is WorkspaceAction.CopyText -> {
                scope.launchCopy(model.clipboard, action.text) { result ->
                    if (result.isSuccess) {
                        toasts.show(getString(Res.string.workspace_copied, action.label))
                    } else {
                        manualCopyText = action.text
                        manualCopyFrom = action.returnFocusTo // b-221f
                        manualCopyOpen = true
                    }
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
            // b-308
            is WorkspaceAction.ShowOnRamp -> {
                model.showOnRamp(action.target)
            }
            // b-306b
            is WorkspaceAction.SetColorAnimation -> {
                model.setColorAnimation(action.target, action.on)
            }
            is WorkspaceAction.SetColorAnimationDuration -> {
                model.setColorAnimationDuration(action.target, action.durationMs)
            }
        }
    }

    // b-306c
    // Held here, so the manual copy dialog asks nothing of a copy button that has left the screen.
    val posterFocus = remember { PosterFocus() }
    WorkspaceScreen(
        state = state,
        posterColors = LocalThemeResult.current.poster,
        toasts = toasts,
        dispatcher = dispatcher,
        modifier = modifier,
        posterFocus = posterFocus, // b-306c
    )
    // b-221c
    ManualCopyDialog(
        visible = manualCopyOpen,
        text = manualCopyText,
        onDismissRequest = { manualCopyOpen = false },
        returnFocusTo = posterFocus.returnFocusFor(manualCopyFrom), // b-306c
    )
}

/**
 * The workspace laid out by the shell, the poster, the top bar, the canvas with its dock, and the
 * panels and toasts over them.
 *
 * @param[posterFocus] The poster buttons that Projects, the explainer and the manual copy dialog
 * hand focus back to once they close (AR-09).
 */
@Composable
internal fun WorkspaceScreen(
    state: WorkspaceModel.State,
    posterColors: PosterColors,
    toasts: BuilderToastHostState,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    posterFocus: PosterFocus = remember { PosterFocus() }, // b-306c
) {
    // b-221c
    // Share and Export hand focus back to the buttons that opened them once they close (AR-09).
    val focus = rememberTopBarFocus()
    WorkspaceShell(
        posterColors = posterColors,
        posterCollapsed = state.preferences.posterCollapsed,
        poster = { rail -> PosterPanel(state, rail, dispatcher, focus = posterFocus) }, // b-221f
        topBar = { TopBarContent(state, dispatcher, focus = focus) }, // b-221c
        canvas = { contentPadding -> CanvasArea(state, contentPadding, dispatcher) },
        dock = { CanvasDock(state, dispatcher) },
        modifier = modifier,
        // b-217
        fullscreen = state.fullscreen,
        // b-217
        fullscreenExit = { FullscreenExit(dispatcher) },
        overlays = {
            ExportHost(state, dispatcher, returnFocusTo = focus.requester(TopBarControl.Export)) // b-221c
            ProjectsHost(state, dispatcher, returnFocusTo = posterFocus.projects.returnFocusTo) // b-221f
            ExplainerHost(state, dispatcher, returnFocusTo = posterFocus.why.returnFocusTo) // b-221f
            // b-306c
            val shareReturn = posterFocus.shareReturn(state.panel, focus.requester(TopBarControl.Share))
            ShareHost(state, dispatcher, returnFocusTo = shareReturn)
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
