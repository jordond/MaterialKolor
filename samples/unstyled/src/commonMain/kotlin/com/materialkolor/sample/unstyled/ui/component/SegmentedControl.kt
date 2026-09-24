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

private val SegmentHeight = ControlHeight - Spacing.XSmall * 2

@Composable
internal fun <T> SegmentedControl(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnstyledRadioGroup(
        value = selected,
        onValueChange = onSelect,
        modifier = modifier
            .clip(Shapes.Card)
            .background(MaterialKolorTokens.surfaceContainerHigh.color)
            .padding(Spacing.XSmall),
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
    val shadow = MaterialKolorTokens.shadow.color
    val interactionSource = remember { MutableInteractionSource() }
    val elevation by animateDpAsState(targetValue = if (isSelected) 1.dp else 0.dp, label = "lift")

    val content = if (isSelected) MaterialKolorTokens.onSurface.color else MaterialKolorTokens.onSurfaceVariant.color
    val container by animateColorAsState(
        targetValue = if (isSelected) MaterialKolorTokens.surfaceBright.color else Color.Transparent,
        label = "segment",
    )

    ProvideContentColor(content) {
        RadioButton(
            value = choice.value,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            modifier = Modifier
                .height(SegmentHeight)
                .controlFocusRing(interactionSource, Shapes.Control, offset = 0.dp)
                .shadow(elevation = elevation, shape = Shapes.Control, ambientColor = shadow, spotColor = shadow)
                .clip(Shapes.Control)
                .background(container),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Tight),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = Spacing.Medium),
            ) {
                if (choice.icon != null) {
                    UnstyledIcon(
                        imageVector = choice.icon,
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
