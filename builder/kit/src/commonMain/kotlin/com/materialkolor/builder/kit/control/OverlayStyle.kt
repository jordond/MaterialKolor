package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.material.materialOverlayStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The style set the headless overlays use in [library].
 *
 * It sits with the dispatchers rather than with the headless styles, since it reaches into every
 * skin's package and the skins build on the headless styles.
 */
@Composable
internal fun overlayStyle(library: SkinLibrary): OverlayStyle =
    when (library) {
        SkinLibrary.Material3 -> materialOverlayStyle()
        SkinLibrary.Custom -> customOverlayStyle(LocalBuilderTokens.current)
    }
