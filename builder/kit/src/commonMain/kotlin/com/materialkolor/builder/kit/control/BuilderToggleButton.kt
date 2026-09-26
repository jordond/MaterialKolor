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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentToggleButton
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.SelectableStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialToggleButton

/**
 * A labelled button that stays on or off, such as Inspect in the dock.
 *
 * It reads out as a checkbox with its on or off state, which is how every skin's own toggle button
 * announces itself. On the web the state travels in its name, as in "Inspect, checked".
 *
 * @param[checked] Whether it is on.
 * @param[onCheckedChange] Called with the state the user asked for.
 * @param[label] What it switches, shown and read out.
 * @param[modifier] Applied to the button.
 * @param[icon] A glyph before the label. The skins with no toggle button of their own show a check
 * in its place while the button is on.
 * @param[enabled] Whether it can be switched.
 * @param[contentDescription] A longer name to read out in place of [label], for a short label that
 * needs the line before it to make sense. Its state follows it the way it follows the label.
 */
@Composable
public fun BuilderToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val name = contentDescription ?: label
    // The skins fold the name only on the web, so a longer name is set here for everywhere else.
    val spoken = if (contentDescription == null) null else stateName(name, ControlState.Checked(checked), enabled)
    val named = if (spoken == null) modifier else modifier.semantics { this.contentDescription = spoken }
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialToggleButton(checked, onCheckedChange, label, named, icon, enabled, name)
        }
        Library.Unstyled -> {
            HeadlessToggleButton(
                checked,
                onCheckedChange,
                label,
                UnstyledActionStyles.toggleButton,
                named,
                icon,
                enabled,
                name,
            )
        }
        Library.Fluent -> {
            FluentToggleButton(checked, onCheckedChange, label, named, icon, enabled, name)
        }
        Library.Custom -> {
            HeadlessToggleButton(
                checked,
                onCheckedChange,
                label,
                CustomActionStyles.toggleButton,
                named,
                icon,
                enabled,
                name,
            )
        }
    }
}

/**
 * A toggle button drawn from [style], filled while it is on. It shows a check in place of its icon
 * while it is on, so on and off never differ by fill alone. It reads out as [name], the label
 * unless a longer name was given.
 */
@Composable
internal fun HeadlessToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    style: SelectableStyle,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    enabled: Boolean = true,
    name: String = label,
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
            ).foldState(name, ControlState.Checked(checked), enabled)
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
        val glyph = if (checked) IconId.Check else icon
        if (glyph != null) BuilderIcon(glyph, contentDescription = null, tint = colors.content)
        BuilderText(label, style = BuilderTextStyle.Label, color = colors.content, maxLines = 1)
    }
}
