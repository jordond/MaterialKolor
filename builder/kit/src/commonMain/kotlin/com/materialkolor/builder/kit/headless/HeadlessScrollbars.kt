package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import com.composeunstyled.Thumb
import com.composeunstyled.ThumbVisibility
import com.composeunstyled.UnstyledVerticalScrollbar
import com.composeunstyled.rememberScrollbarState
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A column that scrolls, with a thumb that can be dragged and a track that pages on a click.
 *
 * The thumb stays on screen rather than fading while idle, so how much is left to read never
 * depends on moving the pointer first. On the web the area is also a Tab stop while it has more
 * to show than fits, with the kit's focus ring, and the arrows and Page Up and Page Down scroll it
 * (S5 row 22).
 *
 * @param[state] The scroll position, hoisted so a caller can jump to a section.
 * @param[style] The skin's overlay style, for the thumb and the focus ring.
 * @param[modifier] Applied to the area.
 * @param[tabStop] Whether the area may be a Tab stop on the web at all.
 * @param[fitContent] Whether the area is only as tall as what it holds, up to the height it may
 * take, rather than all of that height. The scrollbar then takes the area's height instead of
 * setting it.
 * @param[content] What scrolls.
 */
@Composable
internal fun HeadlessScrollArea(
    state: ScrollState,
    style: OverlayStyle,
    modifier: Modifier,
    tabStop: Boolean = true,
    fitContent: Boolean = false, // b-511
    content: @Composable ColumnScope.() -> Unit,
) {
    val keys = if (tabStop && LocalWebKeyboard.current) Modifier.scrollAreaKeys(state, style) else Modifier
    Box(modifier.then(keys)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state)
                .padding(end = OverlayMetrics.thumbThickness + OverlayMetrics.thumbInset * 2),
            content = content,
        )
        if (fitContent) {
            // b-511
            // Sized from the column, so the scrollbar never makes the area taller than what it holds.
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                HeadlessVerticalScrollbar(state, style, Modifier.fillMaxHeight())
            }
        } else {
            HeadlessVerticalScrollbar(state, style, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

/**
 * Makes a scroll area a Tab stop while [state] has more to show than fits, ringed in the style's
 * focus ink. While the area itself has focus an arrow scrolls a step, and Page Up and Page Down a
 * screen less that step, so a line of context stays in view. Keys a control inside it lets go of
 * are left alone.
 */
@Composable
private fun Modifier.scrollAreaKeys(
    state: ScrollState,
    style: OverlayStyle,
): Modifier {
    val interactions = remember { MutableInteractionSource() }
    val overflows by remember(state) { derivedStateOf { state.maxValue in 1 until Int.MAX_VALUE } }
    var focused by remember { mutableStateOf(false) }
    val section = LocalBuilderTokens.current.spacing.section
    val step = with(LocalDensity.current) { section.toPx() }
    return controlRing(interactions, RectangleShape, style.focus)
        .onFocusChanged { focus -> focused = focus.isFocused }
        .onKeyEvent { event ->
            if (!focused || event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val page = (state.viewportSize - step).coerceAtLeast(step)
            val delta = when (event.key) {
                Key.DirectionUp -> -step
                Key.DirectionDown -> step
                Key.PageUp -> -page
                Key.PageDown -> page
                else -> return@onKeyEvent false
            }
            state.dispatchRawDelta(delta)
            true
        }.focusable(enabled = overflows, interactionSource = interactions)
}

/** A vertical scrollbar for [state], drawn as a rounded thumb in the skin's thumb ink. */
@Composable
internal fun HeadlessVerticalScrollbar(
    state: ScrollState,
    style: OverlayStyle,
    modifier: Modifier = Modifier,
) {
    UnstyledVerticalScrollbar(
        scrollbarState = rememberScrollbarState(state),
        modifier = modifier
            .padding(OverlayMetrics.thumbInset)
            .width(OverlayMetrics.thumbThickness),
    ) {
        Thumb(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.thumb, RoundedCornerShape(OverlayMetrics.thumbThickness / 2)),
            thumbVisibility = ThumbVisibility.AlwaysVisible,
        )
    }
}
