package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.materialkolor.builder.kit.headless.HeadlessRadioFlow
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.SelectableStyle
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialChoiceChips
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A row of chips where exactly one is chosen, such as the palette styles.
 *
 * It reads out as a radio group named [label], each chip a radio button with its selected state. Tab
 * lands on the chosen chip, the arrow keys move the choice and wrap at either end, and Home and End
 * jump to the first and last chip. The chosen chip carries a check as well as its fill, so the
 * choice never rests on colour alone. The row wraps onto more lines when it runs out of
 * width.
 *
 * Where every choice costs something, such as a reskin and an undo entry, turn [selectOnFocus] off.
 * The arrow keys then only move the focus, and Enter or Space chooses the focused chip.
 *
 * Use [BuilderFilterChip] instead where several chips can be on together.
 *
 * @param[options] What there is to choose from.
 * @param[selected] The current choice. One that is not among [options] leaves nothing chosen.
 * @param[onSelect] Called with the option the user picked.
 * @param[label] What the choice is about, read out for the group.
 * @param[modifier] Applied to the group.
 * @param[enabled] Whether the choice can change.
 * @param[optionIcon] A glyph for an option while it is not chosen, or null for a label alone.
 * @param[selectOnFocus] Whether the arrow keys choose as they move, or only move the focus.
 * @param[optionLabel] The label of an option.
 */
@Composable
public fun <T> BuilderChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionIcon: (T) -> IconId? = { null },
    selectOnFocus: Boolean = true,
    optionLabel: (T) -> String,
) {
    when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> {
            MaterialChoiceChips(
                options,
                selected,
                onSelect,
                label,
                modifier,
                enabled,
                optionIcon,
                selectOnFocus,
                optionLabel,
            )
        }
        SkinLibrary.Custom -> {
            HeadlessChoiceChips(
                options,
                selected,
                onSelect,
                label,
                CustomActionStyles.chip,
                modifier,
                enabled,
                optionIcon,
                selectOnFocus,
                optionLabel,
            )
        }
    }
}

/**
 * Choice chips drawn from the chip [style] over [HeadlessRadioFlow].
 */
@Composable
internal fun <T> HeadlessChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    style: SelectableStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionIcon: (T) -> IconId? = { null },
    selectOnFocus: Boolean = true,
    optionLabel: (T) -> String,
) {
    val target = LocalLayout.current.primaryTouchTarget
    HeadlessRadioFlow(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = label,
        spacing = LocalBuilderTokens.current.spacing.small,
        modifier = modifier.alpha(enabledAlpha(enabled)),
        enabled = enabled,
        selectOnFocus = selectOnFocus,
    ) { value, isSelected, interactionSource ->
        val colors = style.colors(isSelected)
        Row(
            modifier = Modifier
                .foldState(optionLabel(value), ControlState.Selected(isSelected), enabled, role = FoldedRole.Radio)
                .controlTouchTarget(target)
                .controlPress(interactionSource)
                .controlRing(interactionSource, style.shape)
                .actionSurface(colors, style.shape, style.borderWidth)
                .heightIn(min = style.height)
                .padding(horizontal = style.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(style.gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val glyph = if (isSelected) IconId.Check else optionIcon(value)
            if (glyph != null) BuilderIcon(glyph, contentDescription = null, tint = colors.content)
            BuilderText(optionLabel(value), style = BuilderTextStyle.Label, color = colors.content, maxLines = 1)
        }
    }
}
