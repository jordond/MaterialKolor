package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.icon.FluentIcons
import com.materialkolor.builder.kit.motion.BuilderDurations
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.PressScale
import com.materialkolor.builder.kit.motion.reducedBuilderMotion
import com.materialkolor.builder.kit.skin.ProvideSkinLocals
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.fluent.toFluentColors
import com.materialkolor.ktx.toneColor
import io.github.composefluent.Colors
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentThemeConfiguration
import io.github.composefluent.animation.FluentDuration
import io.github.composefluent.animation.FluentEasing
import io.github.composefluent.component.ContentDialogHostState

/**
 * The Fluent skin, Fluent's theme over the chrome scheme.
 *
 * Fluent takes only its accent from [scheme], through the primary ramp, and keeps the Windows greys
 * for everything else, which is what Fluent is. The builder tokens are read back out of the same
 * Fluent colours, so a builder widget and a Fluent component beside it agree. Acrylic popups stay
 * off since every overlay draws in the page.
 *
 * It sets the theme with `FluentThemeConfiguration`, the part of `FluentTheme` that only provides
 * the theme. `FluentTheme` also puts a dialog host and an acrylic backdrop round the whole builder
 * and, on desktop, swaps in a text context menu built against an older Compose that crashes on a
 * right click in any field.
 *
 * @param[scheme] The chrome scheme for the current mode.
 * @param[isDark] Which set of greys to use.
 * @param[reducedMotion] Whether to provide the reduced motion set instead of the Fluent one.
 * @param[content] The builder.
 */
@Composable
internal fun FluentSkinTheme(
    scheme: DynamicScheme,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = remember(scheme) { scheme.toFluentColors() }
    val tokens = remember(colors, scheme, isDark) { fluentTokens(colors, scheme, isDark) }
    val motion = remember(reducedMotion) { if (reducedMotion) reducedBuilderMotion() else FluentBuilderMotion() }
    FluentChrome(colors) {
        ProvideSkinLocals(tokens, motion, FluentIcons, content)
    }
}

@OptIn(ExperimentalFluentApi::class)
@Composable
private fun FluentChrome(
    colors: Colors,
    content: @Composable () -> Unit,
) {
    // The builder never asks Fluent for a dialog, so this host stays empty.
    val dialogs = remember { ContentDialogHostState() }
    FluentThemeConfiguration(
        colors = colors,
        typography = rememberFluentTypography(),
        useAcrylicPopup = false,
        contentDialogHostState = dialogs,
        content = content,
    )
}

/**
 * The builder tokens off Fluent's own colour groups.
 *
 * Most Fluent inks and fills are translucent, drawn over Mica. The tokens are laid over the ground
 * they sit on here so they come out solid, the way every other skin's are.
 */
internal fun fluentTokens(
    colors: Colors,
    scheme: DynamicScheme,
    isDark: Boolean,
): BuilderTokens {
    val status = StatusColors.of(isDark)
    val background = colors.background
    val stroke = colors.stroke
    val ink = colors.text.text
    val canvas = background.solid.base
    val panel = background.layer.default.compositeOver(canvas)
    val textStrong = ink.primary.compositeOver(panel)
    val textMuted = ink.secondary.compositeOver(panel)
    val accent = colors.fillAccent.default
    val codeTone = if (isDark) DarkCodeTone else LightCodeTone
    return BuilderTokens(
        canvas = canvas,
        panel = panel,
        panelRaised = background.solid.quaternary,
        border = stroke.divider.default.compositeOver(panel),
        borderStrong = stroke.controlStrong.default.compositeOver(panel),
        textStrong = textStrong,
        textMuted = textMuted,
        accent = accent,
        onAccent = colors.text.onAccent.primary,
        focus = stroke.focus.outer.compositeOver(panel),
        codeBackground = background.solid.secondary,
        codePalette = builderCodePalette(
            plain = textStrong,
            muted = textMuted,
            primary = accent,
            secondary = scheme.secondaryPalette.toneColor(codeTone),
            tertiary = scheme.tertiaryPalette.toneColor(codeTone),
            status = status,
        ),
        success = status.success,
        warning = status.warning,
        danger = colors.system.critical,
        scrim = background.smoke.default,
        iconSize = 16.dp,
    )
}

/**
 * The tones the code viewer draws the secondary and tertiary ramps at, where Fluent puts its accent.
 */
private const val LightCodeTone = 40
private const val DarkCodeTone = 80

/**
 * How long the reveal out of the library switcher takes in Fluent, halfway between Fluent's medium
 * and long steps. None of Fluent's own steps land inside the reveal's 400 to 450 ms band.
 */
private const val FluentRevealMillis = 417

/**
 * Fluent's own timings and curves for everything that moves.
 *
 * The durations stay the builder's, the same as Material's, so a test or a screenshot run reads
 * one set for every skin. Each spec takes a Fluent step and the curve Fluent's own components use
 * for that kind of move.
 */
private class FluentBuilderMotion : BuilderMotion {
    override val durations: BuilderDurations = BuilderDurations()

    override val pressScale: Float = PressScale

    override fun <T> spatial(): FiniteAnimationSpec<T> =
        tween(FluentDuration.ShortDuration, easing = FluentEasing.PointToPointEasing)

    override fun <T> effects(): FiniteAnimationSpec<T> =
        tween(FluentDuration.QuickDuration, easing = FluentEasing.FastInvokeEasing)

    override fun <T> slide(): FiniteAnimationSpec<T> =
        tween(FluentDuration.MediumDuration, easing = FluentEasing.PointToPointEasing)

    override fun <T> reveal(): FiniteAnimationSpec<T> =
        tween(FluentRevealMillis, easing = FluentEasing.FastInvokeEasing)

    override fun <T> panelEnter(): FiniteAnimationSpec<T> =
        tween(FluentDuration.MediumDuration, easing = FluentEasing.FastInvokeEasing)

    override fun <T> panelExit(): FiniteAnimationSpec<T> =
        tween(FluentDuration.ShortDuration, easing = FluentEasing.FastDismissEasing)

    override fun <T> popover(): FiniteAnimationSpec<T> =
        tween(FluentDuration.ShortDuration, easing = FluentEasing.FastInvokeEasing)

    override fun <T> press(): FiniteAnimationSpec<T> =
        tween(FluentDuration.QuickDuration, easing = FluentEasing.FastInvokeEasing)

    override fun <T> crossfade(): FiniteAnimationSpec<T> =
        tween(FluentDuration.ShortDuration, easing = FluentEasing.FadeInFadeOutEasing)
}
