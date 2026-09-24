package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import com.composeunstyled.RadioGroupScope
import com.composeunstyled.Text
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledRadioGroup
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * A row of segments in a shared track, exactly one of them picked. It is an Unstyled radio group, so each segment is
 * a radio button that also reports `selected`.
 *
 * @param[choices] The segments, in order.
 * @param[selected] The picked value.
 * @param[onSelect] Called with the value of a segment when it is picked.
 * @param[label] What the group is called.
 * @param[modifier] Applied to the track.
 */
@Composable
internal fun <T> SegmentedControl(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    UnstyledRadioGroup(
        value = selected,
        onValueChange = onSelect,
        modifier = modifier
            .clip(Shapes.Card)
            .background(MaterialKolorTokens.surfaceContainerHigh.color)
            .padding(Spacing.XSmall),
        accessibilityLabel = label,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            for (choice in choices) {
                Segment(choice = choice, isSelected = choice.value == selected)
            }
        }
    }
}

@Composable
private fun <T> RadioGroupScope.Segment(
    choice: Choice<T>,
    isSelected: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (isSelected) MaterialKolorTokens.secondaryContainer.color else Color.Transparent,
        label = "segment",
    )
    val content = if (isSelected) {
        MaterialKolorTokens.onSecondaryContainer.color
    } else {
        MaterialKolorTokens.onSurfaceVariant.color
    }

    ProvideContentColor(content) {
        RadioButton(
            value = choice.value,
            modifier = Modifier
                .testTag(choice.testTag)
                .semantics { selected = isSelected }
                .controlFocusRing(interactionSource, Shapes.Control, offset = 0.dp)
                .clip(Shapes.Control)
                .background(container)
                .padding(horizontal = Spacing.Medium, vertical = 6.dp),
            interactionSource = interactionSource,
            indication = LocalIndication.current,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = choice.icon
                if (icon != null) {
                    UnstyledIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = content,
                    )
                }
                Text(text = choice.label, style = TasksType.Label, maxLines = 1)
            }
        }
    }
}
