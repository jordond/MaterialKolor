package com.materialkolor.builder.kit.headless

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.composeunstyled.Tab
import com.composeunstyled.TabList
import com.composeunstyled.UnstyledTabGroup
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.inputFocusRing

/**
 * How [HeadlessTabs] draws its row of tabs.
 *
 * @property[container] The fill behind the whole row.
 * @property[containerShape] The row's corners.
 * @property[containerPadding] Between the row's edge and the tabs.
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
 * A row of tabs over Compose Unstyled's tab group.
 *
 * Focus roves. Only the selected tab sits in the Tab order, the arrow keys move focus along the row
 * and wrap at the ends, Home and End jump, and focusing a tab selects it. Each tab has the tab role
 * and its selected state.
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
    UnstyledTabGroup(
        selectedTab = selected,
        onSelectedTabChange = onSelect,
        tabs = tabs,
        modifier = modifier,
    ) {
        TabList(
            modifier = Modifier
                .background(style.container, style.containerShape)
                .padding(style.containerPadding),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(style.gap)) {
                for (tab in tabs) {
                    key(tab) {
                        val interactions = remember { MutableInteractionSource() }
                        val isSelected = tab == selected
                        Tab(
                            key = tab,
                            modifier = Modifier
                                .heightIn(min = target)
                                .inputFocusRing(interactions, style.focus, style.tabShape)
                                .tabFill(isSelected, style),
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
