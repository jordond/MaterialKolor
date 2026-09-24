package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.headless.RadioGroupFocus
import com.materialkolor.builder.kit.headless.radioGroupOption
import com.materialkolor.builder.kit.headless.rememberRadioGroupFocus
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.LocalSkin

/**
 * Material's single choice segmented row, with the radio group's roving focus and arrow keys laid
 * over it, since Material's row moves neither. The expressive flavour draws a row of connected toggle
 * buttons instead ([ExpressiveSegmented]).
 *
 * Material raises the chosen button over its neighbours, and the web mirror reads the page in that
 * order, so the chosen option would come last. On the web each button sits in a box of its own,
 * which keeps the options in the order they are written (S5 row 8).
 */
@Composable
internal fun <T> MaterialSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    selectOnFocus: Boolean,
    optionLabel: (T) -> String,
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    if (LocalSkin.current.expressive) {
        ExpressiveSegmented(
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            label = label,
            modifier = modifier,
            enabled = enabled,
            optionIcon = optionIcon,
            selectOnFocus = selectOnFocus,
            optionLabel = optionLabel,
            focus = focus,
        )
        return
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val folds = LocalFoldsStateIntoName.current
    val writingOrder = LocalWebKeyboard.current
    MaterialTarget {
        SingleChoiceSegmentedButtonRow(modifier.semantics { roleLessName(label, folds) }) {
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    val glyph = optionIcon(value)
                    val isSelected = index == selectedIndex
                    InWritingOrder(keep = writingOrder) {
                        SegmentedButton(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            shape = shape,
                            modifier = Modifier
                                .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { target ->
                                    onSelect(options[target])
                                }.foldState(
                                    name = optionLabel(value),
                                    state = ControlState.Selected(isSelected),
                                    enabled = enabled,
                                    role = FoldedRole.Radio,
                                ).materialFeedback(interactionSource, shape),
                            enabled = enabled,
                            interactionSource = interactionSource,
                            icon = {
                                SegmentedButtonDefaults.Icon(
                                    active = isSelected,
                                    activeContent = {
                                        BuilderIcon(
                                            IconId.Check,
                                            contentDescription = null,
                                            tint = LocalContentColor.current,
                                        )
                                    },
                                    inactiveContent = glyph?.let { id ->
                                        { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
                                    },
                                )
                            },
                        ) {
                            BuilderText(
                                optionLabel(value),
                                style = BuilderTextStyle.Label,
                                color = LocalContentColor.current,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The expressive row, Material's connected toggle buttons laid out by hand. Material's own button
 * group folds whatever does not fit into an overflow menu, a popup the web may not open (D40), and
 * nothing turns that off, so the row takes only the group's connected shapes.
 *
 * Each option still reads as a radio button with the radio group's roving focus over it, where the
 * toggle button on its own would read as a checkbox. The chosen option wears the check as well as
 * the fill and the rounder shape, so the choice never rests on colour alone. The focused option is
 * raised over its neighbours, since its ring reaches past the gap between them.
 */
@Composable
private fun <T> ExpressiveSegmented(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    selectOnFocus: Boolean,
    optionLabel: (T) -> String,
    focus: RadioGroupFocus,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val folds = LocalFoldsStateIntoName.current
    val writingOrder = LocalWebKeyboard.current
    MaterialTarget {
        Row(
            modifier = modifier
                .semantics { roleLessName(label, folds) }
                .selectableGroup()
                .width(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused by interactionSource.collectIsFocusedAsState()
                    val shapes = connectedShapes(index, options.size)
                    val isSelected = index == selectedIndex
                    val name = optionLabel(value)
                    InWritingOrder(keep = writingOrder) {
                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { onSelect(value) },
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (focused) 1f else 0f)
                                .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { target ->
                                    onSelect(options[target])
                                }.semantics {
                                    role = Role.RadioButton
                                    selected = isSelected
                                }.foldState(
                                    name = name,
                                    state = ControlState.Selected(isSelected),
                                    enabled = enabled,
                                    role = FoldedRole.Radio,
                                ).materialFeedback(
                                    interactionSource,
                                    if (isSelected) shapes.checkedShape else shapes.shape,
                                ),
                            enabled = enabled,
                            shapes = shapes,
                            interactionSource = interactionSource,
                        ) {
                            val glyph = if (isSelected) IconId.Check else optionIcon(value)
                            if (glyph != null) {
                                BuilderIcon(glyph, contentDescription = null, tint = LocalContentColor.current)
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            }
                            // A label is as wide as its whole line even where the row asks for its least.
                            BuilderText(
                                name,
                                modifier = Modifier.width(IntrinsicSize.Max),
                                style = BuilderTextStyle.Label,
                                color = LocalContentColor.current,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The connected shapes for the option at [index] of [count], by where it sits in the row. */
@Composable
private fun connectedShapes(
    index: Int,
    count: Int,
): ToggleButtonShapes {
    val full = ButtonGroupDefaults.connectedButtonCheckedShape
    return when {
        count == 1 -> ToggleButtonShapes(shape = full, pressedShape = full, checkedShape = full)
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
}

/**
 * Where [keep] is set, holds one button in a box of its own that takes the button's share of the
 * row. Material places the chosen button above the others, and siblings are read in that order, so
 * a box per button leaves only one child to each and the boxes in the order they are written.
 */
@Composable
private fun <S : RowScope> S.InWritingOrder(
    keep: Boolean,
    button: @Composable S.() -> Unit,
) {
    if (!keep) {
        button()
        return
    }
    Box(Modifier.weight(1f), propagateMinConstraints = true) { this@InWritingOrder.button() }
}
