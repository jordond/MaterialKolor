package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.headless.HeadlessSwitch
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialSwitch

/**
 * An on and off switch with its label, in the surrounding skin.
 *
 * The label is part of the target, so a tap anywhere on the row flips it. The row has the switch
 * role and reads out "On" or "Off". The switch sits at the row's end, so a row given the full width
 * puts the label at the start and the switch at the end.
 *
 * @param[checked] Whether it is on.
 * @param[onCheckedChange] Called with the new state when someone flips it.
 * @param[label] What it turns on, shown beside it and read out as its name.
 * @param[modifier] Applied to the row.
 * @param[enabled] Whether it takes input.
 * @param[caption] A quieter line under the label, such as what turning it on does, or null for none.
 * It is part of the target too.
 */
@Composable
public fun BuilderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    caption: String? = null,
) {
    when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> {
            MaterialSwitch(checked, onCheckedChange, label, modifier, enabled, caption)
        }
        SkinLibrary.Custom -> {
            HeadlessSwitch(checked, onCheckedChange, label, CustomInputStyles.switch, modifier, enabled, caption)
        }
    }
}

/**
 * A switch's label, with its [caption] under it when there is one.
 */
@Composable
internal fun SwitchLabel(
    label: String,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        BuilderText(text = label, style = BuilderTextStyle.Label)
        if (caption != null) BuilderText(text = caption, style = BuilderTextStyle.Label, emphasis = Emphasis.Secondary)
    }
}
