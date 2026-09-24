package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessMenuPanel
import com.materialkolor.builder.kit.headless.HeadlessSelectPanel
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.headless.unstyledOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialMenuPanel
import com.materialkolor.builder.kit.skin.material.MaterialSelectPanel
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A [BuilderMenu] as it looks open, drawn where it stands, for a gallery that shows the menu
 * without opening it.
 *
 * It has the rows and the container of the skin's open menu and none of what makes the menu float.
 * There is no popup, no overlay host, no focus trap and nothing that closes it. Tab walks through
 * the rows and on past them like any other controls in the page, and choosing a row runs its item.
 *
 * @param[items] The commands, top to bottom.
 * @param[modifier] Applied to the panel.
 */
@Composable
public fun BuilderMenuPanel(
    items: List<BuilderMenuItem>,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialMenuPanel(items, modifier)
        Library.Unstyled -> HeadlessMenuPanel(items, unstyledOverlayStyle(tokens), modifier)
        // fluent-placeholder
        Library.Fluent -> HeadlessMenuPanel(items, fluentOverlayStyle(tokens), modifier)
        Library.Custom -> HeadlessMenuPanel(items, customOverlayStyle(tokens), modifier)
    }
}

/**
 * A [BuilderSelect] as it looks open, its field over its list of options, drawn where it stands for
 * a gallery that shows the select without opening it.
 *
 * It has the field, the rows and the container of the skin's open select and none of what makes
 * the list float. There is no popup, no overlay host, no focus trap and nothing that closes it. The
 * field only shows the choice, so Tab walks through the options and on past them, and picking one
 * calls [onSelect].
 *
 * @param[label] What is being chosen.
 * @param[options] Every choice, in the order the list shows them.
 * @param[selected] The current choice, which has to be one of [options].
 * @param[onSelect] Called with the option someone picks.
 * @param[modifier] Applied to the panel.
 * @param[optionLabel] How an option reads in the field and in the list.
 */
@Composable
public fun <T> BuilderSelectPanel(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { option -> option.toString() },
) {
    require(selected in options) { "The selected option $selected is not one of the options" }
    val tokens = LocalBuilderTokens.current
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialSelectPanel(label, options, selected, onSelect, optionLabel, modifier)
        Library.Unstyled -> HeadlessSelectPanel(
            label,
            options,
            selected,
            onSelect,
            optionLabel,
            unstyledOverlayStyle(tokens),
            modifier,
        )
        // fluent-placeholder
        Library.Fluent -> HeadlessSelectPanel(
            label,
            options,
            selected,
            onSelect,
            optionLabel,
            fluentOverlayStyle(tokens),
            modifier,
        )
        Library.Custom -> HeadlessSelectPanel(
            label,
            options,
            selected,
            onSelect,
            optionLabel,
            customOverlayStyle(tokens),
            modifier,
        )
    }
}
