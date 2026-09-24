package com.materialkolor.sample.fluent.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.readableOn
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.Checkmark

/**
 * A round swatch of [seed] that picks it as the theme seed.
 *
 * Fluent has no color swatch, so this one is built from the theme's own tokens. The ring shows hover, selection and
 * keyboard focus, and the check on the selected swatch takes the tone from the seed's ramp that reads on it.
 *
 * @param[seed] The seed the swatch shows.
 * @param[selected] Whether [seed] is the current one.
 * @param[onClick] Called when the swatch is picked.
 * @param[modifier] The modifier for the swatch.
 */
@Composable
internal fun SeedSwatch(
    seed: SampleSeed,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val colors = FluentTheme.colors

    val ring by animateColorAsState(
        targetValue = when {
            focused -> colors.stroke.focus.outer
            selected -> colors.fillAccent.default
            hovered -> colors.stroke.controlStrong.default
            else -> colors.subtleFill.transparent
        },
        label = "SeedSwatchRing",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        label = "SeedSwatchScale",
    )
    val check = remember(seed) { seed.color.readableOn() }

    Box(
        modifier = modifier
            .size(32.dp)
            .testTag(SampleTags.seed(seed))
            .semantics { contentDescription = SampleCopy.label(seed) }
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ).border(width = 2.dp, color = ring, shape = CircleShape)
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(CircleShape)
            .background(seed.color),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Checkmark,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = check,
            )
        }
    }
}

private const val PRESSED_SCALE = 0.88f
