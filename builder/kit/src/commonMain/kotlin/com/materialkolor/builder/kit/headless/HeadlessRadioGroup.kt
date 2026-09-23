package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.composeunstyled.UnstyledRadioButton
import com.composeunstyled.UnstyledRadioGroup

/**
 * A single choice row over Compose Unstyled's radio group, with roving focus.
 *
 * Tab lands on the chosen option only. The arrow keys, Home and End move the choice and the focus
 * together, the way a native radio group behaves, and wrap at either end. Every option gets the same
 * width, the width of the widest one, unless [modifier] stretches the row.
 *
 * @param[options] What there is to choose from.
 * @param[selected] The current choice.
 * @param[onSelect] Called with the option the user picked.
 * @param[label] What the group is for, read out when focus enters it.
 * @param[modifier] Applied to the group.
 * @param[enabled] Whether any option can be picked.
 * @param[arrangement] How the options sit along the row.
 * @param[option] Draws one option. The interaction source is the option's own, for press and focus
 * feedback.
 */
@Composable
internal fun <T> HeadlessRadioGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    option: @Composable (value: T, isSelected: Boolean, interactionSource: MutableInteractionSource) -> Unit,
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    UnstyledRadioGroup(
        value = selected,
        onValueChange = onSelect,
        modifier = modifier,
        accessibilityLabel = label,
    ) {
        Row(
            modifier = Modifier.width(IntrinsicSize.Min),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isSelected = index == selectedIndex
                    UnstyledRadioButton(
                        value = value,
                        modifier = Modifier
                            .weight(1f)
                            .radioGroupOption(focus, index, selectedIndex, rtl) { target -> onSelect(options[target]) }
                            .semantics { this.selected = isSelected },
                        enabled = enabled,
                        interactionSource = interactionSource,
                    ) {
                        option(value, isSelected, interactionSource)
                    }
                }
            }
        }
    }
}

/**
 * The focus half of a radio group, shared by the headless group and any library group that lacks
 * arrow keys of its own.
 */
@Stable
internal class RadioGroupFocus internal constructor(
    count: Int,
) {
    internal val requesters: List<FocusRequester> = List(count) { FocusRequester() }

    /** Set by an arrow key, so the focus follows the choice it made once the choice lands. */
    internal var followSelection: Boolean = false
}

/**
 * Focus state for a group of [count] options with [selectedIndex] chosen.
 *
 * When an arrow key moved the choice, the focus moves onto the new choice as soon as the caller
 * hands it back, and not before, since only the chosen option can take focus.
 */
@Composable
internal fun rememberRadioGroupFocus(
    count: Int,
    selectedIndex: Int,
): RadioGroupFocus {
    val focus = remember(count) { RadioGroupFocus(count) }
    LaunchedEffect(focus, selectedIndex) {
        if (focus.followSelection && selectedIndex in focus.requesters.indices) {
            focus.followSelection = false
            focus.requesters[selectedIndex].requestFocus()
        }
    }
    return focus
}

/**
 * Makes the option at [index] a stop of the roving focus in [focus].
 *
 * Only the chosen option, or the first when nothing is chosen, can take focus. Left and right follow
 * the reading direction, up and down always go back and forth, and Home and End jump to either end.
 * [onMove] gets the index of the option the key moved to.
 */
internal fun Modifier.radioGroupOption(
    focus: RadioGroupFocus,
    index: Int,
    selectedIndex: Int,
    rtl: Boolean,
    onMove: (Int) -> Unit,
): Modifier {
    val count = focus.requesters.size
    val tabStop = if (selectedIndex in 0 until count) selectedIndex else 0
    return focusRequester(focus.requesters[index])
        .focusProperties { canFocus = index == tabStop }
        .onKeyEvent { event ->
            val forward = if (rtl) -1 else 1
            val target = when (event.key) {
                Key.DirectionRight -> index + forward
                Key.DirectionLeft -> index - forward
                Key.DirectionDown -> index + 1
                Key.DirectionUp -> index - 1
                Key.MoveHome -> 0
                Key.MoveEnd -> count - 1
                else -> return@onKeyEvent false
            }.mod(count)
            if (event.type == KeyEventType.KeyDown && target != index) {
                focus.followSelection = true
                onMove(target)
            }
            true
        }
}
