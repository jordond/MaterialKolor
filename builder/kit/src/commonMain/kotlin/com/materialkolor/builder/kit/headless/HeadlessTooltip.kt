package com.materialkolor.builder.kit.headless

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.isOverlayShown
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.popoverExit
import com.materialkolor.builder.kit.skin.headless.rememberOverlayVisibility
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlin.math.max

/**
 * A short label over [content], shown while the pointer rests on it and while it has keyboard focus.
 *
 * Focus shows it as readily as hover, so nothing it says is only there for a mouse (AR-03). The
 * pointer can move onto the label without it going away (WCAG 1.4.13). Esc hides it until the
 * pointer and focus have both left, and it never takes focus itself.
 *
 * @param[text] The label.
 * @param[style] The skin's overlay style.
 * @param[modifier] Applied to the box around [content].
 * @param[content] The anchor, usually an icon button.
 */
@Composable
internal fun HeadlessTooltip(
    text: String,
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    val wanted = hovered || focused
    LaunchedEffect(wanted) { if (!wanted) hidden = false }
    val visible = wanted && !hidden
    Box(
        modifier = modifier
            .hoverable(interaction)
            .onFocusChanged { state -> focused = state.hasFocus }
            .onKeyEvent { event ->
                val escape = visible && event.type == KeyEventType.KeyDown && event.key == Key.Escape
                if (escape) hidden = true
                escape
            },
    ) {
        content()
        TooltipPopup(visible, text, style, interaction)
    }
}

@Composable
private fun TooltipPopup(
    visible: Boolean,
    text: String,
    style: OverlayStyle,
    interaction: MutableInteractionSource,
) {
    val state = rememberOverlayVisibility(visible)
    if (!state.isOverlayShown(visible)) return
    val tokens = LocalBuilderTokens.current
    val gap = with(LocalDensity.current) { tokens.spacing.extraSmall.roundToPx() }
    val provider = remember(gap) { TooltipPositionProvider(gap) }
    Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false)) {
        AnimatedVisibility(visibleState = state, enter = popoverEnter(), exit = popoverExit()) {
            Box(
                modifier = Modifier
                    .hoverable(interaction)
                    .padding(tokens.spacing.extraSmall)
                    .widthIn(max = OverlayMetrics.tooltipMaxWidth)
                    .shadow(style.shadow, style.popoverShape)
                    .clip(style.popoverShape)
                    .background(style.tooltip)
                    .then(
                        if (style.tooltipBorder != null) {
                            Modifier.border(style.tooltipBorder, style.popoverShape)
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = tokens.spacing.small, vertical = tokens.spacing.extraSmall),
            ) {
                BuilderText(text, style = BuilderTextStyle.Label, color = style.tooltipContent)
            }
        }
    }
}

/** Centred above the anchor, or below it when the window runs out above. */
private class TooltipPositionProvider(
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centred = anchorBounds.center.x - popupContentSize.width / 2
        val x = centred.coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.bottom + gap
        return IntOffset(x, y)
    }
}
