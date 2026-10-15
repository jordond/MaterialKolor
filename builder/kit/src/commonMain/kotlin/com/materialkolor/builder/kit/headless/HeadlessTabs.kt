package com.materialkolor.builder.kit.headless

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.composeunstyled.Tab
import com.composeunstyled.TabList
import com.composeunstyled.UnstyledTabGroup
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import com.materialkolor.builder.kit.skin.headless.controlRing

/**
 * How [HeadlessTabs] draws its row of tabs.
 *
 * @property[container] The fill behind the whole row.
 * @property[containerShape] The row's corners.
 * @property[containerPadding] Between the row's edge and the tabs. At the ends the row keeps at least
 * the focus ring's reach, so the first and last tab ring whole.
 * @property[tabShape] Each tab's corners.
 * @property[tabPadding] Between a tab's edge and its label.
 * @property[gap] Between tabs.
 * @property[selectedContainer] The fill behind the selected tab.
 * @property[selectedInk] The selected tab's label.
 * @property[ink] The other tabs' labels.
 * @property[indicator] The line under the selected tab.
 * @property[indicatorHeight] How thick that line is, zero for none.
 * @property[indicatorWidth] How wide that line is, or null to run the tab's full width.
 * @property[focus] The keyboard focus ring.
 */
@Immutable
internal class TabsStyle(
    val container: Color,
    val containerShape: Shape,
    val containerPadding: Dp,
    val tabShape: Shape,
    val tabPadding: PaddingValues,
    val gap: Dp,
    val selectedContainer: Color,
    val selectedInk: Color,
    val ink: Color,
    val indicator: Color,
    val indicatorHeight: Dp,
    val indicatorWidth: Dp?,
    val focus: Color,
)

/**
 * A row of tabs over Compose Unstyled's tab group, scrolling sideways when the tabs do not fit.
 *
 * Focus roves. Only the selected tab sits in the Tab order, the arrow keys move focus along the row
 * in reading order and wrap at the ends, Home and End jump, and focusing a tab selects it and
 * scrolls it into view. Each tab has the tab role and its selected state.
 *
 * The row clips where it scrolls, so its ends sit inside the scroll and keep the focus ring's reach
 * free. The first and last tab then ring on every side, as the others do.
 */
@Composable
internal fun <T> HeadlessTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    style: TabsStyle,
    modifier: Modifier = Modifier,
) {
    val target = LocalLayout.current.primaryTouchTarget
    val requesters = remember(tabs.size) { List(tabs.size) { FocusRequester() } }
    val focused = remember { mutableIntStateOf(tabs.indexOf(selected)) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    UnstyledTabGroup(
        selectedTab = selected,
        onSelectedTabChange = onSelect,
        tabs = tabs,
        modifier = modifier,
    ) {
        TabList(
            modifier = Modifier
                .tabArrows(tabs.size, focused, requesters, isRtl)
                .background(style.container, style.containerShape)
                .padding(vertical = style.containerPadding),
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = maxOf(style.containerPadding, TabRingReach)),
                horizontalArrangement = Arrangement.spacedBy(style.gap),
            ) {
                tabs.forEachIndexed { index, tab ->
                    key(tab) {
                        val interactions = remember { MutableInteractionSource() }
                        val isSelected = tab == selected
                        Tab(
                            key = tab,
                            modifier = Modifier
                                .focusRequester(requesters[index])
                                .onFocusChanged { state -> if (state.isFocused) focused.intValue = index }
                                .heightIn(min = target)
                                .controlRing(interactions, style.tabShape, style.focus)
                                .tabFill(isSelected, style)
                                .foldState(label(tab), ControlState.Selected(isSelected), role = FoldedRole.Tab),
                            interactionSource = interactions,
                            contentAlignment = Alignment.Center,
                        ) {
                            TabLabel(label(tab), isSelected, style)
                        }
                    }
                }
            }
        }
    }
}

/**
 * How far a tab's focus ring reaches past the tab, which the ends of the row keep free.
 */
private val TabRingReach: Dp = FocusRingOffset + FocusRingWidth

/**
 * Left and Right, read before Compose Unstyled's tab list sees them, which maps Left to the
 * previous tab even right to left. Here Right moves toward the end of the row as it reads, and
 * both wrap at the ends.
 */
private fun Modifier.tabArrows(
    count: Int,
    focused: MutableIntState,
    requesters: List<FocusRequester>,
    isRtl: Boolean,
): Modifier =
    onPreviewKeyEvent { event ->
        val target = rovingTarget(event.key, focused.intValue, count, isRtl, upDown = false, homeEnd = false)
            ?: return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyDown) requesters[target].requestFocus()
        true
    }

@Composable
private fun Modifier.tabFill(
    selected: Boolean,
    style: TabsStyle,
): Modifier {
    val motion = LocalBuilderMotion.current
    val fill by animateColorAsState(if (selected) style.selectedContainer else Color.Transparent, motion.effects())
    return background(fill, style.tabShape)
        .drawBehind {
            if (!selected || style.indicatorHeight.value <= 0f) return@drawBehind
            val height = style.indicatorHeight.toPx()
            val width = style.indicatorWidth?.toPx()?.coerceAtMost(size.width) ?: size.width
            drawRect(
                color = style.indicator,
                topLeft = Offset((size.width - width) / 2, size.height - height),
                size = Size(width, height),
            )
        }
}

@Composable
private fun TabLabel(
    text: String,
    selected: Boolean,
    style: TabsStyle,
) {
    val motion = LocalBuilderMotion.current
    val ink by animateColorAsState(if (selected) style.selectedInk else style.ink, motion.effects())
    BuilderText(
        text = text,
        modifier = Modifier.padding(style.tabPadding),
        style = BuilderTextStyle.Label,
        color = ink,
        maxLines = 1,
    )
}
