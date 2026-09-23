package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.Dispatcher

/**
 * The color vision the canvas simulates (F-25). Nothing remembers it across a reload.
 */
internal enum class VisionSimulation {
    None,
    Protanopia,
    Deuteranopia,
    Tritanopia,
    Achromatopsia,
}

// stub

/**
 * The preview and its tabs, scrolling out from under [contentPadding] (B-217).
 */
@Composable
internal fun CanvasArea(
    state: WorkspaceModel.State,
    contentPadding: PaddingValues,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}

// stub

/**
 * The floating dock over the canvas with preview mode, device width, inspect and vision, worn in
 * a `DockRegion` (B-217).
 */
@Composable
internal fun CanvasDock(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
}
