package com.materialkolor.builder.preview.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Material 3 sample app, the Trips travel app.
 *
 * Expressive shows the same app, since the switch only changes the colors and the theme around it.
 * The app lays itself out for [deviceWidth] and fills whatever size it is given. On a phone Trips
 * shows the trip list and then the open trip, and on a tablet or desktop it shows them side by side
 * next to a rail. Scaling the frame to fit the pane, down to 0.6, and scrolling past that belong to
 * the canvas, not the app.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun MaterialAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    TripsApp(state, deviceWidth, modifier)
}
