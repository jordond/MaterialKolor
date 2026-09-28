package com.materialkolor.builder.kit.skin.headless

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.a11y.collectIsFocusVisibleAsState
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.token.BuilderTokens

/**
 * How one skin styles the headless overlays.
 *
 * Every colour comes from the skin's tokens or its library theme. The veil is the skin's scrim
 * token, its canvas on Custom and Material's scrim role on Material3.
 *
 * @property[surface] Menus, dialogs, panels and sheets.
 * @property[content] Ink on [surface].
 * @property[muted] Secondary ink on [surface].
 * @property[border] The outline of a popover or a panel, or null for none.
 * @property[popoverShape] Menus, tooltips and toasts.
 * @property[dialogShape] A dialog panel.
 * @property[panelRadius] The rounding on the inner edge of a side panel or the top of a sheet.
 * @property[drawerRadius] The rounding on the inner corners of a modal side panel or end sheet.
 * @property[divider] The hairline over a panel's footer.
 * @property[shadow] How far a popover or a panel floats.
 * @property[scrim] The veil behind a dialog, a panel or a sheet.
 * @property[itemShape] A row inside a menu, or a small button inside a panel.
 * @property[highlight] The ground of a hovered or focused row.
 * @property[selected] The ground of the selected option.
 * @property[focus] The keyboard focus ring.
 * @property[field] The ground of a select field.
 * @property[fieldBorder] The outline of a select field.
 * @property[tooltip] The ground of a tooltip.
 * @property[tooltipContent] Ink on [tooltip].
 * @property[tooltipBorder] The outline of a tooltip, or null for none.
 * @property[thumb] A scrollbar thumb, and the grab handle of a sheet.
 * @property[panelTitle] The type of a panel's title, or null for the builder's own title type.
 */
@Immutable
internal class OverlayStyle(
    val surface: Color,
    val content: Color,
    val muted: Color,
    val border: BorderStroke?,
    val popoverShape: Shape,
    val dialogShape: Shape,
    val panelRadius: Dp,
    val drawerRadius: Dp,
    val divider: Color,
    val shadow: Dp,
    val scrim: Color,
    val itemShape: Shape,
    val highlight: Color,
    val selected: Color,
    val focus: Color,
    val field: Color,
    val fieldBorder: BorderStroke,
    val tooltip: Color,
    val tooltipContent: Color,
    val tooltipBorder: BorderStroke?,
    val thumb: Color,
    val panelTitle: TextStyle? = null,
)

/**
 * How much of the canvas shows through the veil behind a modal overlay, on the skins that veil with it.
 */
internal const val ScrimAlpha: Float = 0.6f

/**
 * The sizes every skin gives its overlays alike.
 *
 * Every skin gives each one the same value, so none of them became a builder token. A size that
 * starts to differ by skin moves to the tokens then.
 */
internal object OverlayMetrics {
    /**
     * The narrowest and the widest a dialog panel gets.
     */
    val dialogMinWidth: Dp = 280.dp
    val dialogMaxWidth: Dp = 560.dp

    /**
     * The widest the toast stack gets.
     */
    val toastMaxWidth: Dp = 560.dp

    /**
     * How much of a bottom sheet shows at peek.
     */
    val sheetPeekHeight: Dp = 96.dp

    /**
     * The widest a side panel gets, and how much of a narrower screen it takes.
     */
    val sidePanelWidth: Dp = 380.dp
    val sidePanelNarrowFraction: Float = 0.85f

    /**
     * The narrowest, the widest and the tallest a dropdown list gets.
     */
    val menuMinWidth: Dp = 160.dp
    val menuMaxWidth: Dp = 360.dp
    val menuMaxHeight: Dp = 400.dp

    /**
     * How far a modal side panel or end sheet keeps its header, body and footer from its edges.
     */
    val panelPadding: Dp = 24.dp

    /**
     * How far a popover hangs under its anchor, from the anchor to the popover's own edge.
     */
    val popoverGap: Dp = 8.dp

    /**
     * How tall a row stands in a long list inside an overlay, such as the command palette's.
     */
    val denseRowHeight: Dp = 44.dp

    /**
     * The widest a tooltip gets before its label wraps.
     */
    val tooltipMaxWidth: Dp = 280.dp

    /**
     * How long the pointer rests on a tooltip's anchor before the label shows.
     */
    const val tooltipDelayMillis: Long = 500L

    /**
     * How long a label stays once the pointer leaves, so the pointer can cross the gap onto it.
     */
    const val tooltipGraceMillis: Long = 100L

    /**
     * The grab handle drawn on top of a bottom sheet.
     */
    val sheetHandleWidth: Dp = 32.dp
    val sheetHandleHeight: Dp = 4.dp

    /**
     * How thick a scrollbar thumb is, and how far it keeps from the edge.
     */
    val thumbThickness: Dp = 6.dp
    val thumbInset: Dp = 2.dp
}

/**
 * Custom floats its overlays on soft shadows and rounds them the way the identity does.
 */
