package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun <T> TabStrip(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            for (tab in tabs) {
                Tab(
                    text = label(tab),
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(7.dp)
                .drawBehind {
                    val heavy = 3.dp.toPx()
                    val light = 1.dp.toPx()
                    drawLine(colors.ink, Offset(0f, heavy / 2), Offset(size.width, heavy / 2), heavy)
                    drawLine(
                        colors.ink,
                        Offset(0f, size.height - light / 2),
                        Offset(size.width, size.height - light / 2),
                        light,
                    )
                },
        )
    }
}

@Composable
private fun Tab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val isMarked = isSelected || state.isHovered

    Text(
        text = text.uppercase(),
        style = AppType.Display,
        color = if (isSelected) colors.ink else colors.inkSoft,
        modifier = Modifier
            .then(
                if (isMarked) {
                    Modifier.highlighter(
                        if (isSelected) colors.highlight else colors.paperShade,
                        colors,
                    )
                } else {
                    Modifier
                },
            ).selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).pointerHoverIcon(PointerIcon.Hand)
            .focusRing(state = state, color = colors.blue, shape = RectangleShape),
    )
}
