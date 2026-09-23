package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessTabs
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentTabs
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialTabs

/**
 * A row of tabs in the surrounding skin, such as the canvas tabs App, Components, Roles, Palettes
 * and Contrast.
 *
 * Each tab has the tab role and its selected state. Focus roves. Tab lands on the selected tab
 * only, and the arrow keys move along the row in reading order, wrapping at the ends and selecting
 * as they go. When the tabs do not fit, as on a Compact canvas, the row scrolls sideways and keeps
 * the focused tab in view.
 *
 * @param[tabs] The tabs, in order. Each has to be distinct.
 * @param[selected] The selected tab, one of [tabs].
 * @param[onSelect] Called with a tab when someone picks it.
 * @param[label] The text on each tab, read out as its name.
 * @param[modifier] Applied to the row.
 */
@Composable
public fun <T> BuilderTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    require(selected in tabs) { "The selected tab $selected is not one of $tabs" }
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialTabs(tabs, selected, onSelect, label, modifier)
        Library.Unstyled -> HeadlessTabs(tabs, selected, onSelect, label, UnstyledInputStyles.tabs, modifier)
        Library.Fluent -> FluentTabs(tabs, selected, onSelect, label, modifier)
        Library.Custom -> HeadlessTabs(tabs, selected, onSelect, label, CustomInputStyles.tabs, modifier)
    }
}
