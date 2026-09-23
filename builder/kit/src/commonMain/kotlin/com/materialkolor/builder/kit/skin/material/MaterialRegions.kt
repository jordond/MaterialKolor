package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.shell.PanelSide
import com.materialkolor.builder.kit.shell.ShellMetrics
import com.materialkolor.builder.kit.skin.headless.innerEdgeShape
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * Material's `TopAppBar` on the workspace ground. The contents take the title slot and all of its
 * width, and the bar leaves the window insets to the shell around it.
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
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

/** Material's `HorizontalFloatingToolbar`, always expanded, in its standard colours. */
@Composable
internal fun MaterialDockRegion(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    HorizontalFloatingToolbar(expanded = true, modifier = modifier, content = content)
}

/** A Material surface in the side panel's dress, rounded on its inner edge like Material's side sheet. */
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

/** Material's dialog container, its shape, colour and tonal elevation. */
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
