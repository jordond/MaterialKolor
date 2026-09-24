package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.RadioButton
import com.composeunstyled.UnstyledRadioGroup
import com.composeunstyled.outline
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * Round color swatches to pick exactly one of. The picked swatch wears a ring. It is an Unstyled radio group, so each
 * swatch is a radio button that also reports `selected`, named by the label of its choice.
 *
 * @param[choices] The swatches, in order.
 * @param[selected] The picked value.
 * @param[onSelect] Called with the value of a swatch when it is picked.
 * @param[color] What a swatch is filled with.
 * @param[label] What the group is called.
 * @param[modifier] Applied to the group.
 */
@Composable
internal fun <T> SwatchPicker(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    color: (T) -> Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    val ring = MaterialKolorTokens.onSurface.color
    UnstyledRadioGroup(
        value = selected,
        onValueChange = onSelect,
        modifier = modifier,
        accessibilityLabel = label,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            for (choice in choices) {
                val isSelected = choice.value == selected
                val interactionSource = remember { MutableInteractionSource() }
                RadioButton(
                    value = choice.value,
                    modifier = Modifier
                        .testTag(choice.testTag)
                        .semantics {
                            contentDescription = choice.label
                            this.selected = isSelected
                        }.size(28.dp)
                        .pressScale(interactionSource)
                        .controlFocusRing(interactionSource, Shapes.Round, offset = 6.dp)
                        .then(if (isSelected) Modifier.outline(2.dp, ring, Shapes.Round, offset = 2.dp) else Modifier)
                        .clip(Shapes.Round)
                        .background(color(choice.value)),
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                ) {}
            }
        }
    }
}
