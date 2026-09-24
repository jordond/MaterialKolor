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
import androidx.compose.ui.unit.dp
import com.composeunstyled.RadioButton
import com.composeunstyled.UnstyledRadioGroup
import com.composeunstyled.outline
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

@Composable
internal fun <T> SwatchPicker(
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    color: (T) -> Color,
    modifier: Modifier = Modifier,
) {
    val ring = MaterialKolorTokens.onSurface.color

    UnstyledRadioGroup(value = selected, onValueChange = onSelect, modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            for (value in values) {
                val isSelected = value == selected
                val interactionSource = remember { MutableInteractionSource() }

                RadioButton(
                    value = value,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    modifier = Modifier
                        .size(28.dp)
                        .pressScale(interactionSource)
                        .controlFocusRing(interactionSource, Shapes.Round, offset = 6.dp)
                        .then(if (isSelected) Modifier.outline(2.dp, ring, Shapes.Round, offset = 2.dp) else Modifier)
                        .clip(Shapes.Round)
                        .background(color(value)),
                ) {}
            }
        }
    }
}
