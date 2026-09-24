package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.feature.about.AboutHost
import com.materialkolor.builder.feature.about.HelpHost
import com.materialkolor.builder.feature.canvas.CanvasArea
import com.materialkolor.builder.feature.canvas.CanvasDock
import com.materialkolor.builder.feature.canvas.FullscreenExit
import com.materialkolor.builder.feature.command.CommandHost
import com.materialkolor.builder.feature.command.PasteHost
import com.materialkolor.builder.feature.command.ShortcutFocus
import com.materialkolor.builder.feature.command.ShortcutScope
import com.materialkolor.builder.feature.command.commandReturnFocus
import com.materialkolor.builder.feature.command.rememberShortcuts
import com.materialkolor.builder.feature.export.ExportHost
import com.materialkolor.builder.feature.export.launchCopy
import com.materialkolor.builder.feature.image.ImageHost
import com.materialkolor.builder.feature.image.ImageSeedModel
import com.materialkolor.builder.feature.image.ProvideImageSeeds
import com.materialkolor.builder.feature.picker.PickerHost
import com.materialkolor.builder.feature.poster.ExplainerHost
import com.materialkolor.builder.feature.poster.LocalPosterSheetState
import com.materialkolor.builder.feature.poster.PosterFocus
import com.materialkolor.builder.feature.poster.PosterPanel
import com.materialkolor.builder.feature.poster.shareReturn
import com.materialkolor.builder.feature.projects.ProjectsHost
import com.materialkolor.builder.feature.projects.ShareHost
import com.materialkolor.builder.feature.topbar.LocalSwitcherForm
import com.materialkolor.builder.feature.topbar.TopBarContent
import com.materialkolor.builder.feature.topbar.TopBarControl
import com.materialkolor.builder.feature.topbar.rememberTopBarFocus
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.workspace_copied
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.rememberBottomSheetState
import com.materialkolor.builder.kit.control.rememberBuilderToastHostState
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.shell.ToastRegion
import com.materialkolor.builder.kit.shell.WorkspaceShell
import com.materialkolor.builder.kit.skin.fluent.preloadFluentFace
import com.materialkolor.builder.kit.transition.RevealStyle
import com.materialkolor.builder.kit.transition.SkinTransition
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberDispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
    images: ImageSeedModel = metroViewModel(), // b-311
) {
    val scope = rememberCoroutineScope()
    val toasts = rememberBuilderToastHostState()
    // b-221c
    var manualCopyText by remember { mutableStateOf("") }
    var manualCopyOpen by remember { mutableStateOf(false) }
    var manualCopyFrom by remember { mutableStateOf<FocusRequester?>(null) } // b-221f
    var pickerFrom by remember { mutableStateOf<FocusRequester?>(null) } // b-307
    val shortcutFocus = remember { ShortcutFocus() } // b-315
    // b-406g
    // Read by the dispatcher, which is remembered once, so it follows the window as it resizes.
    val posterMode by rememberUpdatedState(LocalLayout.current.posterMode)

    // Plays the transition's reveal out of the origin, or a crossfade without one, around the change.
    fun reveal(
        origin: Offset?,
        awaitBeforeReveal: suspend () -> Unit = NoFontWait, // b-404a
        change: () -> Unit,
    ) {
        scope.launch { transition.reveal(revealFrom(origin), awaitBeforeReveal, change) }
    }

    val dispatcher = rememberDispatcher<WorkspaceAction> { action ->
        when (action) {
            is WorkspaceAction.Edit -> {
                model.edit(action.change, action.phase)
            }
            is WorkspaceAction.EditWithReveal -> {
                // b-404a
                reveal(action.origin, fontWaitFor(action.change)) { model.edit(action.change, EditPhase.Discrete) }
            }
            WorkspaceAction.Undo -> {
                model.undo()
            }
            WorkspaceAction.Redo -> {
                model.redo()
            }
            // b-508
            is WorkspaceAction.JumpTo -> {
                model.jumpTo(action.cursor)
            }
            // A shuffle crossfades wherever it was pressed (MO-02).
            is WorkspaceAction.Shuffle -> {
                model.drawShuffle()?.let { shuffle -> reveal(origin = null) { model.applyShuffle(shuffle) } }
            }
            is WorkspaceAction.SetLock -> {
                model.setLock(action.lock, action.on)
            }
            is WorkspaceAction.OpenPicker -> {
                pickerFrom = action.returnFocusTo // b-307
                model.openPicker(action.target)
            }
            WorkspaceAction.OpenImagePicker -> {
                scope.launchImagePick(images) // b-311
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
                model.setPosterCollapsed(action.collapsed, posterMode) // b-406g
            }
            is WorkspaceAction.SetFineTuneRowOpen -> {
                model.setFineTuneRowOpen(action.row, action.open)
            }
            is WorkspaceAction.OpenPanel -> {
                shortcutFocus.noteOpen(action.panel) // b-315
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
            // b-311a
            is WorkspaceAction.ShowWithdrawableToast -> {
                val toast = action.toast
                val shown = toasts.show(toast.message, toast.actionLabel, toast.duration, toast.onAction)
                action.onShown { toasts.dismiss(shown) }
            }
            // b-315c
            is WorkspaceAction.SetVisionMenuOpen -> {
                model.setVisionMenuOpen(action.open)
            }
            is WorkspaceAction.HoldGrayscale -> {
                model.holdGrayscale(action.held)
            }
        }
    }

    // b-306c
    // Held here, so the manual copy dialog asks nothing of a copy button that has left the screen.
    val posterFocus = remember { PosterFocus() }
    // b-315
    // The keyboard map lives on the page root, around the text fields it keeps out of the way of.
    ShortcutScope(shortcutFocus) {
        val shortcuts = rememberShortcuts(state, dispatcher, shortcutFocus)
        WorkspaceScreen(
            state = state,
            posterColors = LocalThemeResult.current.poster,
            toasts = toasts,
            dispatcher = dispatcher,
            modifier = modifier.then(shortcuts), // b-315
            posterFocus = posterFocus, // b-306c
            pickerFrom = pickerFrom, // b-307
            shortcutFocus = shortcutFocus, // b-315
        )
    }
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
 * @param[pickerFrom] The Pick button that opened the picker last, which it hands focus back to.
 * @param[shortcutFocus] The page's focus holder, which a panel a shortcut opened hands focus back
 * to, or null where no shortcuts are wired.
 */
@Composable
internal fun WorkspaceScreen(
    state: WorkspaceModel.State,
    posterColors: PosterColors,
    toasts: BuilderToastHostState,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    posterFocus: PosterFocus = remember { PosterFocus() }, // b-306c
    pickerFrom: FocusRequester? = null, // b-307
    shortcutFocus: ShortcutFocus? = null, // b-315
) {
    // b-221c
    // Share and Export hand focus back to the buttons that opened them once they close (AR-09).
    val focus = rememberTopBarFocus()
    // b-406
    // The poster reads where its phone sheet rests, to know which of its sections are in view.
    val sheetState = rememberBottomSheetState()
    WorkspaceShell(
        posterColors = posterColors,
        posterCollapsed = state.posterCollapsed(LocalLayout.current.posterMode), // b-406g
        // b-221f
        // b-311
        poster = { rail ->
            CompositionLocalProvider(LocalPosterSheetState provides sheetState) {
                // b-406
                ProvideImageSeeds(state) { PosterPanel(state, rail, dispatcher, focus = posterFocus) }
            }
        },
        topBar = { TopBarContent(state, dispatcher, focus = focus) }, // b-221c
        canvas = { contentPadding -> CanvasArea(state, contentPadding, dispatcher) },
        dock = { CanvasDock(state, dispatcher) },
        modifier = modifier,
        sheetState = sheetState, // b-406
        // b-217
        fullscreen = state.fullscreen,
        // b-217
        fullscreenExit = { FullscreenExit(dispatcher) },
        overlays = {
            WorkspaceBanners(state, dispatcher) // b-314b
            ExportHost(state, dispatcher, returnFocusTo = focus.requester(TopBarControl.Export)) // b-221c
            ProjectsHost(state, dispatcher, returnFocusTo = posterFocus.projects.returnFocusTo) // b-221f
            ExplainerHost(state, dispatcher, returnFocusTo = posterFocus.why.returnFocusTo) // b-221f
            // b-306c
            val shareReturn = posterFocus.shareReturn(state.panel, focus.requester(TopBarControl.Share))
            ShareHost(state, dispatcher, returnFocusTo = shareReturn)
            // b-315
            // b-406
            // A Medium bar short of room moves Commands into the overflow as a phone does.
            val overflowed = LocalSwitcherForm.current?.overflowed.orEmpty()
            val compact = LocalLayout.current.windowClass == WindowClass.Compact || TopBarControl.Commands in overflowed
            CommandHost(
                state = state,
                dispatcher = dispatcher,
                returnFocusTo = { panel -> commandReturnFocus(panel, shortcutFocus, focus, compact) },
            )
            PickerHost(state, dispatcher, returnFocusTo = pickerFrom) // b-307
            ImageHost(state, dispatcher)
            PasteHost(state, dispatcher) // b-315c
            // b-314
            AboutHost(state, dispatcher, returnFocusTo = focus.requester(TopBarControl.More))
            HelpHost(state, dispatcher, returnFocusTo = focus.requester(TopBarControl.More))
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

// b-404a

/** What a reveal waits on when the change brings in no face the page has not fetched. */
private val NoFontWait: suspend () -> Unit = {}

/**
 * What the reveal round [change] waits on before it captures the old frame.
 *
 * A change that lands the library on Fluent waits on [fluentFace], so the new frame comes in set in
 * Selawik rather than the fallback face. Every other change waits on nothing.
 *
 * @param[fluentFace] Fetches Fluent's face, [preloadFluentFace] outside tests.
 */
internal fun fontWaitFor(
    change: DocumentChange,
    fluentFace: suspend () -> Unit = ::preloadFluentFace,
): suspend () -> Unit = if (change.landsOnFluent()) fluentFace else NoFontWait

/** Whether [this] leaves the document in Fluent, a switch to it or a whole document that uses it. */
private fun DocumentChange.landsOnFluent(): Boolean =
    when (this) {
        is DocumentChange.SetLibrary -> library == Library.Fluent
        is DocumentChange.Replace -> document.library == Library.Fluent
        else -> false
    }

// b-311

/**
 * What Image and Add the image again do. Opens the platform picker and hands what was picked to
 * [images].
 *
 * Browsers only open the picker inside the click, so the pick is the first suspension and it starts
 * before this returns (R-B-302), with no hop through the model before it.
 */
private fun CoroutineScope.launchImagePick(images: ImageSeedModel) {
    launch(start = CoroutineStart.UNDISPATCHED) {
        val handle = images.images.pick() ?: return@launch
        images.take(handle)
    }
}
