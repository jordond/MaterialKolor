package com.materialkolor.builder.preview.fluent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Fluent sample app, the Trips travel app drawn with compose-fluent's own components in the
 * pane's Fluent theme.
 *
 * It is the same app the Material 3 tab shows, with the same copy, data and state keys, so moving
 * between libraries keeps what the user did. On a phone Trips shows the trip list and then the open
 * trip, and on a tablet or desktop a navigation view holds them side by side.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun FluentAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    TripsApp(state, deviceWidth, modifier)
}
