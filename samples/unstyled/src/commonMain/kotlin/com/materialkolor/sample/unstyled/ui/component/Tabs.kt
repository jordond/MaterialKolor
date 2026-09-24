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
import androidx.compose.foundation.layout.height
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
                for (choice in choices) {
                    PillTab(choice)
                }
            }
        }
    }
}

@Composable
private fun <T> TabListScope<T>.UnderlineTab(choice: Choice<T>) {
    val interactionSource = remember { MutableInteractionSource() }
    Tab(
        key = choice.value,
        indication = LocalIndication.current,
        interactionSource = interactionSource,
        modifier = Modifier
            .controlFocusRing(interactionSource, Shapes.Tab, offset = 0.dp)
            .clip(Shapes.Tab),
    ) {
        val primary = MaterialKolorTokens.primary.color
        val content = if (selected) primary else MaterialKolorTokens.onSurfaceVariant.color
        val indicator by animateColorAsState(
            targetValue = if (selected) primary else Color.Transparent,
            label = "indicator",
        )

        ProvideContentColor(content) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier
                    .drawBehind {
                        val height = 3.dp.toPx()
                        drawRect(
                            color = indicator,
                            topLeft = Offset(x = 0f, y = size.height - height),
                            size = Size(width = size.width, height = height),
                        )
                    }.height(44.dp)
                    .padding(horizontal = Spacing.Large),
            ) {
                if (choice.icon != null) {
                    UnstyledIcon(
                        imageVector = choice.icon,
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
        indication = LocalIndication.current,
        interactionSource = interactionSource,
        modifier = Modifier
            .controlFocusRing(interactionSource, Shapes.Pill)
            .clip(Shapes.Pill),
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.Tight),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(container, Shapes.Pill)
                    .border(width = 1.dp, color = outline, shape = Shapes.Pill)
                    .height(32.dp)
                    .padding(horizontal = Spacing.Large),
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
