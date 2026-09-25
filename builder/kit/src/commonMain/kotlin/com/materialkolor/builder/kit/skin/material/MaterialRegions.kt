package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.shell.PanelSide
import com.materialkolor.builder.kit.shell.ShellMetrics
import com.materialkolor.builder.kit.skin.headless.innerEdgeShape
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * Material's `TopAppBar` on the workspace ground. The contents take the title slot and all of its
 * width, and the bar leaves the window insets to the shell around it. The row reaches back over the
 * bar's title inset, so the first control stands just off the bar's start edge.
 */
@Composable
internal fun MaterialTopBarRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.pullStart(TitleInset - tokens.spacing.extraSmall).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        },
        modifier = modifier,
        expandedHeight = ShellMetrics.topBarHeight,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = tokens.panel),
    )
}

/**
 * Material's `HorizontalFloatingToolbar`, always expanded, in its standard colours on the lightest
 * surface container, since its own container is the canvas's colour and would vanish on it.
 */
@Composable
internal fun MaterialDockRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        content = content,
    )
}

/**
 * How far in from its start edge a `TopAppBar` with no navigation icon starts its title.
 */
private val TitleInset: Dp = 16.dp

/**
 * Lays the content out [pull] wider than it is given and places it [pull] toward the start.
 */
private fun Modifier.pullStart(pull: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = pull.roundToPx().coerceAtLeast(0)
        val wider = if (constraints.hasBoundedWidth) {
            constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
        } else {
            constraints
        }
        val placeable = measurable.measure(wider)
        val width = (placeable.width - extra).coerceAtLeast(0)
        layout(width, placeable.height) { placeable.placeRelative(-extra, 0) }
    }

/**
 * The preview window on the canvas, a Material surface rounded to the builder's large radius and
 * lifted on the same shadow as Material's overlays. It clips what it holds to its corners.
 */
@Composable
internal fun MaterialWindowRegion(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = materialOverlayStyle()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalBuilderTokens.current.radius.large),
        color = style.surface,
        shadowElevation = style.shadow,
    ) {
        Box(content = content)
    }
}

/**
 * A Material surface in the side panel's style, rounded on its inner edge like Material's side sheet.
 */
@Composable
internal fun MaterialPanelRegion(
    side: PanelSide,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = materialOverlayStyle()
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = innerEdgeShape(side, style.panelRadius),
        color = style.surface,
        shadowElevation = style.shadow,
    ) {
        Column(content = content)
    }
}

/**
 * Material's dialog container, its shape, colour and tonal elevation.
 */
@Composable
internal fun MaterialPaletteFrame(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Surface(
        modifier = modifier,
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            content = content,
        )
    }
}
