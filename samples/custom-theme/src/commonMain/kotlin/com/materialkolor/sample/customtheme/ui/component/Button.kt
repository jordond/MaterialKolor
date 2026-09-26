package com.materialkolor.sample.customtheme.ui.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.LocalAppColors

internal enum class ButtonStyle {
    Primary,
    Quiet,
    Danger,
}

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
    val shift = state.animateShift(ControlShift)
    val look = buttonLook(style = style, enabled = enabled, state = state, colors = colors)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(ControlHeight)
            .then(if (look.shadow != null) Modifier.ink(look.shadow, colors, offset = shift) else Modifier)
            .then(if (look.fill != null) Modifier.ink(look.fill, colors) else Modifier)
            .then(if (look.outline != null) Modifier.border(Rule, look.outline) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ).pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .focusRing(state = state, color = colors.blue, shape = RectangleShape)
            .padding(horizontal = 18.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = AppType.Label,
            color = look.content,
        )
    }
}

private data class ButtonLook(
    val fill: Color?,
    val shadow: Color?,
    val outline: Color?,
    val content: Color,
)

private fun buttonLook(
    style: ButtonStyle,
    enabled: Boolean,
    state: ControlState,
    colors: AppColors,
): ButtonLook {
    if (!enabled) {
        return ButtonLook(
            fill = if (style == ButtonStyle.Quiet) null else colors.paperShade,
            shadow = null,
            outline = colors.paperEdge,
            content = colors.inkSoft,
        )
    }

    return when (style) {
        ButtonStyle.Primary -> ButtonLook(
            fill = colors.primary,
            shadow = colors.pink,
            outline = null,
            content = colors.onPrimary,
        )
        ButtonStyle.Quiet -> ButtonLook(
            fill = if (state.isHovered || state.isPressed) colors.highlight else null,
            shadow = null,
            outline = colors.ink,
            content = colors.ink,
        )
        ButtonStyle.Danger -> ButtonLook(
            fill = colors.error,
            shadow = colors.yellow,
            outline = null,
            content = colors.onError,
        )
    }
}
