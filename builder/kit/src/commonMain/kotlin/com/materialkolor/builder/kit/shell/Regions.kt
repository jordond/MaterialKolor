package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderToastHost
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.overlayStyle
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentDockRegion
import com.materialkolor.builder.kit.skin.fluent.FluentPaletteFrame
import com.materialkolor.builder.kit.skin.fluent.FluentPanelRegion
import com.materialkolor.builder.kit.skin.fluent.FluentTopBarRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessDockRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessPaletteFrame
import com.materialkolor.builder.kit.skin.headless.HeadlessPanelRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessTopBarRegion
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.material.MaterialDockRegion
import com.materialkolor.builder.kit.skin.material.MaterialPaletteFrame
import com.materialkolor.builder.kit.skin.material.MaterialPanelRegion
import com.materialkolor.builder.kit.skin.material.MaterialTopBarRegion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/** Which edge of the workspace a panel stands on. Its other edge, the one facing the canvas, is its inner edge. */
public enum class PanelSide {
    /** The poster's edge, where the projects drawer opens. */
    Start,

    /** The far edge, where the export panel opens. */
    End,
}

/**
 * The top bar, 64 dp tall in every window class. Material3 wears its `TopAppBar`, the other skins a
 * plain header row on the workspace ground.
 *
 * @param[modifier] Applied to the bar.
 * @param[content] The bar's contents, laid in a row from the start edge.
 */
@Composable
public fun TopBarRegion(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialTopBarRegion(modifier, content)
        Library.Unstyled, Library.Custom -> HeadlessTopBarRegion(modifier, content)
        Library.Fluent -> FluentTopBarRegion(modifier, content)
    }
}

/**
 * The floating dock over the canvas. Material3 wears its `HorizontalFloatingToolbar`, the other
 * skins a toolbar row in their own overlay dress.
 *
 * @param[modifier] Applied to the dock.
 * @param[content] The dock's tools, laid in a row.
 */
@Composable
public fun DockRegion(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val library = LocalSkin.current.library
    when (library) {
        Library.Material3 -> MaterialDockRegion(modifier, content)
        Library.Unstyled, Library.Custom -> HeadlessDockRegion(overlayStyle(library), modifier, content)
        Library.Fluent -> FluentDockRegion(modifier, content)
    }
}

/**
 * A full height panel standing on [side], such as the export panel or the projects drawer. Its
 * inner edge takes the skin's panel rounding. The caller sizes it.
 *
 * @param[side] The edge the panel stands on.
 * @param[modifier] Applied to the panel, and where its width comes from.
 * @param[content] The panel's contents, laid in a column.
 */
@Composable
public fun PanelRegion(
    side: PanelSide,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val library = LocalSkin.current.library
    when (library) {
        Library.Material3 -> MaterialPanelRegion(side, modifier, content)
        Library.Unstyled, Library.Custom -> HeadlessPanelRegion(overlayStyle(library), side, modifier, content)
        Library.Fluent -> FluentPanelRegion(side, modifier, content)
    }
}

/**
 * Where the toasts of [state] land. Lay it over the whole shell.
 *
 * They stack at the bottom start on Medium and Expanded, and on a phone they rise above the poster
 * peek and the dock. Every skin shares one host, which already draws each toast in the skin's own
 * dress, a `Snackbar` under Material3.
 *
 * @param[state] The toasts to show.
 * @param[modifier] Applied to the region, which fills the space it is given without taking any
 * pointer input.
 */
@Composable
public fun ToastRegion(
    state: BuilderToastHostState,
    modifier: Modifier = Modifier,
) {
    val layout = LocalLayout.current
    val tokens = LocalBuilderTokens.current
    Box(modifier.fillMaxSize()) {
        if (layout.windowClass == WindowClass.Compact) {
            val lift = posterPeekHeight(layout) + tokens.spacing.large + ShellMetrics.dockHeight
            BuilderToastHost(state, Modifier.padding(bottom = lift))
        } else {
            BuilderToastHost(
                state = state,
                modifier = Modifier.align(Alignment.BottomStart).widthIn(max = OverlayMetrics.toastMaxWidth),
            )
        }
    }
}

/**
 * The command palette's frame. A centred dialog on Medium and Expanded, a full width one on a
 * phone. Material3 wears its dialog container, the other skins their dialog dress. The modal
 * plumbing around it, the scrim and the focus trap, is the caller's.
 *
 * @param[modifier] Applied to the frame.
 * @param[content] The query field and the results, laid in a column.
 */
@Composable
public fun PaletteFrame(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val library = LocalSkin.current.library
    val sized = if (LocalLayout.current.windowClass == WindowClass.Compact) {
        modifier.fillMaxWidth()
    } else {
        modifier.widthIn(min = OverlayMetrics.dialogMinWidth, max = OverlayMetrics.dialogMaxWidth)
    }
    when (library) {
        Library.Material3 -> MaterialPaletteFrame(sized, content)
        Library.Unstyled, Library.Custom -> HeadlessPaletteFrame(overlayStyle(library), sized, content)
        Library.Fluent -> FluentPaletteFrame(sized, content)
    }
}
