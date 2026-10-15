package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
    val shift = state.animateShift(InkShift)
    val mark = when {
        isSelected -> colors.ink
        state.isHovered || state.isFocused -> colors.inkSoft
        else -> null
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .drawBehind {
                if (mark != null) {
                    drawRegistrationMark(color = mark, center = center, radius = size.minDimension * MARK_SHARE)
                }
            }.selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).pointerHoverIcon(PointerIcon.Hand)
            .padding(10.dp)
            .ink(color, colors, shape = CircleShape, offset = shift),
    )
}

private const val MARK_SHARE = 0.4f
