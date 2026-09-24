package com.materialkolor.builder.kit.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.BottomSheetState
import com.materialkolor.builder.kit.control.rememberBottomSheetState
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.shell_poster_label
import com.materialkolor.builder.kit.headless.HeadlessBottomSheet
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.layout.ShortHeightBreakpoint
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * The sizes the shell gives its regions in every skin, design D's geometry.
 *
 * Like the overlay metrics they are the same in every skin, so none of them is a builder token. The
 * margins and the corner radii come from the tokens.
 */
internal object ShellMetrics {
    /** The poster docked at Expanded. */
    val posterWideWidth: Dp = 400.dp

    /** The poster docked at Medium from 840 dp, and opened over the canvas below that. */
    val posterNarrowWidth: Dp = 320.dp

    /** The seed strip the poster collapses to. */
    val railWidth: Dp = 72.dp

    /** The top bar, in every window class. */
    val topBarHeight: Dp = 64.dp

    /** How much of the poster sheet shows at peek on a phone held upright. */
    val posterPeekHeight: Dp = 344.dp

    /** The room a floating dock takes, Material's floating toolbar height. */
    val dockHeight: Dp = 64.dp

    /** The command palette's widest on Expanded, where the other dialogs stop at 560 dp. */
    val paletteWideWidth: Dp = 640.dp
}

/**
 * How much of the poster sheet shows at peek. A phone in landscape shrinks it to the kit's plain
 * sheet peek so the canvas keeps some room.
 */
internal fun posterPeekHeight(layout: LayoutInfo): Dp =
    if (layout.heightDp < ShortHeightBreakpoint) OverlayMetrics.sheetPeekHeight else ShellMetrics.posterPeekHeight

/** How far above the bottom edge the poster peek and the floating dock reach in the sheet layout. */
internal fun sheetClearance(
    layout: LayoutInfo,
    tokens: BuilderTokens,
): Dp = posterPeekHeight(layout) + tokens.spacing.large + ShellMetrics.dockHeight

/**
 * The workspace laid out for the space it is in, the poster, the top bar, the canvas and the dock.
 *
 * The geometry comes from `LocalLayout` and is the same in every skin (design D). On Expanded the
 * poster docks at 400 dp and on Medium at 320 dp from 840 dp, and both collapse to the 72 dp rail.
 * Below 840 dp it is the rail, which opens over the canvas. On a phone, upright or on its side, it
 * lives in a bottom sheet with the dock floating above its peek. The poster keeps its seed coloured
 * look in every skin, since the shell draws it inside [PosterSurface]. The canvas is a rounded frame
 * whose content stops growing at the content cap on very wide screens, with the dock floating at its
 * bottom.
 *
 * Tab walks the regions in reading order in every layout (AR-01), the top bar, the poster, the
 * canvas and then the dock.
 *
 * The shell only places the slots. The skin owned regions, `TopBarRegion` and `DockRegion` among
 * them, are for the slots to wear.
 *
 * @param[posterColors] The poster's colours, from the resolved document.
 * @param[posterCollapsed] Whether the poster shows as the rail. It collapses the docked poster on
 * Medium and Expanded, and on the narrow Medium rail false opens the poster over the canvas. The
 * phone sheet leaves it be.
 * @param[poster] The poster's content, told whether to show as the rail. That turns true as soon as
 * a collapse starts. An opening poster gets false at once and is laid out at its full width, which
 * its frame then reveals, so neither form squeezes through the widths in between.
 * @param[topBar] The top bar, usually a `TopBarRegion`.
 * @param[canvas] The preview and its tabs, given the padding its content scrolls out from under.
 * On a phone that clears the poster peek and the dock, and everywhere else it is zero.
 * @param[dock] The floating dock, usually a `DockRegion`.
 * @param[modifier] Applied to the whole shell.
 * @param[sheetState] Where the poster sheet rests on a phone.
 * @param[fullscreen] Whether to hide the poster and the top bar and give the canvas the window. The
 * dock stays and [fullscreenExit] floats at the top end, in a strip of its own over the canvas so it
 * covers none of it. The canvas and the dock keep their state across the switch, while the poster
 * and the top bar leave and start over when they come back.
 * @param[fullscreenExit] The floating pill that leaves fullscreen, shown only while [fullscreen].
 * @param[overlays] Drawn over everything else, such as toasts, panels and the command palette. A
 * `ToastRegion` in it stacks its toasts in the canvas frame, past the poster and above the dock.
 */
