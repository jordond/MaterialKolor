package com.materialkolor.builder.feature.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberDispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The export sheet, open while `state.panel` is `Panel.Export` (F-26).
 *
 * Closing it closes the panel, which drops the history entry opening it added, so Back closes it
 * too. The files are only generated while the sheet shows.
 */
@Composable
internal fun ExportHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val model: ExportModel = metroViewModel()
    val export by model.collectAsState()
    val actions = rememberDispatcher<ExportAction> { action -> model.handle(action) }
    ExportSheet(
        visible = state.panel == Panel.Export,
        state = export,
        // From the export's own document, so the options and the target they are kept under never disagree.
        capabilities = remember(export.document) { capabilitiesOf(export.document) },
        outcomeOf = model::outcome,
        clipboard = model.clipboard,
        files = model.files,
        dispatcher = actions,
        workspace = dispatcher,
        modifier = modifier,
    )
}
