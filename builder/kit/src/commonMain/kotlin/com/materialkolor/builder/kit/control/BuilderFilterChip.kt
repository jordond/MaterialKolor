package com.materialkolor.builder.kit.control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentFilterChip
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.SelectableStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialFilterChip

/**
 * A compact on and off choice among several that can be on together, such as a filter.
 *
 * It reads out as a checkbox with its selected state, the way Material's own filter chip does, and
 * carries the matching on or off state so a reader that mirrors checkboxes never hears it as
 * unchecked. A selected chip shows a check in place of its icon, so selection never rests on colour
 * alone.
 *
 * @param[selected] Whether the chip is on.
 * @param[onSelectedChange] Called with the state the user asked for.
 * @param[label] What the chip stands for, shown and read out.
 * @param[modifier] Applied to the chip.
 * @param[icon] A glyph before the label while the chip is off.
 * @param[enabled] Whether the chip can be switched.
 */
@Composable
public fun BuilderFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    enabled: Boolean = true,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialFilterChip(selected, onSelectedChange, label, modifier, icon, enabled)
        }
        Library.Unstyled -> {
            HeadlessFilterChip(selected, onSelectedChange, label, UnstyledActionStyles.chip, modifier, icon, enabled)
        }
        Library.Fluent -> {
            FluentFilterChip(selected, onSelectedChange, label, modifier, icon, enabled)
        }
        Library.Custom -> {
            HeadlessFilterChip(selected, onSelectedChange, label, CustomActionStyles.chip, modifier, icon, enabled)
        }
    }
}

/**
 * A filter chip drawn from [style].
 */
@Composable
internal fun HeadlessFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    label: String,
    style: SelectableStyle,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    enabled: Boolean = true,
) {
    val colors = style.colors(selected)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onSelectedChange(!selected) },
            ).semantics { toggleableState = ToggleableState(selected) }
            .foldState(label, ControlState.Selected(selected), enabled)
            .controlTouchTarget(LocalLayout.current.primaryTouchTarget)
            .controlPress(interactionSource)
            .alpha(enabledAlpha(enabled))
            .controlRing(interactionSource, style.shape)
            .actionSurface(colors, style.shape, style.borderWidth)
            .heightIn(min = style.height)
            .padding(horizontal = style.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(style.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val glyph = if (selected) IconId.Check else icon
        if (glyph != null) BuilderIcon(glyph, contentDescription = null, tint = colors.content)
        BuilderText(label, style = BuilderTextStyle.Label, color = colors.content, maxLines = 1)
    }
}