@Composable
public fun WorkspaceShell(
    posterColors: PosterColors,
    posterCollapsed: Boolean,
    poster: @Composable (rail: Boolean) -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable (contentPadding: PaddingValues) -> Unit,
    dock: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: BottomSheetState = rememberBottomSheetState(),
    // b-217
    fullscreen: Boolean = false,
    // b-217
    fullscreenExit: @Composable () -> Unit = {},
    overlays: @Composable () -> Unit = {},
) {
    val mode = LocalLayout.current.posterMode
    // b-217
    val exit = if (fullscreen) fullscreenExit else null
    val room = remember { ShellRoom() }
    Box(modifier.fillMaxSize().background(LocalBuilderTokens.current.panel)) {
        when (mode) {
            PosterMode.Sheet -> SheetShell(posterColors, sheetState, poster, topBar, canvas, dock, exit, room)
            PosterMode.Rail72,
            PosterMode.Docked320,
            PosterMode.Docked400,
            -> DockedShell(posterColors, mode, posterCollapsed, poster, topBar, canvas, dock, exit, room)
        }
        CompositionLocalProvider(LocalShellRoom provides room, content = overlays)
    }
}

/**
 * The phone layout, the canvas under the top bar and the poster in a sheet over both. With a
 * [fullscreenExit] the exit takes the top bar's place and the canvas the rest of the window, with
 * only the dock floating over it.
 *
 * Nothing that takes focus stays in the Tab order wholly under the sheet (WCAG 2.4.11). At Half the
 * canvas leaves it, and the dock too where the sheet covers it as placed, and at Full the top bar
 * goes as well. The top bar comes back as the sheet sinks to Half, and the canvas and a covered
 * dock at Peek. A click or a focus request still reaches what shows of them. When the sheet rises
 * over the region that holds focus, focus moves to the sheet's handle.
 */
