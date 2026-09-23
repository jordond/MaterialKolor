package com.materialkolor.builder.kit.shell

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.BottomSheetState
import com.materialkolor.builder.kit.control.rememberBottomSheetState
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.sheet_detent_full
import com.materialkolor.builder.kit.generated.resources.sheet_detent_half
import com.materialkolor.builder.kit.generated.resources.sheet_detent_peek
import com.materialkolor.builder.kit.generated.resources.shell_poster_label
import com.materialkolor.builder.kit.headless.HeadlessBottomSheet
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.layout.ShortHeightBreakpoint
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
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
 * @param[overlays] Drawn over everything else, such as toasts, panels and the command palette.
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
    overlays: @Composable () -> Unit = {},
) {
    val mode = LocalLayout.current.posterMode
    Box(modifier.fillMaxSize().background(LocalBuilderTokens.current.panel)) {
        when (mode) {
            PosterMode.Sheet -> SheetShell(posterColors, sheetState, poster, topBar, canvas, dock)
            PosterMode.Rail72,
            PosterMode.Docked320,
            PosterMode.Docked400,
            -> DockedShell(posterColors, mode, posterCollapsed, poster, topBar, canvas, dock)
        }
        overlays()
    }
}

/** The phone layout, the canvas under the top bar and the poster in a sheet over both. */
@Composable
private fun SheetShell(
    posterColors: PosterColors,
    sheetState: BottomSheetState,
    poster: @Composable (rail: Boolean) -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable (contentPadding: PaddingValues) -> Unit,
    dock: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val layout = LocalLayout.current
    val peek = posterPeekHeight(layout)
    val clearance = PaddingValues(bottom = sheetClearance(layout, tokens))
    ShellLayout(
        start = { 0.dp },
        topBar = topBar,
        poster = {
            PosterSurface(posterColors) {
                HeadlessBottomSheet(
                    state = sheetState,
                    label = stringResource(Res.string.shell_poster_label),
                    detentLabel = posterDetentNames(),
                    peekHeight = peek,
                    style = posterOverlayStyle(LocalBuilderTokens.current),
                    modifier = Modifier.zIndex(1f).fillMaxSize(),
                ) {
                    poster(false)
                }
            }
        },
        canvas = {
            CanvasFrame(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = tokens.spacing.medium)
                    .padding(top = tokens.spacing.extraSmall),
                shape = RoundedCornerShape(topStart = tokens.radius.large, topEnd = tokens.radius.large),
                canvas = { canvas(clearance) },
            ) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = peek + tokens.spacing.large)) { dock() }
            }
        },
    )
}

/**
 * Every wider layout. The poster stands on the start edge and the top bar and the canvas share the
 * rest. On the narrow Medium rail the canvas only makes room for the rail, so the opened poster
 * floats over the canvas instead of pushing it.
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
    ShellLayout(
        start = { roomWidth() + margin * 2 },
        topBar = topBar,
        poster = {
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
        },
        canvas = {
            CanvasFrame(
                modifier = Modifier.fillMaxSize().padding(end = margin, bottom = margin),
                shape = RoundedCornerShape(tokens.radius.large),
                canvas = { canvas(PaddingValues()) },
            ) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = tokens.spacing.large)) { dock() }
            }
        },
    )
}

/**
 * Places the top bar, the poster and the canvas, in that order.
 *
 * Tab follows the order children are placed in, so this is what puts the poster between the top
 * bar and the canvas for keyboard users. The poster still draws over the canvas through its own
 * `zIndex`. The top bar and the canvas stand [start] in from the start edge, read at layout time so
 * the rail animating only lays out again, and the canvas takes the height the top bar leaves.
 */
@Composable
private fun ShellLayout(
    start: () -> Dp,
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
        val room = canvases.map { measurable -> measurable.measure(frame) }
        layout(width, height) {
            bar.forEach { placeable -> placeable.placeRelative(x, 0) }
            panel.forEach { placeable -> placeable.placeRelative(0, 0) }
            room.forEach { placeable -> placeable.placeRelative(x, barHeight) }
        }
    }
}

/**
 * The poster's own frame, the seed page with design D's corners, lifted when it floats.
 *
 * With a [contentWidth] the content is laid out at that width whatever the frame's, so an opening
 * panel is revealed by the frame rather than squeezed by it. Without one it fills the frame.
 */
@Composable
private fun PosterPanel(
    modifier: Modifier,
    floating: Boolean,
    contentWidth: Dp?,
    content: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val style = posterOverlayStyle(tokens)
    val shape = RoundedCornerShape(tokens.radius.large)
    val label = stringResource(Res.string.shell_poster_label)
    Box(
        modifier = modifier
            .then(if (floating) Modifier.shadow(style.shadow, shape) else Modifier)
            .clip(shape)
            .background(style.surface)
            .semantics { paneTitle = label },
    ) {
        val sized = if (contentWidth == null) Modifier.fillMaxWidth() else Modifier.revealWidth(contentWidth)
        Box(sized.fillMaxHeight()) { content() }
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

/**
 * The poster's overlay dress, the same in every skin since the poster is content. It is the Custom
 * dress with its soft shadow and large corners, standing on the page rather than a raised surface.
 * Read it inside [PosterSurface], so its colours are the poster's.
 */
private fun posterOverlayStyle(tokens: BuilderTokens): OverlayStyle =
    customOverlayStyle(tokens.copy(panelRaised = tokens.panel))

/** The rail opens with the panel arrival motion and closes with the exit, and snaps under reduced motion (MO-05). */
@Composable
private fun posterSpec(opening: Boolean): AnimationSpec<Float> {
    val motion = LocalBuilderMotion.current
    return when {
        LocalReducedMotion.current -> snap()
        opening -> motion.panelEnter()
        else -> motion.panelExit()
    }
}

/**
 * Sizes to [width] at layout time, so the rail animating open or shut only lays out again and
 * never recomposes the poster.
 */
private fun Modifier.layoutWidth(width: () -> Dp): Modifier =
    layout { measurable, constraints ->
        val px = width().roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = px, maxWidth = px))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * Lays the content out at [width] even while the space it is given is narrower, its start edge on
 * the start edge. Whatever sticks out past the end is for the parent to clip.
 */
private fun Modifier.revealWidth(width: Dp): Modifier =
    layout { measurable, constraints ->
        val px = width.roundToPx()
        val placeable = measurable.measure(constraints.copy(minWidth = px, maxWidth = px))
        val shown = px.coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(shown, placeable.height) { placeable.placeRelative(0, 0) }
    }

/** The kit's names for the detents, remembered on the three names like the public sheet's. */
@Composable
private fun posterDetentNames(): (BottomSheetDetent) -> String {
    val peek = stringResource(Res.string.sheet_detent_peek)
    val half = stringResource(Res.string.sheet_detent_half)
    val full = stringResource(Res.string.sheet_detent_full)
    return remember(peek, half, full) {
        { detent: BottomSheetDetent ->
            when (detent) {
                BottomSheetDetent.Peek -> peek
                BottomSheetDetent.Half -> half
                BottomSheetDetent.Full -> full
            }
        }
    }
}
