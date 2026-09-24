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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * A round swatch of one seed [color], ringed while it is the picked seed.
 *
 * @param[color] The seed itself. It is the one color in the UI that does not come from the theme, because it is what
 *   the theme comes from.
 * @param[label] The seed's name, read out in place of the color.
 */
@Composable
internal fun SeedSwatch(
    color: Color,
    label: String,
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
                role = Role.RadioButton,
                onClick = onClick,
            ).semantics { contentDescription = label }
            .pointerHoverIcon(PointerIcon.Hand)
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