@Composable
private fun SheetShell(
    posterColors: PosterColors,
    sheetState: BottomSheetState,
    poster: @Composable (rail: Boolean) -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable (contentPadding: PaddingValues) -> Unit,
    dock: @Composable () -> Unit,
    fullscreenExit: (@Composable () -> Unit)?,
    room: ShellRoom,
) {
    val tokens = LocalBuilderTokens.current
    val layout = LocalLayout.current
    // b-217
    val fullscreen = fullscreenExit != null
    val peek = if (fullscreen) 0.dp else posterPeekHeight(layout) // b-217
    val clearance = PaddingValues(
        // b-217
        bottom = if (fullscreen) tokens.spacing.large + ShellMetrics.dockHeight else sheetClearance(layout, tokens),
    )
    // The frame stands a margin off the bottom edge in fullscreen, and on the edge otherwise.
    val toastBottom = clearance.calculateBottomPadding() + if (fullscreen) tokens.spacing.medium else 0.dp
    val sheetOver = { detent: BottomSheetDetent -> !fullscreen && sheetState.targetDetent >= detent }
    val cover = remember { SheetCover() }
    // Asked as focus moves, so the dock as the skin draws it decides, not a nominal height.
    val dockCoveredFrom = {
        if (cover.dockUnder(sheetState, BottomSheetDetent.Half)) BottomSheetDetent.Half else BottomSheetDetent.Full
    }
    val handle = remember { FocusRequester() }
    val moves = remember(sheetState) { SheetMoveReport(sheetState) } // b-406g
    LaunchedEffect(sheetState, fullscreen) {
        if (fullscreen) return@LaunchedEffect
        var resting = sheetState.targetDetent
        snapshotFlow { sheetState.targetDetent }.collect { target ->
            // Moves inside a region never ask it, so focus already there would stay under the sheet.
            if (target > resting && cover.focusUnder(target, dockCoveredFrom())) handle.requestFocus()
            resting = target
        }
    }
    ShellLayout(
        start = { 0.dp },
        toasts = ToastInsets(start = { 0.dp }, end = 0.dp, bottom = toastBottom),
        room = room,
        topBar = {
            Box(
                Modifier.skipTabWhile(
                    covered = { sheetOver(BottomSheetDetent.Full) },
                    focused = { inside -> cover.topBarFocused = inside },
                ),
            ) {
                if (fullscreenExit == null) topBar() else FullscreenExitStrip(fullscreenExit) // b-217
            }
        },
        poster = {
            if (!fullscreen) { // b-217
                PosterSurface(posterColors) {
                    HeadlessBottomSheet(
                        state = sheetState,
                        label = stringResource(Res.string.shell_poster_label),
                        detentLabel = posterDetentNames(),
                        peekHeight = peek,
                        style = posterOverlayStyle(LocalBuilderTokens.current),
                        // The handle is the sheet's first stop, so the requester lands there.
                        modifier = Modifier
                            .zIndex(1f)
                            .fillMaxSize()
                            .onPlaced { coordinates -> cover.sheetTop = coordinates.positionInRoot().y }
                            .focusRequester(handle)
                            .nestedScroll(moves),
                    ) {
                        poster(false)
                    }
                }
            }
        },
        canvas = {
            CanvasFrame(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = tokens.spacing.medium)
                    // b-217
                    .padding(top = tokens.spacing.extraSmall, bottom = if (fullscreen) tokens.spacing.medium else 0.dp),
                // b-217
                shape = if (fullscreen) {
                    RoundedCornerShape(tokens.radius.large)
                } else {
                    RoundedCornerShape(topStart = tokens.radius.large, topEnd = tokens.radius.large)
                },
                canvas = {
                    Box(
                        Modifier.skipTabWhile(
                            covered = { sheetOver(BottomSheetDetent.Half) },
                            focused = { inside -> cover.canvasFocused = inside },
                        ),
                    ) {
                        canvas(clearance)
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = peek + tokens.spacing.large)
                        .onPlaced { coordinates -> cover.dockTop = coordinates.positionInRoot().y }
                        .skipTabWhile(
                            covered = { sheetOver(dockCoveredFrom()) },
                            focused = { inside -> cover.dockFocused = inside },
                        ),
                ) {
                    dock()
                }
            }
        },
    )
}

/**
 * Every wider layout. The poster stands on the start edge and the top bar and the canvas share the
 * rest. On the narrow Medium rail the canvas only makes room for the rail, so the opened poster
 * floats over the canvas instead of pushing it. With a [fullscreenExit] the exit takes the top
 * bar's place and the canvas the rest of the window inside the margin.
 */
