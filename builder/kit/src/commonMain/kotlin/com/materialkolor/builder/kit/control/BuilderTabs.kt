package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessTabs
import com.materialkolor.builder.kit.headless.TabsStyle
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentTabs
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialTabs
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.composefluent.FluentTheme

// b-512

/**
 * How a row of [BuilderTabs] looks.
 */
public enum class TabsVariant {
    /**
     * The skin's own tab row, such as Material's underlined tabs. The export sheet's file tabs use it.
     */
    Standard,

    /**
     * The canvas tabs of design D. Pills in every skin but Unstyled, which keeps its underline, each
     * drawn in the skin's colours and shapes, and starting at the row's own start edge.
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
    variant: TabsVariant = TabsVariant.Standard, // b-512
) {
    require(selected in tabs) { "The selected tab $selected is not one of $tabs" }
    val skin = LocalSkin.current
    if (variant == TabsVariant.Canvas) {
        // b-512
        val style = when (skin.library) {
            Library.Material3 -> if (skin.expressive) expressiveCanvasTabs() else materialCanvasTabs()
            Library.Unstyled -> unstyledCanvasTabs()
            Library.Fluent -> fluentCanvasTabs()
            Library.Custom -> customCanvasTabs()
        }
        HeadlessTabs(tabs, selected, onSelect, label, style, modifier)
        return
    }
    when (skin.library) {
        Library.Material3 -> MaterialTabs(tabs, selected, onSelect, label, modifier)
        Library.Unstyled -> HeadlessTabs(tabs, selected, onSelect, label, UnstyledInputStyles.tabs, modifier)
        Library.Fluent -> FluentTabs(tabs, selected, onSelect, label, modifier)
        Library.Custom -> HeadlessTabs(tabs, selected, onSelect, label, CustomInputStyles.tabs, modifier)
    }
}

// b-512

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
 * Unstyled's canvas tabs, its plain underline, the first label close to the row's start edge.
 */
@Composable
@ReadOnlyComposable
private fun unstyledCanvasTabs(): TabsStyle {
    val tokens = LocalBuilderTokens.current
    return TabsStyle(
        container = Color.Transparent,
        containerShape = RoundedCornerShape(0.dp),
        containerPadding = 0.dp,
        tabShape = RoundedCornerShape(0.dp),
        tabPadding = PaddingValues(horizontal = tokens.spacing.extraSmall, vertical = tokens.spacing.small),
        gap = tokens.spacing.large,
        selectedContainer = Color.Transparent,
        selectedInk = tokens.textStrong,
        ink = tokens.textMuted,
        indicator = tokens.accent,
        indicatorHeight = 2.dp,
        indicatorWidth = null,
        focus = tokens.focus,
    )
}

/**
 * Fluent's canvas tabs, a subtle well holding the tabs with the chosen one on the layer fill, in Fluent's control corners.
 */
@Composable
private fun fluentCanvasTabs(): TabsStyle {
    val tokens = LocalBuilderTokens.current
    val shape = FluentTheme.shapes.control
    return TabsStyle(
        container = tokens.panelRaised,
        containerShape = shape,
        containerPadding = tokens.spacing.extraSmall / 2,
        tabShape = shape,
        tabPadding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.extraSmall),
        gap = tokens.spacing.extraSmall / 2,
        selectedContainer = tokens.panel,
        selectedInk = tokens.textStrong,
        ink = tokens.textMuted,
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
