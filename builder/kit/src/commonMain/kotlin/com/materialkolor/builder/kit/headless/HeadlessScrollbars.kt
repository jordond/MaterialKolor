package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.Thumb
import com.composeunstyled.ThumbVisibility
import com.composeunstyled.UnstyledVerticalScrollbar
import com.composeunstyled.rememberScrollbarState
import com.materialkolor.builder.kit.skin.headless.OverlayStyle

/**
 * A column that scrolls, with a thumb that can be dragged and a track that pages on a click.
 *
 * The thumb stays on screen rather than fading while idle, so how much is left to read never
 * depends on moving the pointer first.
 *
 * @param[state] The scroll position, hoisted so a caller can jump to a section.
 * @param[style] The skin's overlay style, for the thumb.
 * @param[modifier] Applied to the area.
 * @param[content] What scrolls.
 */
@Composable
internal fun HeadlessScrollArea(
    state: ScrollState,
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state)
                .padding(end = ThumbThickness + ThumbInset * 2),
            content = content,
        )
        HeadlessVerticalScrollbar(state, style, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
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
            .padding(ThumbInset)
            .width(ThumbThickness),
    ) {
        Thumb(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.thumb, RoundedCornerShape(ThumbThickness / 2)),
            thumbVisibility = ThumbVisibility.AlwaysVisible,
        )
    }
}

private val ThumbThickness = 6.dp
private val ThumbInset = 2.dp
