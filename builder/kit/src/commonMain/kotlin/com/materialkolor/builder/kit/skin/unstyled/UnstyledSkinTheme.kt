package com.materialkolor.builder.kit.skin.unstyled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.builder.kit.icon.LucideIcons
import com.materialkolor.builder.kit.skin.ProvideSkinLocals
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.skin.builderMotion
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.unstyled.MaterialKolorTokens
import com.materialkolor.unstyled.dynamicColors

/**
 * The Unstyled skin, a Compose Unstyled theme whose colour tokens are the chrome scheme.
 *
 * The theme is applied with the colour scheme [isDark] names rather than the system setting, so the
 * builder's own mode switch wins. Its colour transition stays at the default snap, the skin
 * transition already covers the change.
 *
 * @param[scheme] The chrome scheme for the current mode.
 * @param[isDark] Which Unstyled colour scheme to apply.
 * @param[reducedMotion] Whether to provide the reduced motion set instead of the builder tweens.
 * @param[content] The builder.
 */
@Composable
internal fun UnstyledSkinTheme(
    scheme: DynamicScheme,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val theme = remember(scheme) { buildThemeV2 { dynamicColors(scheme) } }
    theme(colorScheme = if (isDark) ColorScheme.Dark else ColorScheme.Light) {
        val tokens = unstyledTokens(StatusColors.of(isDark))
        val motion = remember(reducedMotion) { builderMotion(reducedMotion) }
        ProvideSkinLocals(tokens, motion, LucideIcons, content)
    }
}

/** The builder tokens, read back out of the Unstyled theme so its components and ours agree. */
@Composable
private fun unstyledTokens(status: StatusColors): BuilderTokens {
    val colors = Theme[MaterialKolorTokens.colors]
    val onSurface = colors[MaterialKolorTokens.onSurface]
    val onSurfaceVariant = colors[MaterialKolorTokens.onSurfaceVariant]
    val primary = colors[MaterialKolorTokens.primary]
    val secondary = colors[MaterialKolorTokens.secondary]
    val tertiary = colors[MaterialKolorTokens.tertiary]
    return BuilderTokens(
        canvas = colors[MaterialKolorTokens.surfaceContainerLow],
        panel = colors[MaterialKolorTokens.surface],
        panelRaised = colors[MaterialKolorTokens.surfaceContainerHigh],
        border = colors[MaterialKolorTokens.outlineVariant],
        borderStrong = colors[MaterialKolorTokens.outline],
        textStrong = onSurface,
        textMuted = onSurfaceVariant,
        accent = primary,
        onAccent = colors[MaterialKolorTokens.onPrimary],
        focus = primary,
        codeBackground = colors[MaterialKolorTokens.surfaceContainerHighest],
        codePalette = builderCodePalette(
            plain = onSurface,
            muted = onSurfaceVariant,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            status = status,
        ),
        success = status.success,
        warning = status.warning,
        danger = colors[MaterialKolorTokens.error],
    )
}
