package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.headless.HeadlessCheckbox
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialCheckbox

/**
 * A checkbox with its label, in the surrounding skin.
 *
 * The label is part of the target. The row has the checkbox role and reads out "Checked" or
 * "Not checked".
 *
 * @param[checked] Whether it is ticked.
 * @param[onCheckedChange] Called with the new state when someone ticks or clears it.
 * @param[label] What it stands for, shown beside it and read out as its name.
 * @param[modifier] Applied to the row.
 * @param[enabled] Whether it takes input.
 */
@Composable
public fun BuilderCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> {
            MaterialCheckbox(checked, onCheckedChange, label, modifier, enabled)
        }
        SkinLibrary.Custom -> {
            HeadlessCheckbox(checked, onCheckedChange, label, CustomInputStyles.checkbox, modifier, enabled)
        }
    }
}
