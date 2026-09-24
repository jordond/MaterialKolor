package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.materialkolor.builder.kit.skin.fluent.FluentButton
import com.materialkolor.builder.kit.skin.headless.ButtonStyle
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialButton

/**
 * A labelled action in the surrounding skin.
 *
 * @param[onClick] Called when the button is pressed.
 * @param[label] What the button does, shown and read out.
 * @param[modifier] Applied to the button.
 * @param[emphasis] How loudly the button speaks. Keep [Emphasis.Primary] to one per screen.
 * @param[icon] A glyph before the label.
 * @param[enabled] Whether the button can be pressed.
 */
@Composable
public fun BuilderButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Secondary,
    icon: IconId? = null,
    enabled: Boolean = true,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialButton(onClick, label, modifier, emphasis, icon, enabled)
        Library.Unstyled -> HeadlessButton(
            onClick,
            label,
            UnstyledActionStyles.button,
            modifier,
            emphasis,
            icon,
            enabled,
        )
        Library.Fluent -> FluentButton(onClick, label, modifier, emphasis, icon, enabled) // fluent-placeholder
        Library.Custom -> HeadlessButton(onClick, label, CustomActionStyles.button, modifier, emphasis, icon, enabled)
    }
}

/** A button drawn from [style] over plain foundation, for the skins without a button of their own. */
@Composable
internal fun HeadlessButton(
    onClick: () -> Unit,
    label: String,
    style: ButtonStyle,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Secondary,
    icon: IconId? = null,
    enabled: Boolean = true,
) {
    val colors = style.colors(emphasis)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).foldState(label, null, enabled)
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
        if (icon != null) BuilderIcon(icon, contentDescription = null, tint = colors.content)
        BuilderText(label, style = BuilderTextStyle.Label, color = colors.content, maxLines = 1)
    }
}
