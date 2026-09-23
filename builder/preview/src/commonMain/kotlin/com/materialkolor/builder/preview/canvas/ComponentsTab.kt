package com.materialkolor.builder.preview.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.preview.custom.CustomGalleryEntry
import com.materialkolor.builder.preview.fluent.FluentGalleryEntry
import com.materialkolor.builder.preview.material.MaterialGalleryEntry
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.unstyled.UnstyledGalleryEntry

/**
 * The Components tab, a curated gallery of the library [LocalSkin] names.
 *
 * Call it inside a [PreviewPane] for [spec], once per copy of a split.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
public fun ComponentsTab(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    when (skin.library) {
        Library.Material3 -> MaterialGalleryEntry(spec, state, skin.expressive, modifier)
        Library.Unstyled -> UnstyledGalleryEntry(spec, state, modifier)
        Library.Fluent -> FluentGalleryEntry(spec, state, modifier)
        Library.Custom -> CustomGalleryEntry(spec, state, modifier)
    }
}
