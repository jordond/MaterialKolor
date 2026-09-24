package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import com.materialkolor.builder.kit.skin.fluent.FluentIconButton
import com.materialkolor.builder.kit.skin.headless.ButtonStyle
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialIconButton

/**
 * An action shown as a glyph alone, such as undo in the top bar.
 *
 * @param[onClick] Called when the button is pressed.
 * @param[icon] The glyph.
 * @param[contentDescription] What the button does, read out in place of a label.
 * @param[modifier] Applied to the button.
 * @param[emphasis] How loudly the button speaks. Most icon buttons stay [Emphasis.Subtle].
 * @param[enabled] Whether the button can be pressed.
 */
@Composable
public fun BuilderIconButton(
    onClick: () -> Unit,
    icon: IconId,
    contentDescription: String,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Subtle,
    enabled: Boolean = true,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialIconButton(onClick, icon, contentDescription, modifier, emphasis, enabled)
        }
        Library.Unstyled -> {
            HeadlessIconButton(
                onClick,
                icon,
                contentDescription,
                UnstyledActionStyles.button,
                modifier,
                emphasis,
                enabled,
            )
        }
        Library.Fluent -> {
            // fluent-placeholder
            FluentIconButton(onClick, icon, contentDescription, modifier, emphasis, enabled)
        }
        Library.Custom -> {
            HeadlessIconButton(
                onClick,
                icon,
                contentDescription,
                CustomActionStyles.button,
                modifier,
                emphasis,
                enabled,
            )
        }
    }
}

/** A square button holding one glyph, drawn from [style]. */
@Composable
internal fun HeadlessIconButton(
    onClick: () -> Unit,
    icon: IconId,
    contentDescription: String,
    style: ButtonStyle,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Subtle,
    enabled: Boolean = true,
) {
    val colors = style.colors(emphasis)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).controlTouchTarget(LocalLayout.current.primaryTouchTarget)
            .controlPress(interactionSource)
            .alpha(enabledAlpha(enabled))
            .controlRing(interactionSource, style.iconShape)
            .actionSurface(colors, style.iconShape, style.borderWidth)
            .size(style.height),
        contentAlignment = Alignment.Center,
    ) {
        BuilderIcon(icon, contentDescription = stateName(contentDescription, null, enabled), tint = colors.content)
    }
}
