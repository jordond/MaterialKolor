package com.materialkolor.builder.kit.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.headless.HeadlessScrollArea
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics

/**
 * A column that scrolls, with a scrollbar that stays on screen and can be dragged.
 *
 * No library has a scrollbar every target shares, so every skin draws the headless one in its own
 * thumb colour. On the web the area takes Tab while its content overflows, shows the focus ring and
 * scrolls with the arrows and Page Up and Page Down.
 *
 * @param[modifier] Applied to the area. Give it a bounded height.
 * @param[state] The scroll position, hoisted so a caller can jump to a section.
 * @param[tabStop] Whether the area may take Tab on the web. Pass false when what scrolls already
 * holds a control that takes focus, the way Chromium leaves such a scroller out of the Tab order.
 * Focus moving into the content scrolls the area to it.
 * @param[fitContent] Whether the area is only as tall as what it holds, up to the height it may
 * take. By default it takes all of that height, however little it holds.
 * @param[scrollbarInGutter] Whether the scrollbar hangs past the area's end edge, in room the
 * caller keeps there such as a panel's padding, so what scrolls lines up with what sits above and
 * below the area rather than stopping short of the scrollbar.
 * @param[content] What scrolls.
 */
@Composable
public fun BuilderScrollArea(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    tabStop: Boolean = true,
    fitContent: Boolean = false,
    scrollbarInGutter: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val area = if (scrollbarInGutter) modifier.bleedEnd(ScrollbarRoom) else modifier
    HeadlessScrollArea(state, overlayStyle(LocalSkin.current.library), area, tabStop, fitContent, content)
}

/**
 * How much room the scrollbar keeps at the end of the area, its thumb and the gap on either side.
 */
private val ScrollbarRoom: Dp = OverlayMetrics.thumbThickness + OverlayMetrics.thumbInset * 2

/**
 * Lays the area out [extra] wider than it is given, the extra past its end edge, while it still
 * takes only the width it is given in its parent.
 */
private fun Modifier.bleedEnd(extra: Dp): Modifier =
    layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        val room = extra.roundToPx()
        val wider = constraints.copy(
            minWidth = constraints.minWidth + room,
            maxWidth = constraints.maxWidth + room,
        )
        val placeable = measurable.measure(wider)
        layout(placeable.width - room, placeable.height) { placeable.placeRelative(0, 0) }
    }
