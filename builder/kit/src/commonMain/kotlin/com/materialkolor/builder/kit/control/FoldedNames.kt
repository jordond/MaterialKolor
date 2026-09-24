package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

// The siblings of [foldedToggleName] for controls an app draws for itself, where kit controls do
// not fit. Each names its node the way the kit's own control of that kind reads, off the web and on
// it, where the mirror drops the state and reads every clickable as a button (P3, D37, D40).

/**
 * Names a button that shows and hides a panel of its own, such as a row that opens its details,
 * and carries whether that panel is open onto the web.
 *
 * Off the web [name] becomes the node's content description, and whether it is open its state
 * description. On the web the state travels in the name too, "Filters, expanded".
 *
 * @param[name] What the button stands for, read out as its name.
 * @param[expanded] Whether its panel is open.
 * @param[enabled] Whether it takes input. On the web a disabled button says so in its name.
 */
@Composable
public fun Modifier.foldedExpandedName(
    name: String,
    expanded: Boolean,
    enabled: Boolean = true,
): Modifier {
    val state = ControlState.Expanded(expanded)
    val words = stateWords()
    val spoken = stateName(name, state, enabled, words)
    val stateText = words.of(state)
    return semantics {
        contentDescription = spoken
        stateDescription = stateText
    }
}

/**
 * Names one tab of a tab row an app draws for itself. Put it after the tab's `selectable` with the
 * tab role, which reports whether it is the current one off the web. On the web the tab word and
 * that state travel in the name, "Orders, tab, selected".
 *
 * @param[name] What the tab shows, read out as its name.
 * @param[selected] Whether it is the current tab.
 * @param[enabled] Whether it takes input. On the web a disabled tab says so in its name.
 */
@Composable
public fun Modifier.foldedTabName(
    name: String,
    selected: Boolean,
    enabled: Boolean = true,
): Modifier = foldedName(name, FoldedRole.Tab, ControlState.Selected(selected), enabled)

/**
 * Names one choice of a single choice an app draws for itself, a chip or a segment. Put it after
 * the choice's `selectable` with the radio button role. On the web the radio word and whether it
 * is chosen travel in the name, "Oat, radio, selected".
 *
 * @param[name] What the choice shows, read out as its name.
 * @param[selected] Whether it is the one chosen.
 * @param[enabled] Whether it takes input. On the web a disabled choice says so in its name.
 */
@Composable
public fun Modifier.foldedChoiceName(
    name: String,
    selected: Boolean,
    enabled: Boolean = true,
): Modifier = foldedName(name, FoldedRole.Radio, ControlState.Selected(selected), enabled)

/**
 * Names one option of a list an app opens for itself to pick a value from, the way a select's
 * options read. Put it after the option's `selectable`. On the web the option word and whether it
 * is the chosen one travel in the name, "Weekly, option, selected".
 *
 * @param[name] What the option shows, read out as its name.
 * @param[selected] Whether it is the one chosen.
 * @param[enabled] Whether it takes input. On the web a disabled option says so in its name.
 */
@Composable
public fun Modifier.foldedOptionName(
    name: String,
    selected: Boolean,
    enabled: Boolean = true,
): Modifier = foldedName(name, FoldedRole.Option, ControlState.Selected(selected), enabled)

/**
 * Names one row of a menu an app draws for itself. On the web the menu item word travels in the
 * name, "Duplicate, menu item", and for a row that is the current one of a set whether it is
 * checked, "Dark, menu item, checked", the way the kit's menu rows read.
 *
 * @param[name] What the row says, read out as its name.
 * @param[checked] Whether the row is the current one of a set, or null for a plain command.
 * @param[enabled] Whether it takes input. On the web a disabled row says so in its name.
 */
@Composable
public fun Modifier.foldedMenuItemName(
    name: String,
    checked: Boolean? = null,
    enabled: Boolean = true,
): Modifier {
    val state = checked?.let { current -> ControlState.Checked(current) }
    return foldedName(name, FoldedRole.MenuItem, state, enabled)
}

/** [name] as the content description, with [role] and [state] folded in on the web. */
@Composable
private fun Modifier.foldedName(
    name: String,
    role: FoldedRole,
    state: ControlState?,
    enabled: Boolean,
): Modifier {
    val spoken = stateName(name, state, enabled, role = role)
    return semantics { contentDescription = spoken }
}
