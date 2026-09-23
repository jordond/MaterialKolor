package com.materialkolor.builder.kit.control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentToggleButton
import com.materialkolor.builder.kit.skin.headless.ActionDisabledAlpha
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.SelectableStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionPress
import com.materialkolor.builder.kit.skin.headless.actionRing
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.actionTouchTarget
import com.materialkolor.builder.kit.skin.material.MaterialToggleButton

/**
 * A labelled button that stays on or off, such as Inspect in the dock.
 *
 * It reads out as a checkbox with its on or off state, which is how every skin's own toggle button
 * announces itself.
 *
 * @param[checked] Whether it is on.
 * @param[onCheckedChange] Called with the state the user asked for.
 * @param[label] What it switches, shown and read out.
 * @param[modifier] Applied to the button.
 * @param[icon] A glyph before the label.
 * @param[enabled] Whether it can be switched.
 */
@Composable
public fun BuilderToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    enabled: Boolean = true,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialToggleButton(checked, onCheckedChange, label, modifier, icon, enabled)
        }
        Library.Unstyled -> {
            HeadlessToggleButton(
                checked,
                onCheckedChange,
                label,
                UnstyledActionStyles.toggleButton,
                modifier,
                icon,
                enabled,
            )
        }
        Library.Fluent -> {
            // fluent-placeholder
            FluentToggleButton(checked, onCheckedChange, label, modifier, icon, enabled)
        }
        Library.Custom -> {
            HeadlessToggleButton(
                checked,
                onCheckedChange,
                label,
                CustomActionStyles.toggleButton,
                modifier,
                icon,
                enabled,
            )
        }
    }
}

/** A toggle button drawn from [style], filled while it is on. */
@Composable
internal fun HeadlessToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    style: SelectableStyle,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    enabled: Boolean = true,
) {
    val colors = style.colors(checked)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ).actionTouchTarget(LocalLayout.current.primaryTouchTarget)
            .actionPress(interactionSource)
            .alpha(if (enabled) 1f else ActionDisabledAlpha)
            .actionRing(interactionSource, style.shape)
            .actionSurface(colors, style.shape, style.borderWidth)
            .heightIn(min = style.height)
            .padding(horizontal = style.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(style.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) BuilderIcon(icon, contentDescription = null, tint = colors.content)
        BuilderText(label, style = BuilderTextStyle.Label, color = colors.content, maxLines = 1)
    }
}
