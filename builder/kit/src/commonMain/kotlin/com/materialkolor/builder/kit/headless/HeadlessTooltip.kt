package com.materialkolor.builder.kit.headless

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.materialkolor.builder.kit.a11y.LocalFocusVisibility
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.isOverlayShown
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.popoverExit
import com.materialkolor.builder.kit.skin.headless.rememberOverlayVisibility
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlin.math.max

/**
 * A short label over [content], shown while the pointer rests on it and while it has keyboard focus.
 *
 * Keyboard focus shows it at once, so nothing it says is only there for a mouse. Focus a click
 * left behind does not, the way the focus ring works. The pointer shows it after a short rest, and
 * only once it has moved over the anchor, so anchors a scroll slides under a still pointer stay
 * quiet. A scroll over the anchor or the label hides it until the pointer moves again. The pointer
 * can move onto the label without it going away (WCAG 1.4.13). Esc or a press on the anchor hides
 * it until the pointer and focus have both left, and it never takes focus itself. Drawn in the
 * page, the label stays out of the semantics tree, since [content] already carries the name, and
 * a wheel turned over it still scrolls the page under it.
 *
 * The label keeps clear of the anchor's focus ring, and far enough off it that its own shadow falls
 * short of the ring too, so a focused anchor rings whole under it.
 *
 * @param[text] The label.
 * @param[style] The skin's overlay style.
 * @param[modifier] Applied to the box around [content].
 * @param[content] The anchor, usually an icon button.
 */
@Composable
internal fun HeadlessTooltip(
    text: String,
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pointer = remember { TooltipPointer() }
    var focused by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    val visibility = LocalFocusVisibility.current
    val focusShows by remember(visibility) { derivedStateOf { focused && visibility.isVisible } }
    LaunchedEffect(hovered, pointer.armed) { pointer.follow(hovered) }
    val wanted = pointer.shows || focusShows
    LaunchedEffect(wanted) { if (!wanted) hidden = false }
    val visible = wanted && !hidden
    Box(
        modifier = modifier
            .hoverable(interaction)
            .onGloballyPositioned { layout -> pointer.placed(layout.positionInRoot()) }
            .onFocusChanged { state -> focused = state.hasFocus }
            .pointerInput(pointer) { watchAnchor(pointer) { hidden = true } }
            .onKeyEvent { event ->
                val escape = visible && event.type == KeyEventType.KeyDown && event.key == Key.Escape
                if (escape) hidden = true
                escape
            },
    ) {
        content()
        TooltipPopup(visible, text, style, interaction, pointer)
    }
}

/**
 * What the pointer has done over a tooltip's anchor and label, beyond plain hover.
 *
 * Hover alone does not show the label. The pointer has to move over the anchor or the label
 * first, and an anchor that content carried under a still pointer has not seen a move.
 */
private class TooltipPointer {
    /**
     * True once the pointer has moved over the anchor or the label, until it scrolls there or leaves.
     */
    var armed: Boolean by mutableStateOf(false)
        private set

    /**
     * Whether the pointer shows the label.
     */
    var shows: Boolean by mutableStateOf(false)
        private set

    /**
     * Goes up each time a scroll takes the label away, so it leaves at once instead of fading out
     * under the pointer.
     */
    var drops: Int by mutableIntStateOf(0)
        private set

    private var origin: Offset? = null
    private var carried = false

    /**
     * Notes where the anchor sits. Once it has moved, the next pointer event over it came from the
     * content moving rather than the pointer.
     */
    fun placed(position: Offset) {
        val last = origin
        if (last != null && last != position) carried = true
        origin = position
    }

    fun moved() {
        if (carried) carried = false else armed = true
    }

    fun left() {
        armed = false
    }

    fun scrolled() {
        armed = false
        if (shows) {
            shows = false
            drops++
        }
    }

    /**
     * Shows the label once the pointer has rested on the anchor, and hides it a moment after the
     * pointer leaves, which leaves time to cross onto the label.
     */
    suspend fun follow(hovered: Boolean) {
        if (!hovered) {
            awaitFrameTime(OverlayMetrics.tooltipGraceMillis)
            shows = false
        } else if (armed && !shows) {
            awaitFrameTime(OverlayMetrics.tooltipDelayMillis)
            shows = true
        }
    }
}

