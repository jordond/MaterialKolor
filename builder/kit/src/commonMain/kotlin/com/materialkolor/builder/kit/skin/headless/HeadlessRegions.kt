package com.materialkolor.builder.kit.skin.headless

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.shell.PanelSide
import com.materialkolor.builder.kit.shell.ShellMetrics
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The header row Custom puts at the top of the workspace, on the workspace ground. Its first control
 * stands just off the start edge, next to the poster.
 */
@Composable
internal fun HeadlessTopBarRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellMetrics.topBarHeight)
            .background(tokens.panel)
            .padding(start = tokens.spacing.extraSmall, end = tokens.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * A toolbar row in the skin's popover style, floating on its own shadow where the skin has one.
 */
@Composable
internal fun HeadlessDockRegion(
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = modifier
            .regionFrame(style, style.popoverShape)
            .padding(tokens.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * The preview window on the canvas in the skin's dialog style, its corners, its shadow where the
 * skin has one and its outline drawn over the edge of what it holds, which it clips to its corners.
 */
@Composable
internal fun HeadlessWindowRegion(
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.regionFrame(style, style.dialogShape), content = content)
}

/**
 * A full height panel in the skin's panel style, rounded on its inner edge.
 */
@Composable
internal fun HeadlessPanelRegion(
    style: OverlayStyle,
    side: PanelSide,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .regionFrame(style, innerEdgeShape(side, style.panelRadius)),
        content = content,
    )
}

/**
 * The command palette in the skin's dialog style.
 */
@Composable
internal fun HeadlessPaletteFrame(
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = modifier
            .regionFrame(style, style.dialogShape)
            .padding(tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        content = content,
    )
}

/**
 * A shape rounded by [radius] only on the edge that faces the canvas, the end edge of a panel on
 * the start side and the other way round.
 */
internal fun innerEdgeShape(
    side: PanelSide,
    radius: Dp,
): Shape =
    when (side) {
        PanelSide.Start -> RoundedCornerShape(topEnd = radius, bottomEnd = radius)
        PanelSide.End -> RoundedCornerShape(topStart = radius, bottomStart = radius)
    }

/**
 * Draws a region's shadow, ground and outline from the overlay style.
 */
private fun Modifier.regionFrame(
    style: OverlayStyle,
    shape: Shape,
): Modifier =
    shadow(style.shadow, shape)
        .clip(shape)
        .background(style.surface)
        .outline(style.border, shape)

private fun Modifier.outline(
    border: BorderStroke?,
    shape: Shape,
): Modifier = if (border != null) border(border, shape) else this
