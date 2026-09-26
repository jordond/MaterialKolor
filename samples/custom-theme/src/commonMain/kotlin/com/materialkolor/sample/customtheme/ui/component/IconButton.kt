package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun IconButton(
    glyph: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalAppColors.current.textMuted,
    activeTint: Color = tint,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val shape = AppShapes.Inner

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .veil(tint = colors.onSurface, state = state)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).pointerHoverIcon(PointerIcon.Hand)
            .focusRing(state = state, color = colors.focusRing, shape = shape),
    ) {
        Icon(
            glyph = glyph,
            color = if (state.isHovered || state.isPressed) activeTint else tint,
            modifier = Modifier.size(14.dp),
        )
    }
}
