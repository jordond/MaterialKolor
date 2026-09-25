package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.headless.unstyledOverlayStyle
import com.materialkolor.builder.kit.skin.material.materialOverlayStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The style set the headless overlays use in [library].
 *
 * It sits with the dispatchers rather than with the headless styles, since it reaches into every
 * skin's package and the skins build on the headless styles.
 */
@Composable
internal fun overlayStyle(library: Library): OverlayStyle =
    when (library) {
        Library.Material3 -> materialOverlayStyle()
        Library.Unstyled -> unstyledOverlayStyle(LocalBuilderTokens.current)
        Library.Fluent -> fluentOverlayStyle(LocalBuilderTokens.current) // fluent-placeholder
        Library.Custom -> customOverlayStyle(LocalBuilderTokens.current)
    }
