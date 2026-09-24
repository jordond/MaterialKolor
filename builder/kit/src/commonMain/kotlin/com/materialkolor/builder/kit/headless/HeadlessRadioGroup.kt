package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.composeunstyled.UnstyledRadioButton
import com.composeunstyled.UnstyledRadioGroup
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName

/**
 * A single choice row over Compose Unstyled's radio group, with roving focus.
 *
 * Tab lands on the chosen option only. The arrow keys, Home and End move the choice and the focus
 * together, the way a native radio group behaves, and wrap at either end. With [selectOnFocus] off
 * they move only the focus, and Enter or Space chooses the focused option. Every option gets the
 * same width, the width of the widest one, unless [modifier] stretches the group, and then the
 * options share the stretched width evenly.
 *
 * The group's name goes in as text on the web, where the group has no role (S5 answer 1).
 *
 * @param[options] What there is to choose from.
 * @param[selected] The current choice. One that is not among [options] leaves nothing chosen.
 * @param[onSelect] Called with the option the user picked.
 * @param[label] What the group is for, read out when focus enters it.
 * @param[modifier] Applied to the group.
 * @param[enabled] Whether any option can be picked.
 * @param[arrangement] How the options sit along the row.
 * @param[selectOnFocus] Whether the arrow keys choose as they move, or only move the focus.
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
    selectOnFocus: Boolean = true,
    option: @Composable (value: T, isSelected: Boolean, interactionSource: MutableInteractionSource) -> Unit,
) {
    RadioGroupFrame(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = label,
        modifier = modifier.width(IntrinsicSize.Min),
        enabled = enabled,
        selectOnFocus = selectOnFocus,
        option = option,
    ) { eachOption ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            eachOption(Modifier.weight(1f))
        }
    }
}

/**
 * The same single choice group as [HeadlessRadioGroup], laid out as a row that wraps onto more lines
 * when it runs out of width. Each option is as wide as its own content.
 *
 * @param[spacing] The gap between two options, along a line and between lines.
 */
@Composable
internal fun <T> HeadlessRadioFlow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    spacing: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectOnFocus: Boolean = true,
    option: @Composable (value: T, isSelected: Boolean, interactionSource: MutableInteractionSource) -> Unit,
) {
    RadioGroupFrame(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = label,
        modifier = modifier,
        enabled = enabled,
        selectOnFocus = selectOnFocus,
        option = option,
    ) { eachOption ->
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            eachOption(Modifier)
        }
    }
}

/**
 * The radio group both layouts share. [layout] places the options by calling the lambda it gets,
 * with the modifier every option takes from its parent.
 */
@Composable
private fun <T> RadioGroupFrame(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    selectOnFocus: Boolean,
    option: @Composable (value: T, isSelected: Boolean, interactionSource: MutableInteractionSource) -> Unit,
    layout: @Composable (eachOption: @Composable (Modifier) -> Unit) -> Unit,
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val asText = LocalFoldsStateIntoName.current
    UnstyledRadioGroup(
        value = selected,
        onValueChange = onSelect,
        modifier = modifier.semantics { roleLessName(label, asText) },
    ) {
        layout { placement ->
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isSelected = index == selectedIndex
                    UnstyledRadioButton(
                        value = value,
                        modifier = placement
                            .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { target ->
                                onSelect(options[target])
                            }.semantics { this.selected = isSelected },
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

    /**
     * The option a key last asked for, or [NoRequest]. The focus follows the choice only when the
     * choice lands on this option, so a key the caller turned down never pulls focus back into the
     * group on some later, unrelated change such as an undo.
     */
    internal var requestedIndex: Int = NoRequest

    /**
     * The option a key moved the focus to without choosing it, or [NoRequest]. It can take focus
     * besides the chosen one until focus leaves it for somewhere outside the group.
     */
    internal var focusedIndex: Int by mutableIntStateOf(NoRequest)

    internal companion object {
        /** No key has asked for an option since the choice last changed. */
        internal const val NoRequest: Int = -1
    }
}

/**
 * Focus state for a group of [count] options with [selectedIndex] chosen.
 *
 * When a key moved the choice, the focus moves onto the new choice as soon as the caller hands it
 * back, and not before, since only the chosen option can take focus. Any change of choice uses up
 * the request, whether it follows or not.
 */
@Composable
internal fun rememberRadioGroupFocus(
    count: Int,
    selectedIndex: Int,
): RadioGroupFocus {
    val focus = remember(count) { RadioGroupFocus(count) }
    LaunchedEffect(focus, selectedIndex) {
        val requested = focus.requestedIndex
        focus.requestedIndex = RadioGroupFocus.NoRequest
        if (requested == selectedIndex && selectedIndex in focus.requesters.indices) {
            focus.requesters[selectedIndex].requestFocus()
        }
    }
    return focus
}

/**
 * Makes the option at [index] a stop of the roving focus in [focus].
 *
 * Only the chosen option, or the first when nothing is chosen, can take focus from Tab. Left and
 * right follow the reading direction, up and down always go back and forth, and Home and End jump
 * to either end. With [selectOnFocus] a key hands [onMove] the index of the option it moved to, and
 * the focus follows once that option is chosen. Without it a key moves only the focus, and the
 * option's own Enter and Space choose it.
 */
internal fun Modifier.radioGroupOption(
    focus: RadioGroupFocus,
    index: Int,
    selectedIndex: Int,
    rtl: Boolean,
    selectOnFocus: Boolean = true,
    onMove: (Int) -> Unit,
): Modifier {
    val count = focus.requesters.size
    val tabStop = if (selectedIndex in 0 until count) selectedIndex else 0
    return focusRequester(focus.requesters[index])
        .focusProperties { canFocus = index == tabStop || index == focus.focusedIndex }
        .onFocusChanged { state ->
            if (!state.isFocused && focus.focusedIndex == index) focus.focusedIndex = RadioGroupFocus.NoRequest
        }.onKeyEvent { event ->
            val target = rovingTarget(event.key, index, count, rtl, upDown = true, homeEnd = true)
                ?: return@onKeyEvent false
            if (event.type == KeyEventType.KeyDown && target != index) {
                if (selectOnFocus) {
                    focus.requestedIndex = target
                    onMove(target)
                } else {
                    focus.focusedIndex = target
                    focus.requesters[target].requestFocus()
                }
            }
            true
        }
}
