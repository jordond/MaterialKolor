package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.RadioButton
import com.composeunstyled.Text
import com.composeunstyled.UnstyledRadioGroup
import com.materialkolor.sample.unstyled.theme.ControlHeight
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

@Immutable
internal data class ChipColors(
    val container: Color,
    val content: Color,
)

@Composable
internal fun Chip(
    label: String,
    colors: ChipColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = TasksType.Small,
        color = colors.content,
        maxLines = 1,
        modifier = modifier
            .clip(Shapes.Pill)
            .background(colors.container)
            .padding(horizontal = Spacing.Small, vertical = Spacing.XSmall),
    )
}

@Composable
internal fun <T> ChoiceChips(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    colors: @Composable (T) -> ChipColors,
    modifier: Modifier = Modifier,
) {
    UnstyledRadioGroup(value = selected, onValueChange = onSelect, modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Tight)) {
            for (choice in choices) {
                val isSelected = choice.value == selected
                val picked = colors(choice.value)
                val interactionSource = remember { MutableInteractionSource() }
                val container by animateColorAsState(
                    targetValue = if (isSelected) picked.container else Color.Transparent,
                    label = "chip",
                )
                val outline = if (isSelected) Color.Transparent else MaterialKolorTokens.outlineVariant.color
                val content = if (isSelected) picked.content else MaterialKolorTokens.onSurfaceVariant.color

                ProvideContentColor(content) {
                    RadioButton(
                        value = choice.value,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        modifier = Modifier
                            .pressScale(interactionSource)
                            .controlFocusRing(interactionSource, Shapes.Control)
                            .clip(Shapes.Control)
                            .background(container)
                            .border(width = 1.dp, color = outline, shape = Shapes.Control)
                            .height(ControlHeight),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = Spacing.Medium),
                        ) {
                            Text(text = choice.label, style = TasksType.Label, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