internal fun customOverlayStyle(tokens: BuilderTokens): OverlayStyle =
    OverlayStyle(
        surface = tokens.panelRaised,
        content = tokens.textStrong,
        muted = tokens.textMuted,
        border = null,
        popoverShape = RoundedCornerShape(tokens.radius.medium),
        dialogShape = RoundedCornerShape(tokens.radius.large),
        panelRadius = tokens.radius.large,
        drawerRadius = tokens.radius.large,
        divider = tokens.border,
        shadow = 8.dp,
        scrim = tokens.scrim,
        itemShape = RoundedCornerShape(tokens.radius.small),
        highlight = tokens.accent.copy(alpha = 0.1f),
        selected = tokens.accent.copy(alpha = 0.16f),
        focus = tokens.focus,
        field = tokens.panelRaised,
        fieldBorder = BorderStroke(tokens.outlineWidth, tokens.border),
        tooltip = tokens.textStrong,
        tooltipContent = tokens.panel,
        tooltipBorder = null,
        thumb = tokens.textMuted,
    )

/**
 * Which edge a panel is pinned to, and so where it slides from.
 */
internal enum class PanelEdge {
    Start,
    End,
    Bottom,
}

/**
 * A menu or a tooltip scaling up from 0.96 with a fade, or only the fade under reduced motion.
 */
@Composable
internal fun popoverEnter(): EnterTransition {
    val motion = LocalBuilderMotion.current
    val fade = fadeIn(motion.popover())
    return if (LocalReducedMotion.current) fade else fade + scaleIn(motion.popover(), PopoverInitialScale)
}

/**
 * The same popover leaving.
 */
@Composable
internal fun popoverExit(): ExitTransition {
    val motion = LocalBuilderMotion.current
    val fade = fadeOut(motion.panelExit())
    return if (LocalReducedMotion.current) fade else fade + scaleOut(motion.panelExit(), PopoverInitialScale)
}

/**
 * A panel sliding in from [edge], or only fading under reduced motion.
 */
@Composable
internal fun panelEnter(edge: PanelEdge): EnterTransition {
    val motion = LocalBuilderMotion.current
    if (LocalReducedMotion.current) return fadeIn(motion.panelEnter())
    val sign = edgeSign(edge)
    return when (edge) {
        PanelEdge.Start, PanelEdge.End -> slideInHorizontally(motion.panelEnter()) { width -> sign * width }
        PanelEdge.Bottom -> slideInVertically(motion.panelEnter()) { height -> height }
    }
}

/**
 * The same panel leaving, quicker than it arrived.
 */
@Composable
internal fun panelExit(edge: PanelEdge): ExitTransition {
    val motion = LocalBuilderMotion.current
    if (LocalReducedMotion.current) return fadeOut(motion.panelExit())
    val sign = edgeSign(edge)
    return when (edge) {
        PanelEdge.Start, PanelEdge.End -> slideOutHorizontally(motion.panelExit()) { width -> sign * width }
        PanelEdge.Bottom -> slideOutVertically(motion.panelExit()) { height -> height }
    }
}

/**
 * The scrim fading in step with its panel.
 */
@Composable
internal fun scrimEnter(): EnterTransition = fadeIn(LocalBuilderMotion.current.panelEnter())

@Composable
internal fun scrimExit(): ExitTransition = fadeOut(LocalBuilderMotion.current.panelExit())

@Composable
private fun edgeSign(edge: PanelEdge): Int {
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    return when (edge) {
        PanelEdge.Start -> if (ltr) -1 else 1
        PanelEdge.End -> if (ltr) 1 else -1
        PanelEdge.Bottom -> 1
    }
}

private const val PopoverInitialScale = 0.96f

/**
 * Keeps an overlay composed while it animates out.
 *
 * Read [isOverlayShown] to decide whether to compose the popup or the layer at all, and hand the
 * state to `AnimatedVisibility` inside it.
 */
@Composable
internal fun rememberOverlayVisibility(visible: Boolean): MutableTransitionState<Boolean> {
    val state = remember { MutableTransitionState(false) }
    SideEffect { state.targetState = visible }
    return state
}

/**
 * True while the overlay is on screen or still on its way in or out.
 */
internal fun MutableTransitionState<Boolean>.isOverlayShown(visible: Boolean): Boolean =
    visible || currentState || targetState || !isIdle

/**
 * The feedback of a row or a small button inside an overlay.
 *
 * It shrinks by the skin's press scale, takes [highlight] while hovered or focused, and draws the
 * focus ring on keyboard focus, not on focus a click left behind. A disabled target only fades.
 */
@Composable
internal fun Modifier.overlayFeedback(
    interactionSource: InteractionSource,
    style: OverlayStyle,
    shape: Shape = style.itemShape,
    enabled: Boolean = true,
    selected: Boolean = false,
    highlight: Color = style.highlight,
    focus: Color = style.focus,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val ringed by interactionSource.collectIsFocusVisibleAsState()
    val ground = when {
        !enabled -> Color.Transparent
        selected -> style.selected
        hovered || focused -> highlight
        else -> Color.Transparent
    }
    return this
        .controlPress(interactionSource, enabled)
        .background(ground, shape)
        .then(if (ringed) Modifier.border(FocusRingWidth, focus, shape) else Modifier)
        .then(if (enabled) Modifier else Modifier.alpha(DisabledAlpha))
}
