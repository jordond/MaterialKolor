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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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

/**
 * What a chip is filled with and what its label is written in.
 */
@Immutable
internal data class ChipColors(
    val container: Color,
    val content: Color,
)

/**
 * A small filled label.
 *
 * @param[label] What it says.
 * @param[colors] Its fill and ink.
 * @param[modifier] Applied to the chip.
 */
@Composable
internal fun Chip(
    label: String,
    colors: ChipColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .clip(Shapes.Pill)
            .background(colors.container)
            .padding(horizontal = Spacing.Small, vertical = Spacing.XSmall),
        style = TasksType.Small,
        color = colors.content,
        maxLines = 1,
    )
}

/**
 * Chips to pick exactly one of. The picked chip is filled in its own colors and the rest are outlined. It is an
 * Unstyled radio group, so each chip is a radio button that also reports `selected`.
 *
 * @param[choices] The chips, in order.
 * @param[selected] The picked value.
 * @param[onSelect] Called with the value of a chip when it is picked.
 * @param[colors] The fill and ink of a picked chip.
 * @param[modifier] Applied to the group.
 */
@Composable
internal fun <T> ChoiceChips(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    colors: @Composable (T) -> ChipColors,
    modifier: Modifier = Modifier,
) {
    UnstyledRadioGroup(value = selected, onValueChange = onSelect, modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        modifier = Modifier
                            .testTag(choice.testTag)
                            .semantics { this.selected = isSelected }
                            .pressScale(interactionSource)
                            .controlFocusRing(interactionSource, Shapes.Control)
                            .clip(Shapes.Control)
                            .background(container)
                            .border(width = 1.dp, color = outline, shape = Shapes.Control)
                            .height(ControlHeight),
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                    ) {
                        // The padding goes inside, since the radio button puts its toggle after this modifier.
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = Spacing.Medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = choice.label, style = TasksType.Label, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
