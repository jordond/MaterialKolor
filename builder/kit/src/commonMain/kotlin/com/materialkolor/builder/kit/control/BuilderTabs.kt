package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.HeadlessTabs
import com.materialkolor.builder.kit.headless.TabsStyle
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialTabs
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * How a row of [BuilderTabs] looks.
 */
public enum class TabsVariant {
    /**
     * The skin's own tab row, such as Material's underlined tabs. The export sheet's file tabs use it.
     */
    Standard,

    /**
     * The canvas tabs. Pills in every skin, each drawn in the skin's colours and shapes, and
     * starting at the row's own start edge.
     */
    Canvas,
}

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
 * @param[variant] How the row looks, the skin's own tabs or the canvas pills.
 */
@Composable
public fun <T> BuilderTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    variant: TabsVariant = TabsVariant.Standard,
) {
    require(selected in tabs) { "The selected tab $selected is not one of $tabs" }
    val skin = LocalSkin.current
    if (variant == TabsVariant.Canvas) {
        val style = when (skin.library) {
            SkinLibrary.Material3 -> if (skin.expressive) expressiveCanvasTabs() else materialCanvasTabs()
            SkinLibrary.Custom -> customCanvasTabs()
        }
        HeadlessTabs(tabs, selected, onSelect, label, style, modifier)
        return
    }
    when (skin.library) {
        SkinLibrary.Material3 -> MaterialTabs(tabs, selected, onSelect, label, modifier)
        SkinLibrary.Custom -> HeadlessTabs(tabs, selected, onSelect, label, CustomInputStyles.tabs, modifier)
    }
}

/**
 * Material's canvas tabs, a tonal pill holding the tabs with the chosen one lifted to the lightest surface.
 */
@Composable
@ReadOnlyComposable
private fun materialCanvasTabs(): TabsStyle {
    val tokens = LocalBuilderTokens.current
    val colors = MaterialTheme.colorScheme
    return TabsStyle(
        container = colors.surfaceContainerHigh,
        containerShape = CircleShape,
        containerPadding = tokens.spacing.extraSmall,
        tabShape = CircleShape,
        tabPadding = PaddingValues(horizontal = tokens.spacing.large, vertical = tokens.spacing.extraSmall),
        gap = tokens.spacing.extraSmall,
        selectedContainer = colors.surfaceContainerLowest,
        selectedInk = colors.onSurface,
        ink = colors.onSurfaceVariant,
        indicator = Color.Transparent,
        indicatorHeight = 0.dp,
        indicatorWidth = null,
        focus = tokens.focus,
    )
}

/**
 * Expressive's canvas tabs, a pill per tab with the chosen one filled in the primary container and
 * squared off to Material's medium shape.
 */
@Composable
@ReadOnlyComposable
private fun expressiveCanvasTabs(): TabsStyle {
    val tokens = LocalBuilderTokens.current
    val colors = MaterialTheme.colorScheme
    return TabsStyle(
        container = Color.Transparent,
        containerShape = CircleShape,
        containerPadding = 0.dp,
        tabShape = MaterialTheme.shapes.medium,
        tabPadding = PaddingValues(horizontal = tokens.spacing.large, vertical = tokens.spacing.small),
        gap = tokens.spacing.extraSmall,
        selectedContainer = colors.primaryContainer,
        selectedInk = colors.onPrimaryContainer,
        ink = colors.onSurfaceVariant,
        indicator = Color.Transparent,
        indicatorHeight = 0.dp,
        indicatorWidth = null,
        focus = tokens.focus,
    )
}

/**
 * Custom's canvas tabs, bare labels with the chosen one in a pill of the strongest ink.
 */
@Composable
@ReadOnlyComposable
private fun customCanvasTabs(): TabsStyle {
    val tokens = LocalBuilderTokens.current
    return TabsStyle(
        container = Color.Transparent,
        containerShape = CircleShape,
        containerPadding = 0.dp,
        tabShape = CircleShape,
        tabPadding = PaddingValues(horizontal = tokens.spacing.large, vertical = tokens.spacing.small),
        gap = tokens.spacing.extraSmall,
        selectedContainer = tokens.textStrong,
        selectedInk = tokens.canvas,
        ink = tokens.textStrong,
        indicator = Color.Transparent,
        indicatorHeight = 0.dp,
        indicatorWidth = null,
        focus = tokens.focus,
    )
}
