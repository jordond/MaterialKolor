package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Material 3 sample app, the plant care app, or the photo app when expressive.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[expressive] Whether to show the Expressive flavour instead.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun MaterialAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    expressive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) // stub
}
