package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    badge: ((T) -> String)? = null,
    accent: ((T) -> Accent)? = null,
) {
    val colors = LocalAppColors.current
    val primary = Accent(container = colors.primary, content = colors.onPrimary)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ControlHeight)
            .border(Rule, colors.ink),
    ) {
        options.forEachIndexed { index, option ->
            Segment(
                text = label(option),
                badge = badge?.invoke(option),
                isSelected = option == selected,
                isFirst = index == 0,
                accent = accent?.invoke(option) ?: primary,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    badge: String?,
    isSelected: Boolean,
    isFirst: Boolean,
    accent: Accent,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val content = if (isSelected) accent.content else colors.ink
    val fill = when {
        isSelected -> accent.container
        state.isHovered || state.isPressed -> colors.paperShade
        else -> null
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxHeight()
            .then(if (fill != null) Modifier.ink(fill, colors) else Modifier)
            .drawBehind {
                if (!isFirst) {
                    val x = Rule.toPx() / 2
                    drawLine(colors.ink, Offset(x, 0f), Offset(x, size.height), Rule.toPx())
                }
            }.selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).pointerHoverIcon(PointerIcon.Hand)
            .focusRing(state = state, color = colors.blue, shape = RectangleShape)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = AppType.Label,
            color = content,
        )

        if (badge != null) {
            Text(
                text = badge,
                style = AppType.Caption,
                color = content.copy(alpha = BADGE_ALPHA),
            )
        }
    }
}

private const val BADGE_ALPHA = 0.7f
