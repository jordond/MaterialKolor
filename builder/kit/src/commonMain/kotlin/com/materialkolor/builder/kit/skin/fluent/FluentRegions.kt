package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.max
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.shell.PanelSide
import com.materialkolor.builder.kit.shell.ShellMetrics
import com.materialkolor.builder.kit.skin.headless.innerEdgeShape
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.BackgroundSizing
import io.github.composefluent.background.ElevationDefaults
import io.github.composefluent.background.Layer
import io.github.composefluent.background.elevation
import io.github.composefluent.component.CommandBar
import io.github.composefluent.component.CommandBarDefaults
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.Text

/**
 * Fluent's header, a `CommandBar` across the top of the workspace on the workspace ground.
 *
 * The bar takes the header's full height, and its contents go in as one item as wide as the bar,
 * so a weighted child still takes the room that is left. The library switcher inside is the kit's
 * Fluent segmented control, which draws its own frame so the focus ring is never clipped.
 */
@Composable
internal fun FluentTopBarRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellMetrics.topBarHeight)
            .background(tokens.panel)
            .padding(horizontal = tokens.spacing.small),
    ) {
        FluentCommands(Modifier.fillMaxSize(), fill = true, content = content)
    }
}

/**
 * Fluent's dock, a `CommandBar` on a `Layer` in the flyout's dress, floating on the flyout's
 * shadow. It is as tall as Fluent's standard bar, or taller where the touch targets need it.
 */
@Composable
internal fun FluentDockRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val colors = FluentTheme.colors
    val height = max(
        CommandBarDefaults.CommandBarStandardHeight,
        LocalLayout.current.primaryTouchTarget + tokens.spacing.small,
    )
    Layer(
        modifier = modifier,
        shape = FluentTheme.shapes.overlay,
        color = colors.background.acrylic.default,
        border = BorderStroke(tokens.outlineWidth, colors.stroke.surface.flyout),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
        elevation = ElevationDefaults.flyout,
    ) {
        FluentCommands(Modifier.height(height), fill = false, content = content)
    }
}

/**
 * A full height panel on a `Layer` in the flyout's dress, rounded on its inner edge by Fluent's
 * overlay radius and clipped to it.
 */
@Composable
internal fun FluentPanelRegion(
    side: PanelSide,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val colors = FluentTheme.colors
    Layer(
        modifier = modifier.fillMaxHeight(),
        shape = innerEdgeShape(side, FluentTheme.cornerRadius.overlay),
        color = colors.background.acrylic.default,
        border = BorderStroke(tokens.outlineWidth, colors.stroke.surface.flyout),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
        clipContent = true,
        elevation = ElevationDefaults.flyout,
    ) {
        Column(Modifier.fillMaxHeight(), content = content)
    }
}

/** The command palette on a `Layer` in the flyout's dress, raised to a dialog's shadow. */
@Composable
internal fun FluentPaletteFrame(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val colors = FluentTheme.colors
    Layer(
        modifier = modifier,
        shape = FluentTheme.shapes.overlay,
        color = colors.background.acrylic.default,
        border = BorderStroke(tokens.outlineWidth, colors.stroke.surface.flyout),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
        clipContent = true,
        elevation = ElevationDefaults.dialog,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            content = content,
        )
    }
}

/**
 * One toast as Fluent's informational `InfoBar`, raised off the page on a tooltip's shadow.
 *
 * The host around it keeps the timing, the live region and the announcer. The bar stands on the
 * ground Fluent's own sits on, laid solid so nothing under the toast shows through, and its badge
 * stays out of what is read, since the message says all there is. The one action is the kit's
 * button, so it keeps the skin's focus ring and its folded name.
 */
@Composable
internal fun FluentToast(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier,
) {
    val colors = FluentTheme.colors
    val shape = FluentTheme.shapes.control
    InfoBar(
        title = {},
        message = { Text(message) },
        severity = InfoBarSeverity.Informational,
        modifier = modifier
            .fillMaxWidth()
            .elevation(ElevationDefaults.tooltip, shape),
        colors = InfoBarDefaults.informationalColors(
            backgroundColor = colors.background.card.secondary
                .compositeOver(colors.background.solid.base),
        ),
        icon = { InfoBarDefaults.Badge(severity = InfoBarSeverity.Informational, contentDescription = null) },
        action = actionLabel?.let { label ->
            { BuilderButton(onClick = onAction, label = label) }
        },
    )
}

/**
 * A `CommandBar` holding [content] as its one item, so the bar never has anything to move into
 * its overflow. The overflow is a popup, which the builder never opens (D40), so the bar is also
 * held closed. With [fill] the row takes the bar's whole width, and otherwise it wraps its tools.
 */
@Composable
private fun FluentCommands(
    modifier: Modifier,
    fill: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val row by rememberUpdatedState(content)
    CommandBar(
        expanded = false,
        onExpandedChanged = {},
        modifier = modifier,
    ) {
        item(key = CommandRowKey) {
            Row(
                modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
                horizontalArrangement = Arrangement.spacedBy(if (fill) spacing.small else spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row()
            }
        }
    }
}

/** The key of the one item every Fluent command bar here holds. */
private const val CommandRowKey = "commands"
