package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderType

// pf-3

/**
 * The skin a Fluent warm-up uses.
 */
private val FluentWarmUpSkin = Skin(library = Library.Fluent, expressive = false)

/**
 * Dresses [content] in the Fluent skin for a warm-up that never shows.
 *
 * The first switch to Fluent draws shadows, clips and shapes the page has not drawn before, and the
 * browser compiles a GPU program for each of them inside that one frame. The app composes its Fluent
 * workspace through this once, in idle time after the first frame, and `SkinTransition.warmUp` draws
 * it under the live frame, so those programs are ready before anyone switches.
 *
 * Fluent's own text takes the builder's body face here instead of Selawik, so a warm-up fetches
 * nothing and the site still fetches Selawik the first time Fluent really shows. Everything else is
 * the Fluent skin as a switch builds it, with the colours of [result] in the mode [isDark] names.
 */
@Composable
public fun FluentWarmUpTheme(
    result: ThemeResult,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val standIn = LocalBuilderType.current.body.fontFamily ?: FontFamily.Default
    CompositionLocalProvider(
        LocalSkin provides FluentWarmUpSkin,
        LocalFluentStandInFace provides standIn,
    ) {
        FluentSkinTheme(result.chrome(isDark), isDark, LocalReducedMotion.current, content)
    }
}
