package com.materialkolor.builder.kit.shell

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
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

    /** A phone shorter than this is lying on its side, and its poster peek shrinks. */
    val shortHeight: Dp = 480.dp

    /** The room a floating dock takes, Material's floating toolbar height. */
    val dockHeight: Dp = 64.dp
}

/**
 * How much of the poster sheet shows at peek. A phone in landscape shrinks it to the kit's plain
 * sheet peek so the canvas keeps some room.
 */
internal fun posterPeekHeight(layout: LayoutInfo): Dp =
    if (layout.heightDp < ShellMetrics.shortHeight) OverlayMetrics.sheetPeekHeight else ShellMetrics.posterPeekHeight

/**
 * The workspace laid out for the space it is in, the poster, the top bar, the canvas and the dock.
 *
 * The geometry comes from `LocalLayout` and is the same in every skin (design D). On Expanded the
 * poster docks at 400 dp and collapses to the 72 dp rail. On Medium it docks at 320 dp from 840 dp,
 * and below that it is the rail, which opens over the canvas. On a phone it lives in a bottom sheet
 * with the dock floating above its peek. The poster keeps its seed coloured look in every skin, since
 * the shell draws it inside [PosterSurface]. The canvas is a rounded frame whose content stops
 * growing at the content cap on very wide screens, with the dock floating at its bottom.
 *
 * The shell only places the slots. The skin owned regions, `TopBarRegion` and `DockRegion` among
 * them, are for the slots to wear.
 *
 * @param[posterColors] The poster's colours, from the resolved document.
 * @param[posterCollapsed] Whether the poster shows as the rail. It collapses the docked poster on
 * Expanded, and on the narrow Medium rail false opens the poster over the canvas. The 320 dp poster
 * and the phone sheet leave it be.
 * @param[poster] The poster's content.
 * @param[topBar] The top bar, usually a `TopBarRegion`.
 * @param[canvas] The preview and its tabs.
 * @param[dock] The floating dock, usually a `DockRegion`.
 * @param[modifier] Applied to the whole shell.
 * @param[sheetState] Where the poster sheet rests on a phone.
 * @param[overlays] Drawn over everything else, such as toasts, panels and the command palette.
 */
@Composable
public fun WorkspaceShell(
    posterColors: PosterColors,
    posterCollapsed: Boolean,
    poster: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable () -> Unit,
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
private fun BoxScope.SheetShell(
    posterColors: PosterColors,
    sheetState: BottomSheetState,
    poster: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable () -> Unit,
    dock: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val peek = posterPeekHeight(LocalLayout.current)
    Column(Modifier.fillMaxSize()) {
        topBar()
        CanvasFrame(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = tokens.spacing.medium, top = tokens.spacing.extraSmall, end = tokens.spacing.medium),
            shape = RoundedCornerShape(topStart = tokens.radius.large, topEnd = tokens.radius.large),
            canvas = canvas,
        )
    }
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = tokens.spacing.medium, end = tokens.spacing.medium, bottom = peek + tokens.spacing.large),
    ) {
        dock()
    }
    PosterSurface(posterColors) {
        HeadlessBottomSheet(
            state = sheetState,
            label = stringResource(Res.string.shell_poster_label),
            detentLabel = posterDetentNames(),
            peekHeight = peek,
            style = posterOverlayStyle(LocalBuilderTokens.current),
            modifier = Modifier.fillMaxSize(),
        ) {
            poster()
        }
    }
}

/**
 * Every wider layout. The poster stands on the start edge and the top bar and the canvas share the
 * rest. On the narrow Medium rail the row only makes room for the rail, so the opened poster floats
 * over the canvas instead of pushing it.
 */
@Composable
private fun BoxScope.DockedShell(
    posterColors: PosterColors,
    mode: PosterMode,
    collapsed: Boolean,
    poster: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    canvas: @Composable () -> Unit,
    dock: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val margin = tokens.spacing.medium
    val open = mode == PosterMode.Docked320 || !collapsed
    val openWidth = if (mode == PosterMode.Docked400) ShellMetrics.posterWideWidth else ShellMetrics.posterNarrowWidth
    val floats = mode == PosterMode.Rail72
    val openness by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = posterSpec(open),
        label = "posterOpenness",
    )
    val posterWidth = { lerp(ShellMetrics.railWidth, openWidth, openness) }
    val roomWidth = if (floats) ({ ShellMetrics.railWidth }) else posterWidth
    Row(Modifier.fillMaxSize()) {
        Spacer(Modifier.fillMaxHeight().padding(horizontal = margin).layoutWidth(roomWidth))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            topBar()
            CanvasFrame(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(end = margin, bottom = margin),
                shape = RoundedCornerShape(tokens.radius.large),
                canvas = canvas,
            ) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = tokens.spacing.large)) { dock() }
            }
        }
    }
    PosterSurface(posterColors) {
        PosterPanel(
            modifier = Modifier.padding(margin).fillMaxHeight().layoutWidth(posterWidth),
            floating = floats && open,
            content = poster,
        )
    }
}

/** The poster's own frame, the seed page with design D's corners, lifted when it floats. */
@Composable
private fun PosterPanel(
    modifier: Modifier,
    floating: Boolean,
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
        content()
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
