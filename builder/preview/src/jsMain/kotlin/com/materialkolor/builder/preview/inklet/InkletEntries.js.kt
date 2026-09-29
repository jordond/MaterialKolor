package com.materialkolor.builder.preview.inklet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.material.MaterialAppEntry
import com.materialkolor.builder.preview.material.MaterialGalleryEntry
import com.materialkolor.builder.preview.split.PaneSpec

@Composable
internal actual fun InkletPaneTheme(content: @Composable () -> Unit) {
    content()
}

@Composable
internal actual fun InkletGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
) {
    MaterialGalleryEntry(spec, state, modifier)
}

@Composable
internal actual fun InkletAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier,
) {
    MaterialAppEntry(spec, state, deviceWidth, modifier)
}