@Composable
private fun DockedShell(
    posterColors: PosterColors,
    mode: PosterMode,
    collapsed: Boolean,
    poster: @Composable (rail: Boolean) -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable (contentPadding: PaddingValues) -> Unit,
    dock: @Composable () -> Unit,
    fullscreenExit: (@Composable () -> Unit)?,
    room: ShellRoom,
) {
    val tokens = LocalBuilderTokens.current
    val margin = tokens.spacing.medium
    val open = !collapsed
    val openWidth = if (mode == PosterMode.Docked400) ShellMetrics.posterWideWidth else ShellMetrics.posterNarrowWidth
    val floats = mode == PosterMode.Rail72
    val openness by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = posterSpec(open),
        label = "posterOpenness",
    )
    val posterWidth = { lerp(ShellMetrics.railWidth, openWidth, openness) }
    val roomWidth = if (floats) ({ ShellMetrics.railWidth }) else posterWidth
    // b-217
    val fullscreen = fullscreenExit != null
    ShellLayout(
        start = { if (fullscreen) 0.dp else roomWidth() + margin * 2 }, // b-217
        toasts = ToastInsets(
            start = { if (fullscreen) margin else posterWidth() + margin * 2 },
            end = margin,
            bottom = margin + tokens.spacing.large + ShellMetrics.dockHeight,
        ),
        room = room,
        topBar = { if (fullscreenExit == null) topBar() else FullscreenExitStrip(fullscreenExit) }, // b-217
        poster = {
            if (!fullscreen) { // b-217
                PosterSurface(posterColors) {
                    PosterPanel(
                        modifier = Modifier
                            .zIndex(1f)
                            .padding(margin)
                            .fillMaxHeight()
                            .layoutWidth(posterWidth),
                        floating = floats && open,
                        contentWidth = if (open) openWidth else null,
                    ) {
                        poster(!open)
                    }
                }
            }
        },
        canvas = {
            CanvasFrame(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (fullscreen) margin else 0.dp) // b-217
                    .padding(end = margin, bottom = margin),
                shape = RoundedCornerShape(tokens.radius.large),
                canvas = { canvas(PaddingValues()) },
            ) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = tokens.spacing.large)) { dock() }
            }
        },
    )
}

// b-217

/**
 * The strip across the top that holds [exit] at its end while the top bar is away, so the canvas
 * starts below the pill and a tab row that fills the width stays clear of it.
 */
@Composable
private fun FullscreenExitStrip(exit: @Composable () -> Unit) {
    val spacing = LocalBuilderTokens.current.spacing
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium, vertical = spacing.small),
        contentAlignment = Alignment.CenterEnd,
    ) {
        exit()
    }
}

/**
 * Places the top bar, the poster and the canvas, in that order.
 *
 * Tab follows the order children are placed in, so this is what puts the poster between the top
 * bar and the canvas for keyboard users. The poster still draws over the canvas through its own
 * `zIndex`. The top bar and the canvas stand [start] in from the start edge, read at layout time so
 * the rail animating only lays out again, and the canvas takes the height the top bar leaves. The
 * toast room, [toasts] in from the edges and below the top bar, goes to [room] as it measures.
 */
@Composable
private fun ShellLayout(
    start: () -> Dp,
    toasts: ToastInsets,
    room: ShellRoom,
    topBar: @Composable () -> Unit,
    poster: @Composable () -> Unit,
    canvas: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(topBar, poster, canvas),
        modifier = Modifier.fillMaxSize(),
    ) { (bars, posters, canvases), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val x = start().roundToPx().coerceIn(0, width)
        val bar = bars.map { measurable -> measurable.measure(Constraints(maxWidth = width - x, maxHeight = height)) }
        val barHeight = bar.maxOfOrNull { placeable -> placeable.height } ?: 0
        val panel = posters.map { measurable -> measurable.measure(Constraints(maxWidth = width, maxHeight = height)) }
        val frame = Constraints.fixed(width - x, (height - barHeight).coerceAtLeast(0))
        val frames = canvases.map { measurable -> measurable.measure(frame) }
        val toastStart = toasts.start().roundToPx().coerceIn(0, width)
        room.toasts = IntRect(
            left = toastStart,
            top = barHeight,
            right = (width - toasts.end.roundToPx()).coerceAtLeast(toastStart),
            bottom = (height - toasts.bottom.roundToPx()).coerceAtLeast(barHeight),
        )
        layout(width, height) {
            bar.forEach { placeable -> placeable.placeRelative(x, 0) }
            panel.forEach { placeable -> placeable.placeRelative(0, 0) }
            frames.forEach { placeable -> placeable.placeRelative(x, barHeight) }
        }
    }
}

/** The canvas frame, with its content capped on very wide screens and centred in what is left. */
@Composable
private fun CanvasFrame(
    modifier: Modifier,
    shape: Shape,
    canvas: @Composable () -> Unit,
    floating: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.clip(shape).background(LocalBuilderTokens.current.canvas),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = LocalLayout.current.canvasMaxWidth).fillMaxSize()) { canvas() }
        floating()
    }
}
