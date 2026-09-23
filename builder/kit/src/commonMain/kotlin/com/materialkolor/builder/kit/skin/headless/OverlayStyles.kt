package com.materialkolor.builder.kit.skin.headless

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.material.materialOverlayStyle
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * How one skin dresses the headless overlays.
 *
 * Every colour comes from the skin's tokens or its library theme. There is no scrim token, so each
 * skin veils the page with its own canvas or its library's scrim role.
 *
 * @property[surface] Menus, dialogs, panels and sheets.
 * @property[content] Ink on [surface].
 * @property[muted] Secondary ink on [surface].
 * @property[border] The outline of a popover or a panel, or null for none.
 * @property[popoverShape] Menus, tooltips and toasts.
 * @property[dialogShape] A dialog panel.
 * @property[panelRadius] The rounding on the inner edge of a side panel or the top of a sheet.
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
 * @property[toast] The ground of a toast.
 * @property[toastContent] Ink on [toast].
 * @property[toastBorder] The outline of a toast, or null for none.
 * @property[thumb] A scrollbar thumb, and the grab handle of a sheet.
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
    val toast: Color,
    val toastContent: Color,
    val toastBorder: BorderStroke?,
    val thumb: Color,
)

/** The style set the headless overlays wear in [library]. */
@Composable
internal fun overlayStyle(library: Library): OverlayStyle =
    when (library) {
        Library.Material3 -> materialOverlayStyle()
        Library.Unstyled -> unstyledOverlayStyle(LocalBuilderTokens.current)
        Library.Fluent -> fluentOverlayStyle(LocalBuilderTokens.current) // fluent-placeholder
        Library.Custom -> customOverlayStyle(LocalBuilderTokens.current)
    }

/** How much of the canvas shows through the veil behind a modal overlay. */
internal const val ScrimAlpha: Float = 0.6f

/** How much a disabled row fades. */
internal const val DisabledAlpha: Float = 0.38f

/** The width of a hairline border and of the focus ring. */
internal val Hairline: Dp = 1.dp
internal val FocusRingWidth: Dp = 2.dp

/** Unstyled keeps to hairlines and small corners, and floats nothing. */
internal fun unstyledOverlayStyle(tokens: BuilderTokens): OverlayStyle {
    val hairline = BorderStroke(Hairline, tokens.border)
    return OverlayStyle(
        surface = tokens.panel,
        content = tokens.textStrong,
        muted = tokens.textMuted,
        border = hairline,
        popoverShape = RoundedCornerShape(tokens.radius.small),
        dialogShape = RoundedCornerShape(tokens.radius.small),
        panelRadius = 0.dp,
        shadow = 0.dp,
        scrim = tokens.canvas.copy(alpha = ScrimAlpha),
        itemShape = RoundedCornerShape(tokens.radius.small),
        highlight = tokens.textStrong.copy(alpha = 0.06f),
        selected = tokens.textStrong.copy(alpha = 0.1f),
        focus = tokens.focus,
        field = tokens.panel,
        fieldBorder = BorderStroke(Hairline, tokens.borderStrong),
        tooltip = tokens.textStrong,
        tooltipContent = tokens.panel,
        tooltipBorder = null,
        toast = tokens.panelRaised,
        toastContent = tokens.textStrong,
        toastBorder = hairline,
        thumb = tokens.borderStrong,
    )
}

/** Custom floats its overlays on soft shadows and rounds them the way the identity does. */
internal fun customOverlayStyle(tokens: BuilderTokens): OverlayStyle =
    OverlayStyle(
        surface = tokens.panelRaised,
        content = tokens.textStrong,
        muted = tokens.textMuted,
        border = null,
        popoverShape = RoundedCornerShape(tokens.radius.medium),
        dialogShape = RoundedCornerShape(tokens.radius.large),
        panelRadius = tokens.radius.large,
        shadow = 8.dp,
        scrim = tokens.canvas.copy(alpha = ScrimAlpha),
        itemShape = RoundedCornerShape(tokens.radius.small),
        highlight = tokens.accent.copy(alpha = 0.1f),
        selected = tokens.accent.copy(alpha = 0.16f),
        focus = tokens.focus,
        field = tokens.panelRaised,
        fieldBorder = BorderStroke(Hairline, tokens.border),
        tooltip = tokens.textStrong,
        tooltipContent = tokens.panel,
        tooltipBorder = null,
        toast = tokens.textStrong,
        toastContent = tokens.panel,
        toastBorder = null,
        thumb = tokens.textMuted,
    )

/** Which edge a panel is pinned to, and so where it slides from. */
internal enum class PanelEdge {
    Start,
    End,
    Bottom,
}

/** A menu or a tooltip scaling up from 0.96 with a fade, or only the fade under reduced motion (MO-05). */
@Composable
internal fun popoverEnter(): EnterTransition {
    val motion = LocalBuilderMotion.current
    val fade = fadeIn(motion.popover())
    return if (LocalReducedMotion.current) fade else fade + scaleIn(motion.popover(), PopoverInitialScale)
}

/** The same popover leaving. */
@Composable
internal fun popoverExit(): ExitTransition {
    val motion = LocalBuilderMotion.current
    val fade = fadeOut(motion.panelExit())
    return if (LocalReducedMotion.current) fade else fade + scaleOut(motion.panelExit(), PopoverInitialScale)
}

/** A panel sliding in from [edge], or only fading under reduced motion (MO-05). */
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

/** The same panel leaving, quicker than it arrived. */
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

/** The scrim fading in step with its panel. */
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

/** True while the overlay is on screen or still on its way in or out. */
internal fun MutableTransitionState<Boolean>.isOverlayShown(visible: Boolean): Boolean =
    visible || currentState || targetState || !isIdle

/**
 * The feedback of a row or a small button inside an overlay.
 *
 * It shrinks by the skin's press scale, takes [highlight] while hovered or focused, and draws the
 * focus ring on keyboard focus. A disabled target only fades.
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
    val motion = LocalBuilderMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) motion.pressScale else 1f, motion.press())
    val ground = when {
        !enabled -> Color.Transparent
        selected -> style.selected
        hovered || focused -> highlight
        else -> Color.Transparent
    }
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.background(ground, shape)
        .then(if (focused) Modifier.border(FocusRingWidth, focus, shape) else Modifier)
        .then(if (enabled) Modifier else Modifier.alpha(DisabledAlpha))
}
