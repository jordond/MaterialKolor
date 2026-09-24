package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Names a toggle an app draws for itself, such as a heart that marks a favourite, and carries its
 * role and state onto the web the way the kit's own toggles do.
 *
 * Put it on the node that takes the toggle, after its `toggleable` with the checkbox role. Off the
 * web [name] becomes the node's content description, and the toggleable reports the role and
 * whether it is on. The web mirror drops that state and reads every clickable as a button (P3), so
 * there the checkbox word and the state travel in the name instead, as in "Favourite Flat white,
 * checkbox, checked" (D37).
 *
 * @param[name] What the toggle stands for, read out as its name.
 * @param[checked] Whether it is on.
 * @param[enabled] Whether it takes input. On the web a disabled toggle says so in its name.
 */
@Composable
public fun Modifier.foldedToggleName(
    name: String,
    checked: Boolean,
    enabled: Boolean = true,
): Modifier {
    val spoken = stateName(name, ControlState.Checked(checked), enabled, role = FoldedRole.Checkbox)
    return semantics { contentDescription = spoken }
}
