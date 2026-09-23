package com.materialkolor.builder.preview.split

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import com.materialkolor.builder.preview.canvas.PreviewPane

/**
 * Two copies of one screen, the end copy wiped over the start copy up to a draggable handle.
 *
 * Only the end copy's layer reads [split], so dragging the handle moves a clip and a handle and
 * recomposes neither copy. Hit testing follows the clip, so each side takes pointer input where it
 * shows. The end copy is hidden from assistive tech and refuses keyboard focus, so a screen reader
 * and the Tab key find one app, the start copy.
 *
 * @param[start] The copy drawn from the start edge, the one assistive tech reads.
 * @param[end] The copy past the handle.
 * @param[split] Where the handle sits.
 * @param[modifier] Applied to the preview as a whole. Size it here, both copies fill it.
 * @param[orientation] Horizontal to split side by side, vertical to stack the copies on narrow panes.
 * @param[screen] What both copies show, called once per copy with that copy's spec.
 */
@Composable
public fun SplitPreview(
    start: PaneSpec,
    end: PaneSpec,
    split: SplitState,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    screen: @Composable (spec: PaneSpec) -> Unit,
) {
    // Written by layout and read only in placement, draw and gesture callbacks.
    val size = remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { measured -> size.value = measured },
        propagateMinConstraints = true,
    ) {
        PreviewPane(start) { screen(start) }
        CompositionLocalProvider(LocalPaneSide provides PaneSide.End) {
            PreviewPane(
                spec = end,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        clip = true
                        shape = SplitShape(split.fraction, orientation)
                    }.clearAndSetSemantics { }
                    .focusProperties { onEnter = { cancelFocusChange() } }
                    .focusGroup(),
            ) { screen(end) }
        }
        SplitHandle(
            split = split,
            orientation = orientation,
            startLabel = start.label,
            size = { size.value },
            modifier = Modifier.matchParentSize(),
        )
    }
}
