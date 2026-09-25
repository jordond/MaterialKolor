package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType

/**
 * A labelled action in the surrounding skin.
 *
 * @param[onClick] Called when the button is pressed.
 * @param[label] What the button does, shown and read out.
 * @param[modifier] Applied to the button.
 * @param[emphasis] How loudly the button speaks. Keep [Emphasis.Primary] to one per screen.
 * @param[icon] A glyph before the label.
 * @param[enabled] Whether the button can be pressed.
 * @param[hint] A key that does the same, drawn as a keycap after the label, such as Space. It only
 * shows, so the button still reads out as [label].
 */
@Composable
public fun BuilderButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Secondary,
    icon: IconId? = null,
    enabled: Boolean = true,
    hint: String? = null, // b-510
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialButton(onClick, label, modifier, emphasis, icon, enabled, hint)
        Library.Unstyled -> HeadlessButton(
            onClick,
            label,
            UnstyledActionStyles.button,
            modifier,
            emphasis,
            icon,
            enabled,
            hint,
        )
        Library.Fluent -> FluentButton(onClick, label, modifier, emphasis, icon, enabled, hint)
        Library.Custom -> HeadlessButton(
            onClick,
            label,
            CustomActionStyles.button,
            modifier,
            emphasis,
            icon,
            enabled,
            hint,
        )
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
    hint: String? = null, // b-510
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
        if (hint != null) ButtonKeycap(hint, colors.content) // b-510
    }
}

// b-510

/**
 * A key drawn as a keycap inside a button, in [ink] on a faint wash of it. It only shows, since the
 * button's label already says what it does.
 */
@Composable
internal fun ButtonKeycap(
    key: String,
    ink: Color,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(percent = 50)
    val style = LocalBuilderType.current.value.merge(color = ink, fontSize = KeycapSize, lineHeight = KeycapLine)
    BasicText(
        text = key,
        modifier = Modifier
            .clearAndSetSemantics {}
            .background(ink.copy(alpha = KeycapWash), shape)
            .padding(horizontal = tokens.spacing.extraSmall + KeycapInset, vertical = KeycapInset),
        style = style,
        maxLines = 1,
    )
}

/** How strongly a keycap's wash takes its ink. */
private const val KeycapWash: Float = 0.16f

/** The room round a keycap's key, past the small spacing at its sides. */
private val KeycapInset: Dp = 2.dp

/** How big a keycap sets its key, smaller than the label beside it as the design has it. */
private val KeycapSize: TextUnit = 11.sp

/** The line a keycap's key takes. */
private val KeycapLine: TextUnit = 14.sp
