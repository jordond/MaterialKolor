package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun SeedSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val ring = when {
        state.isFocused -> colors.focusRing
        isSelected -> colors.textStrong
        state.isHovered -> colors.borderStrong
        else -> null
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).pointerHoverIcon(PointerIcon.Hand)
            .padding(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (state.isPressed) PRESSED_SCALE else 1f)
                .clip(CircleShape)
                .background(color),
        )
    }
}

private const val PRESSED_SCALE = 0.9f
