package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * How loud a [Button] is.
 */
internal enum class ButtonStyle {
    /** Filled with the primary accent, for the one main action in a group. */
    Primary,

    /** Outlined and see-through, for everything else. */
    Quiet,

    /** Filled with the error accent, for an action that removes something for good. */
    Danger,
}

/**
 * A text button in one of the [ButtonStyle] looks.
 *
 * The primary style steps through the theme's own interaction tones, `primaryRaised` on hover and `primaryPressed`
 * while held, instead of laying a tint over the fill.
 */
@Composable
internal fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val look = buttonLook(style = style, enabled = enabled, state = state, colors = colors)
    val shape = AppShapes.Control

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(ControlHeight)
            .clip(shape)
            .then(if (look.container != null) Modifier.background(look.container) else Modifier)
            .then(if (look.border != null) Modifier.border(1.dp, look.border, shape) else Modifier)
            .then(if (look.veil != null) Modifier.veil(look.veil, state) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .focusRing(state = state, color = colors.focusRing, shape = shape)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            style = AppType.Label,
            color = look.content,
        )
    }
}

/**
 * The colors one button draws with in its current state.
 *
 * @property[container] The fill, or null for none.
 * @property[content] The label color.
 * @property[border] The outline, or null for none.
 * @property[veil] The tint laid over the fill on hover and press, or null when the fill changes on its own.
 */
private data class ButtonLook(
    val container: Color?,
    val content: Color,
    val border: Color?,
    val veil: Color?,
)

private fun buttonLook(
    style: ButtonStyle,
    enabled: Boolean,
    state: ControlState,
    colors: AppColors,
): ButtonLook {
    if (!enabled) {
        return ButtonLook(
            container = if (style == ButtonStyle.Quiet) null else colors.borderFaint,
            content = colors.textMuted.copy(alpha = DISABLED_ALPHA),
            border = if (style == ButtonStyle.Quiet) colors.borderFaint else null,
            veil = null,
        )
    }

    return when (style) {
        ButtonStyle.Primary -> ButtonLook(
            container = when {
                state.isPressed -> colors.primaryPressed
                state.isHovered -> colors.primaryRaised
                else -> colors.primary
            },
            content = colors.onPrimary,
            border = null,
            veil = null,
        )
        ButtonStyle.Quiet -> ButtonLook(
            container = null,
            content = colors.textStrong,
            border = colors.borderSoft,
            veil = colors.onSurface,
        )
        ButtonStyle.Danger -> ButtonLook(
            container = colors.error,
            content = colors.onError,
            border = null,
            veil = colors.onError,
        )
    }
}

/** Disabled labels fade back so the control reads as unavailable, not just muted. */
private const val DISABLED_ALPHA = 0.6f
