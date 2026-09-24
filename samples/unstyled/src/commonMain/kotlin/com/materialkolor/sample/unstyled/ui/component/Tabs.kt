package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Tab
import com.composeunstyled.TabList
import com.composeunstyled.TabListScope
import com.composeunstyled.Text
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledTabGroup
import com.materialkolor.sample.unstyled.theme.IconSize
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private const val BADGE_ALPHA = 0.72f

/**
 * Tabs over a line, the picked one in primary with an indicator on the line. Arrow keys move between them.
 *
 * @param[choices] The tabs, in order.
 * @param[selected] The picked value.
 * @param[onSelect] Called with the value of a tab when it is picked.
 * @param[modifier] Applied to the tab group.
 */
@Composable
internal fun <T> UnderlineTabs(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnstyledTabGroup(
        selectedTab = selected,
        onSelectedTabChange = onSelect,
        tabs = choices.map { choice -> choice.value },
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth()) {
            TabList {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    for (choice in choices) UnderlineTab(choice)
                }
            }
            UnstyledHorizontalSeparator(color = MaterialKolorTokens.outlineVariant.color)
        }
    }
}

/**
 * Tabs drawn as pills, the picked one filled. Arrow keys move between them.
 *
 * @param[choices] The tabs, in order. A badge shows after the label.
 * @param[selected] The picked value.
 * @param[onSelect] Called with the value of a tab when it is picked.
 * @param[modifier] Applied to the tab group.
 */
@Composable
internal fun <T> PillTabs(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnstyledTabGroup(
        selectedTab = selected,
        onSelectedTabChange = onSelect,
        tabs = choices.map { choice -> choice.value },
        modifier = modifier,
    ) {
        TabList {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                for (choice in choices) PillTab(choice)
            }
        }
    }
}

@Composable
private fun <T> TabListScope<T>.UnderlineTab(choice: Choice<T>) {
    val interactionSource = remember { MutableInteractionSource() }
    Tab(
        key = choice.value,
        modifier = Modifier
            .testTag(choice.testTag)
            .controlFocusRing(interactionSource, Shapes.Tab, offset = 0.dp)
            .clip(Shapes.Tab),
        indication = LocalIndication.current,
        interactionSource = interactionSource,
    ) {
        val primary = MaterialKolorTokens.primary.color
        val indicator by animateColorAsState(
            targetValue = if (selected) primary else Color.Transparent,
            label = "indicator",
        )
        val content = if (selected) primary else MaterialKolorTokens.onSurfaceVariant.color
        ProvideContentColor(content) {
            Row(
                modifier = Modifier
                    .drawBehind {
                        val height = 3.dp.toPx()
                        drawRect(
                            color = indicator,
                            topLeft = Offset(x = 0f, y = size.height - height),
                            size = Size(width = size.width, height = height),
                        )
                    }.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = choice.icon
                if (icon != null) {
                    UnstyledIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize),
                        tint = content,
                    )
                }
                Text(text = choice.label, style = TasksType.Label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun <T> TabListScope<T>.PillTab(choice: Choice<T>) {
    val interactionSource = remember { MutableInteractionSource() }
    Tab(
        key = choice.value,
        modifier = Modifier
            .testTag(choice.testTag)
            .controlFocusRing(interactionSource, Shapes.Pill)
            .clip(Shapes.Pill),
        indication = LocalIndication.current,
        interactionSource = interactionSource,
    ) {
        val container by animateColorAsState(
            targetValue = if (selected) MaterialKolorTokens.secondaryContainer.color else Color.Transparent,
            label = "pill",
        )
        val outline = if (selected) Color.Transparent else MaterialKolorTokens.outlineVariant.color
        val content = if (selected) {
            MaterialKolorTokens.onSecondaryContainer.color
        } else {
            MaterialKolorTokens.onSurfaceVariant.color
        }
        ProvideContentColor(content) {
            Row(
                modifier = Modifier
                    .background(container, Shapes.Pill)
                    .border(width = 1.dp, color = outline, shape = Shapes.Pill)
                    .padding(horizontal = Spacing.Large, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = choice.label, style = TasksType.Label, maxLines = 1)
                val badge = choice.badge
                if (badge != null) {
                    Text(
                        text = badge,
                        style = TasksType.Label,
                        color = content.copy(alpha = BADGE_ALPHA),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
