package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The Unstyled components gallery.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
internal fun UnstyledGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) // stub
}
