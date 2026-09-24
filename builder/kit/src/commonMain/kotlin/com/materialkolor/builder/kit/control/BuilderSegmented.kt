package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessRadioGroup
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentSegmented
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.SegmentedStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialSegmented

/**
 * A short row of mutually exclusive options, such as Light, Split and Dark.
 *
 * It reads out as a radio group named [label]. Tab lands on the chosen option and the arrow keys
 * move the choice. The chosen option carries a check as well as its fill, so the choice never rests
 * on colour alone.
 *
 * Where every choice costs something, such as the library switcher's reskin and undo entry, turn
 * [selectOnFocus] off. The arrow keys then only move the focus, and Enter or Space chooses the
 * focused option.
 *
 * @param[options] What there is to choose from, a handful at most so every label fits.
 * @param[selected] The current choice. One that is not among [options], say for a frame while the
 * options catch up, leaves nothing chosen.
 * @param[onSelect] Called with the option the user picked.
 * @param[label] What the choice is about, read out for the group.
 * @param[modifier] Applied to the group.
 * @param[enabled] Whether the choice can change.
 * @param[optionIcon] A glyph for an option, or null for a label alone.
 * @param[selectOnFocus] Whether the arrow keys choose as they move, or only move the focus.
 * @param[optionLabel] The label of an option.
 */
@Composable
public fun <T> BuilderSegmented(
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
        Library.Material3 -> {
            MaterialSegmented(
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
        Library.Unstyled -> {
            HeadlessSegmented(
                options,
                selected,
                onSelect,
                label,
                UnstyledActionStyles.segmented,
                modifier,
                enabled,
                optionIcon,
                selectOnFocus,
                optionLabel,
            )
        }
        Library.Fluent -> {
            FluentSegmented(
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
        Library.Custom -> {
            HeadlessSegmented(
                options,
                selected,
                onSelect,
                label,
                CustomActionStyles.segmented,
                modifier,
                enabled,
                optionIcon,
                selectOnFocus,
                optionLabel,
            )
        }
    }
}

/** A segmented control drawn from [style] over [HeadlessRadioGroup]. */
@Composable
internal fun <T> HeadlessSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    style: SegmentedStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionIcon: (T) -> IconId? = { null },
    selectOnFocus: Boolean = true,
    optionLabel: (T) -> String,
) {
    val target = LocalLayout.current.primaryTouchTarget
    val option = style.option
    HeadlessRadioGroup(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = label,
        modifier = modifier
            .alpha(enabledAlpha(enabled))
            .actionSurface(style.colors, style.shape, style.borderWidth)
            .padding(style.inset),
        enabled = enabled,
        selectOnFocus = selectOnFocus,
    ) { value, isSelected, interactionSource ->
        val colors = option.colors(isSelected)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .foldState(optionLabel(value), ControlState.Selected(isSelected), enabled, role = FoldedRole.Radio)
                .controlTouchTarget(target)
                .controlPress(interactionSource)
                .controlRing(interactionSource, segmentRingShape(options, value, style))
                .actionSurface(colors, option.shape, option.borderWidth)
                .heightIn(min = option.height)
                .padding(horizontal = option.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(option.gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = if (isSelected) IconId.Check else optionIcon(value)
            if (icon != null) BuilderIcon(icon, contentDescription = null, tint = colors.content)
            BuilderText(optionLabel(value), style = BuilderTextStyle.Label, color = colors.content, maxLines = 1)
        }
    }
}

// b-230d

/**
 * The outline the focus ring follows round [value]. At either end of the row it reaches the style's
 * [SegmentedStyle.endRingOffset] off the rounded end alone, and the ring's straight top and bottom
 * stay in line with the middle options' rings.
 */
private fun <T> segmentRingShape(
    options: List<T>,
    value: T,
    style: SegmentedStyle,
): Shape {
    val reach = (style.endRingOffset - FocusRingOffset).coerceAtLeast(0.dp)
    val start = if (value == options.first()) reach else 0.dp
    val end = if (value == options.last()) reach else 0.dp
    if (start == 0.dp && end == 0.dp) return style.option.shape
    return EndReachShape(style.option.shape, start, end)
}

/**
 * [shape] drawn [start] further out at its start edge and [end] further out at its end edge, with its
 * corners as they are. A ring round it stands further off those two edges only.
 */
@Immutable
private data class EndReachShape(
    val shape: Shape,
    val start: Dp,
    val end: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val bounds = when (val outline = shape.createOutline(size, layoutDirection, density)) {
            is Outline.Rounded -> outline.roundRect
            is Outline.Rectangle -> RoundRect(outline.rect, CornerRadius.Zero)
            // Every skin's options are rounded rectangles, so no option takes this path and loses its reach.
            is Outline.Generic -> return outline
        }
        val ltr = layoutDirection == LayoutDirection.Ltr
        val left = with(density) { (if (ltr) start else end).toPx() }
        val right = with(density) { (if (ltr) end else start).toPx() }
        return Outline.Rounded(bounds.copy(left = bounds.left - left, right = bounds.right + right))
    }
}
