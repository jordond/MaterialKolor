package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.shell.PanelSide
import com.materialkolor.builder.kit.skin.headless.HeadlessDockRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessPaletteFrame
import com.materialkolor.builder.kit.skin.headless.HeadlessPanelRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessTopBarRegion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

// fluent-placeholder

/*
 * The Fluent regions, for now the headless ones in the placeholder's overlay dress. B-404 swaps in a
 * header with a `CommandBar`, a `CommandBar` on a `Layer` as the dock, and Fluent layer styling on
 * the panel and the palette.
 */

@Composable
internal fun FluentTopBarRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    HeadlessTopBarRegion(modifier, content)
}

@Composable
internal fun FluentDockRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    HeadlessDockRegion(fluentOverlayStyle(LocalBuilderTokens.current), modifier, content)
}

@Composable
internal fun FluentPanelRegion(
    side: PanelSide,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessPanelRegion(fluentOverlayStyle(LocalBuilderTokens.current), side, modifier, content)
}

@Composable
internal fun FluentPaletteFrame(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessPaletteFrame(fluentOverlayStyle(LocalBuilderTokens.current), modifier, content)
}
