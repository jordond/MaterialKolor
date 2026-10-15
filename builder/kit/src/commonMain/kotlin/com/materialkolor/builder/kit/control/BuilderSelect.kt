package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.headless.HeadlessSelect
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialSelect
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A field showing one choice out of [options], opening a list of all of them.
 *
 * The field reads as a dropdown list whose state is the chosen option, and each option reads as a
 * radio button that knows whether it is selected. On the web the field reads as a button named
 * "Style, pop-up button, Tonal spot" and each option as "Vibrant, option, not selected". The
 * chosen option also carries a check, so the choice never rests on colour alone. The field fills
 * the width it is given. Material3 draws its exposed dropdown, Custom the headless select.
 *
 * @param[label] What is being chosen.
 * @param[options] Every choice, in the order the list shows them.
 * @param[selected] The current choice, which has to be one of [options].
 * @param[onSelect] Called with the option someone picks.
 * @param[modifier] Applied to the field.
 * @param[enabled] False to show the choice without letting anyone change it.
 * @param[optionLabel] How an option reads in the field and in the list.
 */
@Composable
public fun <T> BuilderSelect(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { option -> option.toString() },
) {
    require(selected in options) { "The selected option $selected is not one of the options" }
    val tokens = LocalBuilderTokens.current
    when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> MaterialSelect(label, options, selected, onSelect, optionLabel, enabled, modifier)
        SkinLibrary.Custom -> HeadlessSelect(
            label,
            options,
            selected,
            onSelect,
            optionLabel,
            enabled,
            customOverlayStyle(tokens),
            modifier,
        )
    }
}
