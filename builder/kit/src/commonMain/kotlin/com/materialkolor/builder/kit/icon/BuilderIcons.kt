package com.materialkolor.builder.kit.icon

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

// fluent-placeholder

/**
 * One icon set, the glyph a skin draws for each [IconId].
 *
 * Material3 draws the rounded Material icons, and Unstyled and Custom draw Lucide. Fluent draws
 * Lucide too until the Fluent System Icons join the kit.
 */
@Immutable
public interface BuilderIcons {
    /** The glyph for [id]. The same id always gives back the same vector. */
    public operator fun get(id: IconId): ImageVector
}

/**
 * The icon set of the surrounding skin.
 *
 * Static for the same reason as `LocalSkin`, and with no default for the same reason as the
 * tokens. An icon drawn outside a skin would otherwise quietly wear the wrong set.
 */
public val LocalBuilderIcons: ProvidableCompositionLocal<BuilderIcons> = staticCompositionLocalOf {
    error("No BuilderIcons provided")
}
