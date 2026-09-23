package com.materialkolor.builder.kit.skin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.materialkolor.builder.domain.Library

/**
 * Which library the builder is wearing right now.
 *
 * Expressive is a flag rather than a fifth library because it only swaps the Material3 theme and a
 * handful of components inside the Material3 skin. It changes nothing about the document.
 *
 * @property[library] The library whose look and components the chrome dispatches to.
 * @property[expressive] Whether the Material3 skin uses its expressive theme, shapes and motion.
 */
@Immutable
public data class Skin(
    public val library: Library,
    public val expressive: Boolean,
)

/**
 * The skin the surrounding tree is drawn in.
 *
 * Static on purpose. A skin switch rebuilds the subtree anyway, and it does it behind the
 * transition snapshot, so nobody sees the rebuild.
 */
public val LocalSkin: ProvidableCompositionLocal<Skin> = staticCompositionLocalOf {
    error("No Skin provided")
}
