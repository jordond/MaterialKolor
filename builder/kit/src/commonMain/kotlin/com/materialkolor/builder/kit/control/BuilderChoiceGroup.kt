package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.headless.radioGroupOption
import com.materialkolor.builder.kit.headless.rememberRadioGroupFocus
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A single choice whose options the caller draws, such as a row of scheme chips.
 *
 * It keeps the keyboard of [BuilderChoiceChips]. The group is one Tab stop, on the chosen option or
 * the first when nothing is chosen. The arrow keys follow the reading direction and wrap at either
 * end, and Home and End jump to the first and last option. The options wrap onto more lines when
 * they run out of width, or with [columns] set they sit in a grid of that many equal cells a row.
 *
 * Each option draws its own selectable node, with its role and selected state, and puts the
 * modifier it is handed on that node, or on a wrapper around it with no focus of its own. The
 * group's name goes in as text on the web, where the group has no role.
 *
 * Use [BuilderChoiceChips] where a label is all an option shows.
 *
 * @param[options] What there is to choose from.
 * @param[selected] The current choice. One that is not among [options] leaves nothing chosen.
 * @param[onSelect] Called with the option the arrow keys, Home or End move to. A click, Enter or
 * Space lands on the option's own node, so the option calls this from its own click too.
 * @param[label] What the choice is about, read out for the group.
 * @param[modifier] Applied to the group.
 * @param[selectOnFocus] Whether the arrow keys choose as they move, or only move the focus. Turn it
 * off where every choice costs something, and Enter or Space on an option chooses it.
 * @param[columns] How many equal cells each row holds, or 0 to let the options flow at their own
 * widths. Each option sits at the top centre of its cell, so no cell's width depends on its option.
 * @param[option] Draws one option, given its value, whether it is the chosen one and the modifier
 * that makes it a stop of the group's focus. Its own click has to call [onSelect].
 */
@Composable
public fun <T> BuilderChoiceGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    selectOnFocus: Boolean = true,
    columns: Int = 0,
    option: @Composable (value: T, isSelected: Boolean, optionModifier: Modifier) -> Unit,
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val asText = LocalFoldsStateIntoName.current
    val spacing = LocalBuilderTokens.current.spacing
    val group = modifier
        .selectableGroup()
        .semantics { roleLessName(label, asText) }
    val each: @Composable (index: Int) -> Unit = { index ->
        key(index) {
            val optionModifier = Modifier.radioGroupOption(
                focus = focus,
                index = index,
                selectedIndex = selectedIndex,
                rtl = rtl,
                selectOnFocus = selectOnFocus,
            ) { target -> onSelect(options[target]) }
            option(options[index], index == selectedIndex, optionModifier)
        }
    }
    if (columns > 0) {
        ChoiceGrid(options.size, columns, group, rowGap = spacing.medium, each)
        return
    }
    FlowRow(
        modifier = group,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        options.indices.forEach { index -> each(index) }
    }
}

/**
 * [count] options in rows of [columns] equal cells, each option at the top centre of its cell. A
 * short last row keeps the cells as wide as the rows above.
 */
@Composable
private fun ChoiceGrid(
    count: Int,
    columns: Int,
    modifier: Modifier,
    rowGap: Dp,
    each: @Composable (index: Int) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(rowGap)) {
        (0 until count).chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { index ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { each(index) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
