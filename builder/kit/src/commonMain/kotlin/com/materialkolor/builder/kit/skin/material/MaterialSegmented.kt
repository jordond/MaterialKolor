package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.headless.radioGroupOption
import com.materialkolor.builder.kit.headless.rememberRadioGroupFocus
import com.materialkolor.builder.kit.icon.IconId

/**
 * Material's single choice segmented row, with the radio group's roving focus and arrow keys laid
 * over it, since Material's row moves neither. B-402 swaps in the expressive button group.
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
 * Where [keep] is set, holds one button in a box of its own that takes the button's share of the
 * row. Material places the chosen button above the others, and siblings are read in that order, so
 * a box per button leaves only one child to each and the boxes in the order they are written.
 */
@Composable
private fun SingleChoiceSegmentedButtonRowScope.InWritingOrder(
    keep: Boolean,
    button: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit,
) {
    if (!keep) {
        button()
        return
    }
    Box(Modifier.weight(1f), propagateMinConstraints = true) { this@InWritingOrder.button() }
}
