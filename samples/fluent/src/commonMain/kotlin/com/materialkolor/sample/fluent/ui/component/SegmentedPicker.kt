package com.materialkolor.sample.fluent.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.SegmentedButton
import io.github.composefluent.component.SegmentedControl
import io.github.composefluent.component.SegmentedItemPosition

/**
 * A Fluent [SegmentedControl] that picks one of [options].
 *
 * Fluent's [SegmentedButton] does not say whether it is the checked one, so each button here adds `selected` and a
 * radio button role to the node that owns its click.
 *
 * @param[options] What there is to pick from, in order.
 * @param[selected] The option that is picked now.
 * @param[onSelect] Called with the option the user picks.
 * @param[testTag] The test tag of each option.
 * @param[modifier] The modifier for the control.
 * @param[icon] What each option shows before its text.
 * @param[text] What each option says.
 */
@Composable
internal fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    testTag: (T) -> String,
    modifier: Modifier = Modifier,
    icon: (@Composable (T) -> Unit)? = null,
    text: @Composable (T) -> Unit,
) {
    SegmentedControl(modifier = modifier.selectableGroup()) {
        options.forEachIndexed { index, option ->
            key(option) {
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val checked = option == selected

                SegmentedButton(
                    checked = checked,
                    onCheckedChanged = { onSelect(option) },
                    modifier = Modifier
                        .testTag(testTag(option))
                        .semantics {
                            this.selected = checked
                            role = Role.RadioButton
                        }
                        .focusStroke(
                            visible = focused,
                            color = FluentTheme.colors.stroke.focus.outer,
                            shape = FluentTheme.shapes.control,
                        ),
                    position = when (index) {
                        0 -> SegmentedItemPosition.Start
                        options.lastIndex -> SegmentedItemPosition.End
                        else -> SegmentedItemPosition.Center
                    },
                    interactionSource = interaction,
                    icon = icon?.let { content -> { content(option) } },
                    text = { text(option) },
                )
            }
        }
    }
}
