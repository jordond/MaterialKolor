package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.shadow
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
import com.materialkolor.sample.unstyled.theme.ControlHeight
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

/** A segment with the track's padding around it is as tall as a button. */
private val SegmentHeight = ControlHeight - Spacing.XSmall * 2

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
    // The picked segment is the brightest surface, so it reads as raised off the track in light and dark alike.
    val container by animateColorAsState(
        targetValue = if (isSelected) MaterialKolorTokens.surfaceBright.color else Color.Transparent,
        label = "segment",
    )
    val elevation by animateDpAsState(targetValue = if (isSelected) 1.dp else 0.dp, label = "lift")
    val shadow = MaterialKolorTokens.shadow.color
    val content = if (isSelected) MaterialKolorTokens.onSurface.color else MaterialKolorTokens.onSurfaceVariant.color

    ProvideContentColor(content) {
        RadioButton(
            value = choice.value,
            modifier = Modifier
                .testTag(choice.testTag)
                .semantics { selected = isSelected }
                .height(SegmentHeight)
                .controlFocusRing(interactionSource, Shapes.Control, offset = 0.dp)
                .shadow(elevation = elevation, shape = Shapes.Control, ambientColor = shadow, spotColor = shadow)
                .clip(Shapes.Control)
                .background(container),
            interactionSource = interactionSource,
            indication = LocalIndication.current,
        ) {
            // The padding goes inside, since the radio button puts its toggle after this modifier.
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = choice.icon
                if (icon != null) {
                    UnstyledIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialKolorTokens.primary.color else content,
                    )
                }
                Text(text = choice.label, style = TasksType.Label, maxLines = 1)
            }
        }
    }
}
