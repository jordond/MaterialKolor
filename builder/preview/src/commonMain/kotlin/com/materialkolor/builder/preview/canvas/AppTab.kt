package com.materialkolor.builder.preview.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.preview.custom.CustomAppEntry
import com.materialkolor.builder.preview.fluent.FluentAppEntry
import com.materialkolor.builder.preview.material.MaterialAppEntry
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.unstyled.UnstyledAppEntry

/**
 * The App tab, the sample app of the library [LocalSkin] names.
 *
 * Call it inside a [PreviewPane] for [spec], once per copy of a split.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[modifier] Applied to the app.
 */
@Composable
public fun AppTab(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    when (skin.library) {
        Library.Material3 -> MaterialAppEntry(spec, state, deviceWidth, modifier)
        Library.Unstyled -> UnstyledAppEntry(spec, state, deviceWidth, modifier)
        Library.Fluent -> FluentAppEntry(spec, state, deviceWidth, modifier)
        Library.Custom -> CustomAppEntry(spec, state, deviceWidth, modifier)
    }
}
