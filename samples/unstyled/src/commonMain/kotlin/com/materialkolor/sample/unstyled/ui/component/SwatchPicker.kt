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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composeunstyled.RadioButton
import com.composeunstyled.UnstyledRadioGroup
import com.composeunstyled.outline
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.unstyled.MaterialKolorTokens

private const val SHINE_ALPHA = 0.55f
private const val GLOW_ALPHA = 0.5f

/**
 * Round gems, each lit from the top left and glowing in its own color.
 */
@Composable
internal fun <T> SwatchPicker(
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    color: (T) -> Color,
    modifier: Modifier = Modifier,
) {
    val ring = MaterialKolorTokens.onSurface.color
    val round = ShapeTokens.pill.shape

    UnstyledRadioGroup(value = selected, onValueChange = onSelect, modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            for (value in values) {
                val isSelected = value == selected
                val fill = color(value)
                val interactionSource = remember { MutableInteractionSource() }

                RadioButton(
                    value = value,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    modifier = Modifier
                        .size(28.dp)
                        .pressScale(interactionSource)
                        .controlFocusRing(interactionSource, round, offset = 6.dp)
                        .then(if (isSelected) Modifier.outline(2.dp, ring, round, offset = 3.dp) else Modifier)
                        .dropShadow(
                            shape = round,
                            shadow = Shadow(
                                radius = 8.dp,
                                color = fill,
                                offset = DpOffset(0.dp, 3.dp),
                                alpha = GLOW_ALPHA,
                            ),
                        ).clip(round)
                        .background(fill)
                        .shine(),
                ) {}
            }
        }
    }
}

private fun Modifier.shine(): Modifier =
    drawWithCache {
        val highlight = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = SHINE_ALPHA), Color.Transparent),
            center = Offset(size.width * 0.32f, size.height * 0.28f),
            radius = size.minDimension * 0.6f,
        )
        onDrawBehind { drawRect(highlight) }
    }
