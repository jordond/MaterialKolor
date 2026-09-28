package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.custom.CustomPaneTheme
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Custom sample app, the Trips travel app every library's App tab shows.
 *
 * The app is built from the builder's own kit controls under [CustomPaneTheme], so they use the
 * pane's Custom slots and not the chrome's. Nothing in it opens a popup, a dialog or an overlay.
 * The error banner is drawn in place, and the note field gets the galleries' text toolbar that
 * never shows, with right presses swallowed, so it opens no context menu either.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun CustomAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    CustomPaneTheme(spec.result.customSlots, spec.isDark, LocalReducedMotion.current) {
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            CustomTripsApp(
                state = state,
                deviceWidth = deviceWidth,
                colors = rememberCustomColors(spec),
                modifier = modifier.fillMaxSize().gallerySwallowRightPresses(),
            )
        }
    }
}
