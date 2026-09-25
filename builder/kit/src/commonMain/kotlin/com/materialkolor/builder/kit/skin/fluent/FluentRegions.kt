package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import io.github.composefluent.component.CommandBarDefaults
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.Text

/**
 * Fluent's header, a command bar across the top of the workspace on the workspace ground.
 *
 * It is the bar's own frame, a clear `Layer` with the bar's padding and Fluent's ink, rather than
 * Fluent's `CommandBar`. That one keeps its overflow button composed off screen even when nothing
 * overflows, a nameless button that a screen reader and the page's mirror still find, and the
 * overflow opens as a popup (D40). The library switcher inside is the kit's Fluent segmented
 * control, which draws its own frame so its focus ring is never clipped.
 */
@Composable
internal fun FluentTopBarRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Layer(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellMetrics.topBarHeight),
        shape = RectangleShape,
        color = tokens.panel,
        border = null,
    ) {
        FluentCommands(
            // b-512
            // The bar's own inset alone on the start edge, so the switcher stands next to the poster.
            modifier = Modifier.fillMaxSize().padding(end = tokens.spacing.small),
            gap = tokens.spacing.extraSmall,
            content = content,
        )
    }
}

// b-512

/**
 * The preview window on the canvas, a `Layer` in the card's dress with Fluent's overlay corners,
 * clipping what it holds to them.
 */
@Composable
internal fun FluentWindowRegion(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val colors = FluentTheme.colors
    Layer(
        modifier = modifier,
        shape = FluentTheme.shapes.overlay,
        color = colors.background.layer.default,
        border = BorderStroke(tokens.outlineWidth, colors.stroke.card.default),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
        clipContent = true,
    ) {
        Box(content = content)
    }
}

/**
 * Fluent's dock, a command bar on a `Layer` in the flyout's dress, floating on the flyout's
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
        FluentCommands(Modifier.height(height), gap = tokens.spacing.extraSmall, content = content)
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

/**
 * The command palette on a `Layer` in the flyout's dress, raised to a dialog's shadow.
 */
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
 * A row laid out the way a `CommandBar` lays out its commands, centred in the bar's height behind
 * the bar's side padding, [gap] apart. Nothing in it ever moves to an overflow, so a narrow window
 * squeezes the tools rather than hiding them.
 */
@Composable
private fun FluentCommands(
    modifier: Modifier,
    gap: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.padding(horizontal = CommandBarPadding),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * How far a `CommandBar` insets its commands from its ends.
 */
private val CommandBarPadding = 8.dp
