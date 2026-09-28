package com.materialkolor.builder.kit.skin.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.icon.MaterialIcons
import com.materialkolor.builder.kit.motion.BuilderDurations
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.PressScale
import com.materialkolor.builder.kit.motion.reducedBuilderMotion
import com.materialkolor.builder.kit.motion.tweenBuilderMotion
import com.materialkolor.builder.kit.skin.ProvideSkinLocals
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.CodePalette
import com.materialkolor.builder.kit.token.brandFontFamily
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.material3.toColorScheme

/**
 * The Material3 skin, a `MaterialTheme` over the chrome scheme.
 *
 * Expressive swaps in `MaterialExpressiveTheme` with the expressive motion scheme. Everything
 * Material draws tints from [scheme], and the builder tokens are read back out of the theme so a
 * builder widget and a Material component beside it agree. Both flavours set Material's type
 * scale in the brand face at the library's own sizes and weights, since the default family on wasm
 * is a fallback font fetched from the network.
 *
 * @param[scheme] The chrome scheme for the current mode.
 * @param[expressive] Whether to use the expressive theme and motion.
 * @param[reducedMotion] Whether to provide the reduced motion set instead of the Material one.
 * @param[content] The builder.
 */
@Composable
internal fun MaterialSkinTheme(
    scheme: DynamicScheme,
    expressive: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(scheme) { scheme.toColorScheme() }
    val isDark = scheme.isDark
    val brand = brandFontFamily()
    val typography = remember(brand) { Typography(fontFamily = brand) }
    if (expressive) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = typography,
        ) {
            MaterialSkinLocals(isDark, reducedMotion, content)
        }
    } else {
        MaterialTheme(colorScheme = colorScheme, typography = typography) {
            MaterialSkinLocals(isDark, reducedMotion, content)
        }
    }
}

@Composable
private fun MaterialSkinLocals(
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val tokens = remember(colorScheme, isDark) { colorScheme.builderTokens(isDark) }
    val motion = remember(motionScheme, reducedMotion) {
        if (reducedMotion) reducedBuilderMotion() else MaterialBuilderMotion(motionScheme)
    }
    ProvideSkinLocals(tokens, motion, MaterialIcons, content)
}

/**
 * The builder tokens read back out of a Material scheme.
 *
 * The code sits on a dark ground in both modes. In light mode that is the inverse surface, so its
 * inks come from the inverse and fixed dim roles and the dark status inks. In dark mode it is the
 * lowest surface container and the inks are the scheme's own.
 */
internal fun ColorScheme.builderTokens(isDark: Boolean): BuilderTokens {
    val status = StatusColors.of(isDark)
    return BuilderTokens(
        canvas = surfaceContainer,
        panel = surfaceContainerLow,
        panelRaised = surfaceContainerHigh,
        border = outlineVariant,
        borderStrong = outline,
        textStrong = onSurface,
        textMuted = onSurfaceVariant,
        accent = primary,
        onAccent = onPrimary,
        focus = secondary,
        codeBackground = if (isDark) surfaceContainerLowest else inverseSurface,
        codePalette = codePalette(isDark),
        success = status.success,
        warning = status.warning,
        danger = error,
        scrim = scrim.copy(alpha = MaterialScrimAlpha),
        iconSize = 20.dp,
    )
}

private fun ColorScheme.codePalette(isDark: Boolean): CodePalette =
    if (isDark) {
        builderCodePalette(
            plain = onSurface,
            muted = onSurfaceVariant,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            status = StatusColors.of(isDark = true),
        )
    } else {
        builderCodePalette(
            plain = inverseOnSurface,
            muted = outlineVariant,
            primary = primaryFixedDim,
            secondary = secondaryFixedDim,
            tertiary = tertiaryFixedDim,
            status = StatusColors.of(isDark = true),
        )
    }

/**
 * Material's own scrim opacity.
 */
private const val MaterialScrimAlpha = 0.32f

/**
 * Material's own springs for everything that moves, and the builder's fenced tweens for the reveal,
 * the crossfade and the panels, which the skin transition times itself against.
 */
private class MaterialBuilderMotion(
    private val scheme: MotionScheme,
) : BuilderMotion {
    private val tweens = tweenBuilderMotion()

    override val durations: BuilderDurations = BuilderDurations()

    override val pressScale: Float = PressScale

    override fun <T> spatial(): FiniteAnimationSpec<T> = scheme.defaultSpatialSpec()

    override fun <T> effects(): FiniteAnimationSpec<T> = scheme.defaultEffectsSpec()

    override fun <T> slide(): FiniteAnimationSpec<T> = scheme.slowSpatialSpec()

    override fun <T> reveal(): FiniteAnimationSpec<T> = tweens.reveal()

    override fun <T> panelEnter(): FiniteAnimationSpec<T> = tweens.panelEnter()

    override fun <T> panelExit(): FiniteAnimationSpec<T> = tweens.panelExit()

    override fun <T> popover(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()

    override fun <T> press(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()

    override fun <T> crossfade(): FiniteAnimationSpec<T> = tweens.crossfade()
}
