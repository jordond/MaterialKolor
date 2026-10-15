package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.control.BuilderToastHost
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.material.MaterialDockRegion
import com.materialkolor.builder.kit.skin.material.MaterialTopBarRegion
import com.materialkolor.builder.kit.skin.material.MaterialWindowRegion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The top bar, 64 dp tall in every window class, drawn as Material's `TopAppBar`.
 *
 * @param[modifier] Applied to the bar.
 * @param[content] The bar's contents, laid in a row from the start edge.
 */
@Composable
public fun TopBarRegion(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialTopBarRegion(modifier, content)
}

/**
 * The tallest a control in the [TopBarRegion] may stand. A field that floats its label over its top
 * edge, such as Material's outlined dropdown, keeps that label inside the bar at this height.
 */
public val TopBarControlMaxHeight: Dp
    get() = ShellMetrics.topBarControlHeight

/**
 * The dock, a compact toolbar centred under the preview, drawn as Material's
 * `HorizontalFloatingToolbar`.
 *
 * @param[modifier] Applied to the dock.
 * @param[content] The dock's tools, laid in a row.
 */
@Composable
public fun DockRegion(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialDockRegion(modifier, content)
}

/**
 * The window the preview shows in on the canvas, rounded to the builder's large corners and set off
 * the canvas by the shadow of Material's overlays. It clips what it holds to its corners, so a split
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
    MaterialWindowRegion(modifier, content)
}

/**
 * Where the toasts of [state] land. Lay it over the whole shell, in the shell's `overlays`.
 *
 * They stack at the bottom of the canvas frame, past the poster as it is drawn, docked, as the rail
 * or opened over the canvas, and above the dock, so they cover neither. Wherever the poster is a
 * sheet they rise above its peek and the dock. The shell hands the region its room as it lays out,
 * so the toasts follow the rail as it moves. Outside a shell they stack at the bottom start, or
 * above the peek and the dock where the poster would be a sheet. The host draws each toast as a
 * Material `Snackbar`. Where overlays
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
