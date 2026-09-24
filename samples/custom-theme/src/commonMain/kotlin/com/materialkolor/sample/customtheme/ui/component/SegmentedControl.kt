package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * A row of options where exactly one is picked, like the mode picker, the composer tag and the filter.
 *
 * @param[label] The text on each option.
 * @param[badge] An optional count or note after the label.
 * @param[accent] The colors of the picked option. Defaults to the primary container.
 * @param[optionModifier] Extra modifiers for each option, applied to the node that takes the click.
 */
@Composable
internal fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    badge: ((T) -> String)? = null,
    accent: ((T) -> Accent)? = null,
    optionModifier: (T) -> Modifier = { Modifier },
) {
    val colors = LocalAppColors.current
    val primary = Accent(container = colors.primaryContainer, content = colors.onPrimaryContainer)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ControlHeight)
            .clip(AppShapes.Control)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.borderFaint, AppShapes.Control)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        for (option in options) {
            Segment(
                text = label(option),
                badge = badge?.invoke(option),
                isSelected = option == selected,
                accent = accent?.invoke(option) ?: primary,
                onClick = { onSelect(option) },
                modifier = optionModifier(option),
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    badge: String?,
    isSelected: Boolean,
    accent: Accent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val state = interactionSource.collectControlState()
    val content = if (isSelected) accent.content else colors.textMuted
    val shape = AppShapes.Inner

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .then(if (isSelected) Modifier.background(accent.container) else Modifier)
            .veil(tint = content, state = state)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ).pointerHoverIcon(PointerIcon.Hand)
            .focusRing(state = state, color = colors.focusRing, shape = shape)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = text,
            style = AppType.Label,
            color = content,
        )

        if (badge != null) {
            Text(
                text = badge,
                style = AppType.Caption,
                color = content.copy(alpha = BADGE_ALPHA),
            )
        }
    }
}

/** Counts sit a step back from the label they follow. */
private const val BADGE_ALPHA = 0.7f
