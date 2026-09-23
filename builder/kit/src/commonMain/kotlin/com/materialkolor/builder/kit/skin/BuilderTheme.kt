package com.materialkolor.builder.kit.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.headless.OverlayHost
import com.materialkolor.builder.kit.icon.BuilderIcons
import com.materialkolor.builder.kit.icon.LocalBuilderIcons
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.reducedBuilderMotion
import com.materialkolor.builder.kit.motion.tweenBuilderMotion
import com.materialkolor.builder.kit.skin.custom.CustomSkinTheme
import com.materialkolor.builder.kit.skin.fluent.FluentSkinTheme
import com.materialkolor.builder.kit.skin.material.MaterialSkinTheme
import com.materialkolor.builder.kit.skin.unstyled.UnstyledSkinTheme
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.CodePalette
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.token.rememberBuilderType
import com.materialkolor.palettes.TonalPalette

/**
 * Dresses [content] in [skin], coloured from [result].
 *
 * Every skin draws from the chrome schemes of [result], which floor contrast at the standard level
 * and leave pins and AMOLED out, so the builder stays readable whatever the document does (P7). No
 * skin generates anything. Each one provides [LocalSkin], the builder's type, its tokens, its
 * motion and its icons.
 *
 * @param[skin] The library and flavour to wear.
 * @param[result] The resolved document, read on the UI thread only like every result.
 * @param[isDark] Which mode of the chrome to draw.
 * @param[reducedMotion] Swap every skin's motion for the reduced set (F-37).
 * @param[content] The builder.
 */
@Composable
public fun BuilderTheme(
    skin: Skin,
    result: ThemeResult,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSkin provides skin,
        LocalBuilderType provides rememberBuilderType(),
        LocalReducedMotion provides reducedMotion,
    ) {
        // b-219
        OverlayHost {
            when (skin.library) {
                Library.Material3 -> MaterialSkinTheme(result.chrome(isDark), skin.expressive, reducedMotion, content)
                Library.Unstyled -> UnstyledSkinTheme(result.chrome(isDark), isDark, reducedMotion, content)
                Library.Fluent -> FluentSkinTheme(result.chrome(isDark), isDark, reducedMotion, content)
                Library.Custom -> CustomSkinTheme(result.chromeCustomSlots, isDark, reducedMotion, content)
            }
        }
    }
}

/** Hands a skin's tokens, motion and icons to [content]. */
@Composable
internal fun ProvideSkinLocals(
    tokens: BuilderTokens,
    motion: BuilderMotion,
    icons: BuilderIcons,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBuilderTokens provides tokens,
        LocalBuilderMotion provides motion,
        LocalBuilderIcons provides icons,
        content = content,
    )
}

/** The builder's own tweens, or the reduced set when the user asked for less motion. */
internal fun builderMotion(reducedMotion: Boolean): BuilderMotion =
    if (reducedMotion) reducedBuilderMotion() else tweenBuilderMotion()

/**
 * The success and warning inks, which no library scheme has a role for.
 *
 * They sit on fixed hues so a green always means passing, and on the tone every skin's error role
 * uses, so they read on a panel in either mode.
 */
internal class StatusColors(
    val success: Color,
    val warning: Color,
) {
    companion object {
        private val SuccessPalette = TonalPalette.fromHueAndChroma(hue = 145.0, chroma = 48.0)
        private val WarningPalette = TonalPalette.fromHueAndChroma(hue = 70.0, chroma = 60.0)

        private val Light = StatusColors(
            success = Color(SuccessPalette.tone(40)),
            warning = Color(WarningPalette.tone(40)),
        )
        private val Dark = StatusColors(
            success = Color(SuccessPalette.tone(80)),
            warning = Color(WarningPalette.tone(80)),
        )

        fun of(isDark: Boolean): StatusColors = if (isDark) Dark else Light
    }
}

/**
 * A code palette cut from a skin's own accents.
 *
 * Keywords and TOML tables take the primary ink, types and annotations the tertiary, functions
 * and keys the secondary. Literals take the status inks so a string reads the same in every skin.
 */
internal fun builderCodePalette(
    plain: Color,
    muted: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    status: StatusColors,
): CodePalette =
    CodePalette(
        colors = mapOf(
            TokenKind.Keyword to primary,
            TokenKind.Type to tertiary,
            TokenKind.Function to secondary,
            TokenKind.Parameter to plain,
            TokenKind.StringLiteral to status.success,
            TokenKind.NumberLiteral to status.warning,
            TokenKind.ColorLiteral to status.warning,
            TokenKind.Comment to muted,
            TokenKind.Annotation to tertiary,
            TokenKind.Punctuation to muted,
            TokenKind.TomlTable to primary,
            TokenKind.TomlKey to secondary,
        ),
        plain = plain,
    )
