package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderToastHost
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.overlayStyle
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentDockRegion
import com.materialkolor.builder.kit.skin.fluent.FluentPaletteFrame
import com.materialkolor.builder.kit.skin.fluent.FluentPanelRegion
import com.materialkolor.builder.kit.skin.fluent.FluentTopBarRegion
import com.materialkolor.builder.kit.skin.fluent.FluentWindowRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessDockRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessPaletteFrame
import com.materialkolor.builder.kit.skin.headless.HeadlessPanelRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessTopBarRegion
import com.materialkolor.builder.kit.skin.headless.HeadlessWindowRegion
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.material.MaterialDockRegion
import com.materialkolor.builder.kit.skin.material.MaterialPaletteFrame
import com.materialkolor.builder.kit.skin.material.MaterialPanelRegion
import com.materialkolor.builder.kit.skin.material.MaterialTopBarRegion
import com.materialkolor.builder.kit.skin.material.MaterialWindowRegion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * Which edge of the workspace a panel stands on. Its other edge, the one facing the canvas, is its inner edge.
 */
public enum class PanelSide {
    /**
     * The poster's edge, where the projects drawer opens.
     */
    Start,

    /**
     * The far edge, where the export panel opens.
     */
    End,
}

/**
 * The top bar, 64 dp tall in every window class. Material3 uses its `TopAppBar`, the other skins a
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

// b-512

/**
 * The tallest a control in the [TopBarRegion] may stand. A field that floats its label over its top
 * edge, such as Material's outlined dropdown, keeps that label inside the bar at this height.
 */
public val TopBarControlMaxHeight: Dp
    get() = ShellMetrics.topBarControlHeight

/**
 * The dock, a compact toolbar centred under the preview. Material3 uses its
 * `HorizontalFloatingToolbar`, the other skins a toolbar row in their own overlay style.
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

// b-512

/**
 * The window the preview shows in on the canvas, rounded to the skin's large corners and set off
 * the canvas by the skin's own shadow or outline. It clips what it holds to its corners, so a split
 * wipes inside it. The caller sizes and places it.
 *
 * @param[modifier] Applied to the window, and where its size comes from.
 * @param[content] The preview, filling the window.
 */
@Composable
public fun PreviewWindowRegion(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val library = LocalSkin.current.library
    when (library) {
        Library.Material3 -> MaterialWindowRegion(modifier, content)
        Library.Unstyled, Library.Custom -> HeadlessWindowRegion(overlayStyle(library), modifier, content)
        Library.Fluent -> FluentWindowRegion(modifier, content)
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
 * Where the toasts of [state] land. Lay it over the whole shell, in the shell's `overlays`.
 *
 * They stack at the bottom of the canvas frame, past the poster as it is drawn, docked, as the rail
 * or opened over the canvas, and above the dock, so they cover neither. Wherever the poster is a
 * sheet they rise above its peek and the dock. The shell hands the region its room as it lays out,
 * so the toasts follow the rail as it moves. Outside a shell they stack at the bottom start, or
 * above the peek and the dock where the poster would be a sheet. Every skin shares one host, which
 * already draws each toast in the skin's own look, a `Snackbar` under Material3. Where overlays
 * render in the page the toasts keep these spots but draw over every open dialog, sheet and menu.
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
    val room = LocalShellRoom.current
    Box(modifier.fillMaxSize()) {
        if (room != null) {
            BuilderToastHost(state, Modifier.inToastRoom(room))
        } else if (layout.posterMode == PosterMode.Sheet) {
            BuilderToastHost(state, Modifier.padding(bottom = sheetClearance(layout, tokens)))
        } else {
            BuilderToastHost(
                state = state,
                modifier = Modifier.align(Alignment.BottomStart).widthIn(max = OverlayMetrics.toastMaxWidth),
            )
        }
    }
}

/**
 * The command palette's frame. A centred dialog on Medium and Expanded, up to 560 dp wide on
 * Medium and 640 dp on Expanded, and a full width one on a phone. Material3 uses its dialog
 * container, the other skins their dialog style. The modal plumbing around it, the scrim and the
 * focus trap, is the caller's.
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
    val widest = ShellMetrics.paletteWideWidth
    val sized = when (LocalLayout.current.windowClass) {
        WindowClass.Compact -> modifier.fillMaxWidth()
        WindowClass.Medium -> modifier.widthIn(min = OverlayMetrics.dialogMinWidth, max = OverlayMetrics.dialogMaxWidth)
        WindowClass.Expanded -> modifier.widthIn(min = OverlayMetrics.dialogMinWidth, max = widest)
    }
    when (library) {
        Library.Material3 -> MaterialPaletteFrame(sized, content)
        Library.Unstyled, Library.Custom -> HeadlessPaletteFrame(overlayStyle(library), sized, content)
        Library.Fluent -> FluentPaletteFrame(sized, content)
    }
}
