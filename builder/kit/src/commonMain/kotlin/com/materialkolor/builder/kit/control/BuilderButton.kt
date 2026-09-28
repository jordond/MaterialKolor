package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.ButtonStyle
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.headless.filledActionColors
import com.materialkolor.builder.kit.skin.material.MaterialButton
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.token.readableInk

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
 * @param[trailingIcon] A glyph after the label, the size of [icon]. It only shows, so whatever it says
 * belongs in the button's spoken name too. With one the label gives way first and ends in an
 * ellipsis, so the glyph always keeps its room.
 * @param[size] How much room the drawn button takes. The touch target stays the layout's in either
 * size, reaching past a compact button's edges.
 * @param[tonal] Fills a secondary or subtle button with the raised surface of whatever it stands on,
 * the way Material's tonal button fills, so a quiet action still reads as a button on a coloured
 * page. The ink keeps its contrast, since the raised surface is cut to carry it. Primary and danger
 * buttons are filled already and draw the same either way.
 * @param[fill] A colour of the caller's own to fill the button with, such as the colour a picker has
 * picked. The button then draws as a primary button in that fill whatever [emphasis] says, with its
 * label and glyphs in [readableInk] of it and a hairline of that ink round it, so a fill close to the
 * panel still shows. It presses and takes focus the way a primary button does. Unspecified keeps the
 * skin's own look.
 */
@Composable
public fun BuilderButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Secondary,
    icon: IconId? = null,
    enabled: Boolean = true,
    hint: String? = null,
    trailingIcon: IconId? = null,
    size: ButtonSize = ButtonSize.Regular,
    tonal: Boolean = false,
    fill: Color = Color.Unspecified,
) {
    when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> MaterialButton(
            onClick,
            label,
            modifier,
            emphasis,
            icon,
            enabled,
            hint,
            trailingIcon,
            size,
            tonal,
            fill,
        )
        SkinLibrary.Custom -> HeadlessButton(
            onClick,
            label,
            CustomActionStyles.button,
            modifier,
            emphasis,
            icon,
            enabled,
            hint,
            trailingIcon,
            size,
            tonal,
            fill,
        )
    }
}

/**
 * How much room a [BuilderButton] draws itself in.
 */
public enum class ButtonSize {
    /**
     * The skin's own button.
     */
    Regular,

    /**
     * A small pill for a tight row, such as the poster's header, with smaller glyphs and less room
     * round the label.
     */
    Compact,
}

/**
 * How tall a compact button draws in Material and Custom.
 */
internal val CompactButtonHeight: Dp = 34.dp

/**
 * The room at a compact button's sides.
 */
internal val CompactButtonPadding: Dp = 10.dp

/**
 * The room between a compact button's glyphs and its label.
 */
internal val CompactButtonGap: Dp = 4.dp

/**
 * How big a compact button draws its glyphs.
 */
internal val CompactButtonIcon: Dp = 16.dp

/**
 * A button drawn from [style] over plain foundation, for the skins without a button of their own. A
 * specified [fill] draws it in [filledActionColors] with a hairline outline.
 */
@Composable
internal fun HeadlessButton(
    onClick: () -> Unit,
    label: String,
    style: ButtonStyle,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Secondary,
    icon: IconId? = null,
    enabled: Boolean = true,
    hint: String? = null,
    trailingIcon: IconId? = null,
    size: ButtonSize = ButtonSize.Regular,
    tonal: Boolean = false,
    fill: Color = Color.Unspecified,
) {
    val compact = size == ButtonSize.Compact
    val tokens = LocalBuilderTokens.current
    val iconSize = if (compact) CompactButtonIcon else tokens.iconSize
    val filled = fill.isSpecified
    val colors = if (filled) filledActionColors(fill) else style.colors(emphasis, tonal)
    val borderWidth = if (filled) tokens.outlineWidth else style.borderWidth
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
            .actionSurface(colors, style.shape, borderWidth)
            .heightIn(min = if (compact) CompactButtonHeight else style.height)
            .padding(horizontal = if (compact) CompactButtonPadding else style.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) CompactButtonGap else style.gap,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) BuilderIcon(icon, contentDescription = null, tint = colors.content, size = iconSize)
        BuilderText(
            text = label,
            modifier = trailingLabel(trailingIcon),
            style = BuilderTextStyle.Label,
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hint != null) ButtonKeycap(hint, colors.content)
        if (trailingIcon != null) {
            BuilderIcon(trailingIcon, contentDescription = null, tint = colors.content, size = iconSize)
        }
    }
}

/**
 * Whether a button of [emphasis] takes the tonal fill it was asked for. Primary and danger buttons
 * are filled already.
 */
internal fun takesTonalFill(
    emphasis: Emphasis,
    tonal: Boolean,
): Boolean = tonal && (emphasis == Emphasis.Secondary || emphasis == Emphasis.Subtle)

/**
 * How a button's label sits beside a [trailingIcon]. With one it gives way first, so the glyph keeps
 * its room while the label ends in an ellipsis.
 */
internal fun RowScope.trailingLabel(trailingIcon: IconId?): Modifier =
    if (trailingIcon == null) Modifier else Modifier.weight(1f, fill = false)

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

/**
 * How strongly a keycap's wash takes its ink.
 */
private const val KeycapWash: Float = 0.16f

/**
 * The room round a keycap's key, past the small spacing at its sides.
 */
private val KeycapInset: Dp = 2.dp

/**
 * How big a keycap sets its key, smaller than the label beside it.
 */
private val KeycapSize: TextUnit = 11.sp

/**
 * The line a keycap's key takes.
 */
private val KeycapLine: TextUnit = 14.sp
