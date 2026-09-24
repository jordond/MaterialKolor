package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.Thumb
import com.composeunstyled.UnstyledVerticalScrollbar
import com.composeunstyled.rememberScrollbarState
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private const val THUMB_ALPHA = 0.32f
private const val THUMB_ACTIVE_ALPHA = 0.6f

/**
 * A thin scrollbar for [scrollState] that darkens under a pointer and while dragged. It hides while everything fits.
 *
 * @param[scrollState] The scroll it shows and drives.
 * @param[modifier] Applied to the track. Give it the height to span.
 */
@Composable
internal fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    // Before the first layout the maximum is unknown and reads as Int.MAX_VALUE.
    val scrolls = scrollState.maxValue in 1 until Int.MAX_VALUE
    if (!scrolls) return

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (hovered || dragged) THUMB_ACTIVE_ALPHA else THUMB_ALPHA,
        label = "thumb",
    )

    UnstyledVerticalScrollbar(
        scrollbarState = rememberScrollbarState(scrollState),
        modifier = modifier
            .width(Spacing.Medium)
            .padding(vertical = Spacing.XSmall),
        interactionSource = interactionSource,
    ) {
        Thumb(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp)
                .clip(Shapes.Pill)
                .background(MaterialKolorTokens.onSurfaceVariant.color.copy(alpha = alpha)),
        )
    }
}
