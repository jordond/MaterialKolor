package com.materialkolor.builder.preview.unstyled

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Unstyled sample app, the Trips travel app drawn with Compose Unstyled primitives.
 *
 * The app lays itself out for [deviceWidth] and fills whatever size it is given. Scaling the frame
 * to fit the pane, down to 0.6, and scrolling past that belong to the canvas, not the app.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun UnstyledAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    UnstyledTripsApp(state, deviceWidth, modifier)
}
