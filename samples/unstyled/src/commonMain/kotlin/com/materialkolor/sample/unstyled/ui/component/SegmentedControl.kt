package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.RadioButton
import com.composeunstyled.RadioGroupScope
import com.composeunstyled.Text
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledRadioGroup
import com.materialkolor.sample.unstyled.theme.ControlHeight
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.unstyled.MaterialKolorTokens

private val SegmentHeight = ControlHeight - Spacing.XSmall * 2

/**
 * A sunken track with a raised thumb that slides to the selected choice.
 */
@Composable
internal fun <T> SegmentedControl(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = ShapeTokens.pill.shape
    val thumb = rememberSlidingThumbState()

    UnstyledRadioGroup(
        value = selected,
        onValueChange = onSelect,
        modifier = modifier
            .sunken(pill)
            .padding(Spacing.XSmall),
    ) {
        Box {
            SlidingThumb(state = thumb, selected = selected, modifier = Modifier.raised(pill))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                for (choice in choices) {
                    Segment(
                        choice = choice,
                        isSelected = choice.value == selected,
                        modifier = Modifier.thumbSlot(thumb, choice.value),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> RadioGroupScope.Segment(
    choice: Choice<T>,
    isSelected: Boolean,
    modifier: Modifier,
) {
    val pill = ShapeTokens.pill.shape
    val interactionSource = remember { MutableInteractionSource() }
    val content by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialKolorTokens.onSurface.color
        } else {
            MaterialKolorTokens.onSurfaceVariant.color
        },
        label = "segment",
    )
    val icon by animateColorAsState(
        targetValue = if (isSelected) MaterialKolorTokens.primary.color else content,
        label = "segmentIcon",
    )

    ProvideContentColor(content) {
        RadioButton(
            value = choice.value,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            modifier = modifier
                .height(SegmentHeight)
                .controlFocusRing(interactionSource, pill, offset = 0.dp)
                .clip(pill),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Tight),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = Spacing.Large),
            ) {
                if (choice.icon != null) {
                    UnstyledIcon(
                        imageVector = choice.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = icon,
                    )
                }

                Text(text = choice.label, style = TasksType.Label, maxLines = 1)
            }
        }
    }
}
