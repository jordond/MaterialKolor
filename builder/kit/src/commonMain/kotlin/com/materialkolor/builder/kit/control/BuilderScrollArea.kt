package com.materialkolor.builder.kit.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.headless.HeadlessScrollArea
import com.materialkolor.builder.kit.skin.LocalSkin

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
 * @param[content] What scrolls.
 */
@Composable
public fun BuilderScrollArea(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    tabStop: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessScrollArea(state, overlayStyle(LocalSkin.current.library), modifier, tabStop, content)
}