/**
 * Waits [millis] on the frame clock, the one the page's animations run on, so a test's clock
 * drives the wait the way it drives them.
 */
private suspend fun awaitFrameTime(millis: Long) {
    val start = withFrameMillis { time -> time }
    do {
        val now = withFrameMillis { time -> time }
    } while (now - start < millis)
}

/**
 * Follows the pointer over the anchor, before the anchor sees it. A press calls [hide].
 */
private suspend fun PointerInputScope.watchAnchor(
    pointer: TooltipPointer,
    hide: () -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            when (awaitPointerEvent(PointerEventPass.Initial).type) {
                PointerEventType.Press -> hide()
                PointerEventType.Scroll -> pointer.scrolled()
                PointerEventType.Enter, PointerEventType.Move -> pointer.moved()
                PointerEventType.Exit -> pointer.left()
            }
        }
    }
}

/**
 * Follows the pointer over the label. A press on the label stops there, so it lands on nothing
 * under it, while a scroll goes on to the page.
 */
private suspend fun PointerInputScope.watchLabel(pointer: TooltipPointer) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            when (event.type) {
                PointerEventType.Press -> event.changes.forEach { change -> change.consume() }
                PointerEventType.Scroll -> pointer.scrolled()
                PointerEventType.Enter, PointerEventType.Move -> pointer.moved()
                PointerEventType.Exit -> pointer.left()
            }
        }
    }
}

@Composable
private fun TooltipPopup(
    visible: Boolean,
    text: String,
    style: OverlayStyle,
    interaction: MutableInteractionSource,
    pointer: TooltipPointer,
) {
    val anchor = if (LocalOverlaysInTree.current) rememberOverlayAnchor() else null
    val state = key(pointer.drops) { rememberOverlayVisibility(visible) }
    if (!state.isOverlayShown(visible)) return
    val host = inTreeOverlayHost()
    val tokens = LocalBuilderTokens.current
    val clearance = maxOf(tokens.spacing.extraSmall, FocusRingOffset + FocusRingWidth + style.shadow)
    val gap = with(LocalDensity.current) { clearance.roundToPx() }
    val provider = remember(gap) { TooltipPositionProvider(gap) }
    // In the page the anchor already carries the name, so the bubble stays out of the semantics tree.
    val quiet = if (host != null) Modifier.clearAndSetSemantics {} else Modifier
    val bubble: @Composable () -> Unit = {
        AnimatedVisibility(visibleState = state, enter = popoverEnter(), exit = popoverExit()) {
            Box(
                modifier = Modifier
                    .hoverable(interaction)
                    .pointerInput(pointer) { watchLabel(pointer) }
                    .then(quiet)
                    .padding(tokens.spacing.extraSmall)
                    .widthIn(max = OverlayMetrics.tooltipMaxWidth)
                    .shadow(style.shadow, style.popoverShape)
                    .clip(style.popoverShape)
                    .background(style.tooltip)
                    .then(
                        if (style.tooltipBorder != null) {
                            Modifier.border(style.tooltipBorder, style.popoverShape)
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = tokens.spacing.small, vertical = tokens.spacing.extraSmall),
            ) {
                BuilderText(text, style = BuilderTextStyle.Label, color = style.tooltipContent)
            }
        }
    }
    if (host != null && anchor != null) {
        val layer = remember { OverlayLayer(OverlayKind.Passive) }
        val placement = remember(anchor, provider) { OverlayPlacement(anchor, provider) }
        OverlayPortal(host, layer, visible, placement, content = bubble)
    } else {
        Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false), content = bubble)
    }
}

/**
 * Centred above the anchor, or below it when the window runs out above.
 */
internal class TooltipPositionProvider(
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centred = anchorBounds.center.x - popupContentSize.width / 2
        val x = centred.coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.bottom + gap
        return IntOffset(x, y)
    }
}
