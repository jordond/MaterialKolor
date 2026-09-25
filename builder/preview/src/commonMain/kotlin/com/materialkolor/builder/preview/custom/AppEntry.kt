package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.custom.CustomPaneTheme
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Custom sample app, the cafe ordering app.
 *
 * The app is built from the builder's own kit controls under [CustomPaneTheme], so they use the
 * pane's Custom slots and not the chrome's. Nothing in it opens a popup, a dialog or an overlay. A
 * confirmation is drawn in place.
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
        CafeApp(state, deviceWidth, rememberCafeColors(spec), modifier.fillMaxSize())
    }
}
