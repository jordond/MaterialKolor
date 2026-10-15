package com.materialkolor.builder.kit.headless

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import com.materialkolor.builder.kit.skin.headless.isOverlayShown
import com.materialkolor.builder.kit.skin.headless.panelEnter
import com.materialkolor.builder.kit.skin.headless.panelExit
import com.materialkolor.builder.kit.skin.headless.rememberOverlayVisibility
import com.materialkolor.builder.kit.skin.headless.scrimEnter
import com.materialkolor.builder.kit.skin.headless.scrimExit
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A modal sheet that rises from the bottom of its parent and stays inside it, over a veil on the
 * rest of the parent.
 *
 * Unlike [HeadlessModal] it is drawn in the page rather than in a layer of its own, so it covers
 * only the region it stands in. Focus moves into it as it opens and Tab and Shift+Tab stop at its
 * ends rather than leave it. Esc and a click on the veil both ask to close it, and once it has
 * gone focus goes back to [returnFocusTo]. It slides up, or only fades under reduced motion. Inside
 * an [InsetSheetHost] the region under it also leaves the semantics tree while it is open.
 *
 * The panel is the surrounding panel colour, so inside an inverse poster it is the poster's ink.
 * The skin's [style] gives its corners, edge, shadow, veil and handle.
 *
 * @param[open] Whether the sheet is wanted.
 * @param[onDismiss] Called on Esc and on a click on the veil.
 * @param[title] The sheet's pane title.
 * @param[maxHeight] The tallest the sheet gets. It never grows past its parent either.
 * @param[style] The skin's overlay style.
 * @param[returnFocusTo] The trigger that opened the sheet, focused again once it is gone.
 * @param[modifier] Applied to the sheet panel.
 * @param[content] The sheet's header and body, under the handle.
 */
@Composable
internal fun HeadlessInsetSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    title: String,
    maxHeight: Dp,
    style: OverlayStyle,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberOverlayVisibility(open)
    val shown = state.isOverlayShown(open)
    ReturnFocusWhenGone(shown, returnFocusTo)
    val host = LocalInsetSheetHost.current
    if (host != null) {
        DisposableEffect(host, shown) {
            host.covered = shown
            onDispose { host.covered = false }
        }
    }
    if (!shown) return
    val dismiss by rememberUpdatedState(onDismiss)
    val panel = LocalBuilderTokens.current.panel
    AnimatedVisibility(
        visibleState = state,
        modifier = Modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        val focus = remember { OverlayFocus() }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                    if (escape) dismiss()
                    escape
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .animateEnterExit(enter = scrimEnter(), exit = scrimExit())
                    .fillMaxSize()
                    .background(style.scrim.copy(alpha = InsetSheetVeilAlpha))
                    .pointerInput(Unit) { detectTapGestures { dismiss() } },
            )
            val shape = RoundedCornerShape(style.drawerRadius)
            Column(
                modifier = modifier
                    .animateEnterExit(enter = panelEnter(PanelEdge.Bottom), exit = panelExit(PanelEdge.Bottom))
                    .fillMaxWidth()
                    .heightIn(max = min(maxHeight, this.maxHeight))
                    .shadow(style.shadow, shape)
                    .clip(shape)
                    .background(panel)
                    .then(if (style.border != null) Modifier.border(style.border, shape) else Modifier)
                    .modalPane(title)
                    .keepTaps()
                    .keepTabInside()
                    .then(focus.modifier),
            ) {
                InsetSheetHandle(style)
                content()
            }
        }
        LaunchedEffect(Unit) { focus.enter() }
    }
}

/**
 * The grab handle across the top of the sheet. It only shows the sheet can be lowered, so it takes
 * no focus and says nothing.
 */
@Composable
private fun ColumnScope.InsetSheetHandle(style: OverlayStyle) {
    val spacing = LocalBuilderTokens.current.spacing
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = spacing.small + spacing.extraSmall)
            .size(OverlayMetrics.sheetHandleWidth, OverlayMetrics.sheetHandleHeight)
            .background(style.thumb, RoundedCornerShape(OverlayMetrics.sheetHandleHeight)),
    )
}

/**
 * Keeps Tab and Shift+Tab from leaving the sheet, so they stop at its first and last stops.
 */
private fun Modifier.keepTabInside(): Modifier =
    focusProperties {
        onExit = {
            val direction = requestedFocusDirection
            if (direction == FocusDirection.Next || direction == FocusDirection.Previous) cancelFocusChange()
        }
    }

/**
 * Where an inset sheet sits, and whether one is covering the region beside it.
 */
@Stable
internal class InsetSheetHost {
    /**
     * Whether a sheet is on screen, or still on its way in or out.
     */
    var covered: Boolean by mutableStateOf(false)
}

/**
 * The host the inset sheets below it report to, or null outside one.
 */
internal val LocalInsetSheetHost = staticCompositionLocalOf<InsetSheetHost?> { null }

/**
 * Lays [sheet] over [content] in the same bounds, so a sheet stays put while [content] scrolls.
 * While a sheet is open [content] leaves the semantics tree, the way the page under a modal does.
 */
@Composable
internal fun HeadlessInsetSheetHost(
    modifier: Modifier,
    sheet: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val host = remember { InsetSheetHost() }
    Box(modifier, propagateMinConstraints = true) {
        Box(
            modifier = if (host.covered) Modifier.clearAndSetSemantics {} else Modifier,
            propagateMinConstraints = true,
        ) { content() }
        Box(Modifier.matchParentSize()) {
            CompositionLocalProvider(LocalInsetSheetHost provides host, content = sheet)
        }
    }
}

/**
 * How strongly the veil under an inset sheet dims its region. It is lighter than a full modal's,
 * since what stays in view above the sheet still matters.
 */
private const val InsetSheetVeilAlpha = 0.32f
