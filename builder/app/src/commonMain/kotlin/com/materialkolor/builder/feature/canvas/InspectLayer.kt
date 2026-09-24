package com.materialkolor.builder.feature.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.preview.inspect.InspectActions
import com.materialkolor.builder.preview.inspect.InspectOverlay
import dev.stateholder.dispatcher.Dispatcher

/** The tabs Inspect works on, the two that show the sample screens in light and dark. */
private val InspectTabs: Set<PreviewTab> = setOf(PreviewTab.App, PreviewTab.Components)

/**
 * The layer around the canvas tab body that Inspect works through (F-44), which also saves the
 * split handle when a pointer lets go.
 *
 * Inspect is live on App and Components while `state.inspect` is on, and the pinned card's actions
 * go out through [dispatcher]. Pin this role pins the role in the mode it was inspected in, Show on
 * ramp opens the Palettes tab, and Jump to key color opens the poster at its core colors row. Every
 * pointer release over the canvas asks [preview] to save a handle that moved, without the wait a
 * keyboard move gets.
 */
@Composable
internal fun InspectLayer(
    state: WorkspaceModel.State,
    preview: PreviewSplit,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pinEnabled = state.capabilities[Control.RolePins] is ControlState.Enabled
    val actions = remember(dispatcher, pinEnabled) {
        InspectActions(
            pinEnabled = pinEnabled,
            onPin = { role, isDark, argb ->
                val mode = if (isDark) PinMode.Dark else PinMode.Light
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetPin(role, mode, argb), EditPhase.Discrete))
            },
            // B-308b swaps in the Palettes tab's own way to show a color on its ramp.
            onShowOnRamp = { _, _ -> dispatcher.dispatch(WorkspaceAction.SetPreviewTab(PreviewTab.Palettes)) },
            onJumpToKeyColor = { _ ->
                dispatcher.dispatch(WorkspaceAction.SetPosterCollapsed(collapsed = false))
                dispatcher.dispatch(WorkspaceAction.SetFineTuneRowOpen(FineTuneRow.CoreColors, open = true))
            },
            onLeave = { dispatcher.dispatch(WorkspaceAction.SetInspect(on = false)) },
        )
    }
    InspectOverlay(
        on = state.inspect && state.view.tab in InspectTabs,
        result = LocalThemeResult.current,
        shown = preview.shown,
        split = preview.split,
        actions = actions,
        modifier = modifier.saveOnRelease(preview),
        scene = state.view.tab,
        content = content,
    )
}

/** Tell [preview] each time a pointer lets go over the canvas, before anything under it sees the release. */
private fun Modifier.saveOnRelease(preview: PreviewSplit): Modifier =
    pointerInput(preview) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { change -> change.changedToUpIgnoreConsumed() }) preview.release()
            }
        }
    }
