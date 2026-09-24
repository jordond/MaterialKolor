package com.materialkolor.builder.kit.skin.unstyled

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.builder.kit.icon.LucideIcons
import com.materialkolor.builder.kit.skin.ProvideSkinLocals
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.skin.builderMotion
import com.materialkolor.builder.kit.skin.headless.ScrimAlpha
import com.materialkolor.builder.kit.skin.headless.UnstyledHighlightAlpha
import com.materialkolor.builder.kit.skin.headless.UnstyledSelectedAlpha
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.brandFontFamily
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.unstyled.MaterialKolorTokens
import com.materialkolor.unstyled.dynamicColors
import kotlinx.coroutines.launch

/**
 * The Unstyled skin, a Compose Unstyled theme whose colour tokens are the chrome scheme.
 *
 * The theme is applied with the colour scheme [isDark] names rather than the system setting, so the
 * builder's own mode switch wins. Its colour transition stays at the default snap, the skin
 * transition already covers the change. The default text style keeps Unstyled's sizes and weights
 * but wears the brand face, since the default family on wasm is a fallback font fetched from the
 * network.
 *
 * The theme also hands out the skin's own indication. Left unset, Compose Unstyled falls back to a
 * legacy indication that foundation's `clickable` refuses, so any plain clickable would throw.
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
    val brand = brandFontFamily()
    val theme = remember(scheme, brand) {
        buildThemeV2 {
            dynamicColors(scheme)
            defaultTextStyle = TextStyle.Default.copy(fontFamily = brand)
            defaultIndication = UnstyledIndication
        }
    }
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
    val canvas = colors[MaterialKolorTokens.surfaceContainerLow]
    return BuilderTokens(
        canvas = canvas,
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
        scrim = canvas.copy(alpha = ScrimAlpha),
        iconSize = 16.dp,
    )
}

/**
 * The ground a plain clickable wears under Unstyled, the skin's ink faint on hover and a touch
 * stronger while pressed, the same veils its menus put under a row.
 */
internal data object UnstyledIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        UnstyledIndicationNode(interactionSource)
}

private class UnstyledIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var hovered = false
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            val hovers = mutableListOf<HoverInteraction.Enter>()
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> hovers += interaction
                    is HoverInteraction.Exit -> hovers -= interaction.enter
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                hovered = hovers.isNotEmpty()
                pressed = presses.isNotEmpty()
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = when {
            pressed -> UnstyledSelectedAlpha
            hovered -> UnstyledHighlightAlpha
            else -> return
        }
        drawRect(currentValueOf(LocalBuilderTokens).textStrong.copy(alpha = alpha))
    }
}
