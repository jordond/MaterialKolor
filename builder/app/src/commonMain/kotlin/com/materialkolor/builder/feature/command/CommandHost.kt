package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.feature.topbar.TopBarControl
import com.materialkolor.builder.feature.topbar.TopBarFocus
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The command palette and the shortcut cheat sheet, open while `state.panel` is `Panel.Palette` or
 * `Panel.CheatSheet`. The global shortcuts sit on the page root, see [rememberShortcuts].
 *
 * @param[returnFocusTo] Where each of its panels hands focus once it closes (AR-09).
 */
@Composable
internal fun CommandHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: (Panel) -> FocusRequester? = { null },
    shortcuts: ShortcutsModel = metroViewModel(),
) {
    CheatSheet(
        visible = state.panel == Panel.CheatSheet,
        singleKeys = state.preferences.singleKeyShortcuts,
        onSingleKeysChange = shortcuts::setSingleKeys,
        onDismissRequest = { dispatcher.dispatch(WorkspaceAction.ClosePanel) },
        modifier = modifier,
        returnFocusTo = returnFocusTo(Panel.CheatSheet),
    )
}

/**
 * Where [panel] hands focus back, the holder when a shortcut in [keys] opened it, else the top bar
 * control that did, Commands for the palette on a wide window and the overflow button otherwise.
 */
internal fun commandReturnFocus(
    panel: Panel,
    keys: ShortcutFocus?,
    topBar: TopBarFocus,
    compact: Boolean,
): FocusRequester? {
    val opener = if (panel == Panel.Palette && !compact) TopBarControl.Commands else TopBarControl.More
    val otherwise = topBar.requester(opener)
    return keys?.returnFocusFor(panel, otherwise) ?: otherwise
}
