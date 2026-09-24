package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * The section tabs, a row of labels over a hairline with the picked one underlined in the primary accent.
 *
 * @param[tabModifier] Extra modifiers for each tab, applied to the node that takes the click.
 */
@Composable
internal fun <T> TabStrip(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    tabModifier: (T) -> Modifier = { Modifier },
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            for (tab in tabs) {
                Tab(
                    text = label(tab),
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = tabModifier(tab),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderFaint),
        )
    }
}

@Composable
private fun Tab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val underline = colors.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .clip(TabShape)
            .veil(tint = colors.onSurface, state = state)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .focusRing(state = state, color = colors.focusRing, shape = TabShape)
            .drawBehind {
                if (isSelected) {
                    val thickness = 2.dp.toPx()
                    drawRect(
                        color = underline,
                        topLeft = Offset(0f, size.height - thickness),
                        size = Size(size.width, thickness),
                    )
                }
            }
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = text,
            style = AppType.Label,
            color = if (isSelected) colors.textStrong else colors.textMuted,
        )
    }
}

private val TabShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
