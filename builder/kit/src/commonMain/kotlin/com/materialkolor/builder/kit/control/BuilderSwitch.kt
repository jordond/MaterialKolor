package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessSwitch
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentSwitch
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialSwitch

/**
 * An on and off switch with its label, in the surrounding skin.
 *
 * The label is part of the target, so a tap anywhere on the row flips it. The row has the switch
 * role and reads out "On" or "Off".
 *
 * @param[checked] Whether it is on.
 * @param[onCheckedChange] Called with the new state when someone flips it.
 * @param[label] What it turns on, shown beside it and read out as its name.
 * @param[modifier] Applied to the row.
 * @param[enabled] Whether it takes input.
 */
@Composable
public fun BuilderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialSwitch(checked, onCheckedChange, label, modifier, enabled)
        }
        Library.Unstyled -> {
            HeadlessSwitch(checked, onCheckedChange, label, UnstyledInputStyles.switch, modifier, enabled)
        }
        Library.Fluent -> {
            FluentSwitch(checked, onCheckedChange, label, modifier, enabled)
        }
        Library.Custom -> {
            HeadlessSwitch(checked, onCheckedChange, label, CustomInputStyles.switch, modifier, enabled)
        }
    }
}
