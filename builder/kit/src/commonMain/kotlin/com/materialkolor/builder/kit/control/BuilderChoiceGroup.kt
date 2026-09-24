package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
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
 * they run out of width.
 *
 * Each option draws its own selectable node, with its role and selected state, and puts the
 * modifier it is handed on that node, or on a wrapper around it with no focus of its own. The
 * group's name goes in as text on the web, where the group has no role (S5 answer 1).
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
    option: @Composable (value: T, isSelected: Boolean, optionModifier: Modifier) -> Unit,
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val asText = LocalFoldsStateIntoName.current
    val spacing = LocalBuilderTokens.current.spacing.small
    FlowRow(
        modifier = modifier
            .selectableGroup()
            .semantics { roleLessName(label, asText) },
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, value ->
            key(index) {
                val optionModifier = Modifier.radioGroupOption(
                    focus = focus,
                    index = index,
                    selectedIndex = selectedIndex,
                    rtl = rtl,
                    selectOnFocus = selectOnFocus,
                ) { target -> onSelect(options[target]) }
                option(value, index == selectedIndex, optionModifier)
            }
        }
    }
}
