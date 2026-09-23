package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.icon.FluentIcons
import com.materialkolor.builder.kit.skin.ProvideSkinLocals
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.skin.builderMotion
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette

// fluent-placeholder

/**
 * The Fluent skin, for now a stand in drawn the way Windows draws its chrome.
 *
 * Fluent is not a kit dependency yet, so this lays Windows greys under the accent the chrome's
 * primary ramp gives, at the tones Windows puts its accent on. B-403 replaces it with a real
 * `FluentTheme`.
 *
 * @param[scheme] The chrome scheme for the current mode.
 * @param[isDark] Which set of greys and accent tones to use.
 * @param[reducedMotion] Whether to provide the reduced motion set instead of the builder tweens.
 * @param[content] The builder.
 */
@Composable
internal fun FluentSkinTheme(
    scheme: DynamicScheme,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val tokens = remember(scheme, isDark) { fluentTokens(scheme, isDark) }
    val motion = remember(reducedMotion) { builderMotion(reducedMotion) }
    ProvideSkinLocals(tokens, motion, FluentIcons, content)
}

/** The Windows greys have no hue at all, unlike a scheme's neutrals. */
private val WindowsGrey = TonalPalette.fromHueAndChroma(hue = 0.0, chroma = 0.0)

private fun fluentTokens(
    scheme: DynamicScheme,
    isDark: Boolean,
): BuilderTokens {
    val tones = if (isDark) FluentTones.Dark else FluentTones.Light
    val status = StatusColors.of(isDark)
    val textStrong = WindowsGrey.toneColor(tones.textStrong)
    val textMuted = WindowsGrey.toneColor(tones.textMuted)
    val accent = scheme.primaryPalette.toneColor(tones.accent)
    return BuilderTokens(
        canvas = WindowsGrey.toneColor(tones.canvas),
        panel = WindowsGrey.toneColor(tones.panel),
        panelRaised = WindowsGrey.toneColor(tones.panelRaised),
        border = WindowsGrey.toneColor(tones.border),
        borderStrong = WindowsGrey.toneColor(tones.borderStrong),
        textStrong = textStrong,
        textMuted = textMuted,
        accent = accent,
        onAccent = scheme.primaryPalette.toneColor(tones.onAccent),
        focus = textStrong,
        codeBackground = WindowsGrey.toneColor(tones.codeBackground),
        codePalette = builderCodePalette(
            plain = textStrong,
            muted = textMuted,
            primary = accent,
            secondary = scheme.secondaryPalette.toneColor(tones.accent),
            tertiary = scheme.tertiaryPalette.toneColor(tones.accent),
            status = status,
        ),
        success = status.success,
        warning = status.warning,
        danger = scheme.errorPalette.toneColor(tones.accent),
        iconSize = 16.dp,
    )
}

/** The tones Windows draws each part of its chrome at, per mode. */
private enum class FluentTones(
    val canvas: Int,
    val panel: Int,
    val panelRaised: Int,
    val border: Int,
    val borderStrong: Int,
    val textStrong: Int,
    val textMuted: Int,
    val accent: Int,
    val onAccent: Int,
    val codeBackground: Int,
) {
    Light(
        canvas = 95,
        panel = 100,
        panelRaised = 98,
        border = 85,
        borderStrong = 40,
        textStrong = 10,
        textMuted = 40,
        accent = 40,
        onAccent = 100,
        codeBackground = 96,
    ),
    Dark(
        canvas = 10,
        panel = 16,
        panelRaised = 22,
        border = 34,
        borderStrong = 70,
        textStrong = 98,
        textMuted = 72,
        accent = 80,
        onAccent = 20,
        codeBackground = 8,
    ),
}
