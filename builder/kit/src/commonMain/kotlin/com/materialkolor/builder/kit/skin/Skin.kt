package com.materialkolor.builder.kit.skin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The libraries the kit can draw its controls in.
 *
 * The builder's own chrome is always Material 3. Custom is here for the Custom preview panes, whose
 * apps and galleries are built from kit controls.
 */
public enum class SkinLibrary {
    /**
     * Material 3 components, standard or Expressive.
     */
    Material3,

    /**
     * The builder's own identity, drawn from the Custom target's slots.
     */
    Custom,
}

/**
 * Which library the kit's controls use right now.
 *
 * Expressive is a flag rather than a library of its own because it only swaps the Material3 theme
 * and a handful of components inside the Material3 skin.
 *
 * @property[library] The library whose look and components the controls dispatch to.
 * @property[expressive] Whether the Material3 skin uses its expressive theme, shapes and motion.
 */
@Immutable
public data class Skin(
    public val library: SkinLibrary,
    public val expressive: Boolean,
)

/**
 * The skin the surrounding tree is drawn in.
 *
 * Static on purpose. The builder's chrome keeps one skin for its whole life, and a Custom preview
 * pane provides its own once, so nothing reads it while it changes.
 */
public val LocalSkin: ProvidableCompositionLocal<Skin> = staticCompositionLocalOf {
    error("No Skin provided")
}
