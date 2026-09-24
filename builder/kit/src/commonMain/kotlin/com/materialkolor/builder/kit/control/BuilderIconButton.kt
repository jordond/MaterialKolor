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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
 * @param[expanded] Whether the panel this button shows and hides is open, for a button that
 * discloses one. It is read as the button's state, and on the web it travels in the name as well.
 * Left null, the button has no state.
 */
@Composable
public fun BuilderIconButton(
    onClick: () -> Unit,
    icon: IconId,
    contentDescription: String,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Subtle,
    enabled: Boolean = true,
    expanded: Boolean? = null,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialIconButton(onClick, icon, contentDescription, modifier, emphasis, enabled, expanded)
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
                expanded,
            )
        }
        Library.Fluent -> {
            // fluent-placeholder
            FluentIconButton(onClick, icon, contentDescription, modifier, emphasis, enabled, expanded)
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
                expanded,
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
    expanded: Boolean? = null,
) {
    val colors = style.colors(emphasis)
    val interactionSource = remember { MutableInteractionSource() }
    val spoken = iconButtonSemantics(contentDescription, enabled, expanded)
    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).then(spoken.state)
            .controlTouchTarget(LocalLayout.current.primaryTouchTarget)
            .controlPress(interactionSource)
            .alpha(enabledAlpha(enabled))
            .controlRing(interactionSource, style.iconShape)
            .actionSurface(colors, style.iconShape, style.borderWidth)
            .size(style.height),
        contentAlignment = Alignment.Center,
    ) {
        BuilderIcon(icon, contentDescription = spoken.name, tint = colors.content)
    }
}

/**
 * What an icon button reads out, the [name] its glyph carries and the [state] modifier that sets
 * the button's state description.
 */
internal class IconButtonSemantics(
    val name: String,
    val state: Modifier,
)

/**
 * The name and state of an icon button whose panel is open when [expanded] is true and closed when
 * it is false. Left null, the name is [contentDescription] with only the disabled note folded in,
 * and the state adds nothing.
 */
@Composable
internal fun iconButtonSemantics(
    contentDescription: String,
    enabled: Boolean,
    expanded: Boolean?,
): IconButtonSemantics {
    if (expanded == null) return IconButtonSemantics(stateName(contentDescription, null, enabled), Modifier)
    val state = ControlState.Expanded(expanded)
    val words = stateWords()
    val spoken = words.of(state)
    return IconButtonSemantics(
        name = stateName(contentDescription, state, enabled, words),
        state = Modifier.semantics { stateDescription = spoken },
    )
}
