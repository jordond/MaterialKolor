package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Tab
import com.composeunstyled.TabList
import com.composeunstyled.TabListScope
import com.composeunstyled.Text
import com.composeunstyled.UnstyledTabGroup
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShadowTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.brush
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shadow
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.unstyled.MaterialKolorTokens

private const val BADGE_ALPHA = 0.2f

/**
 * Tabs with a glowing accent thumb that slides to the selected tab.
 */
@Composable
internal fun <T> AccentTabs(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = ShapeTokens.pill.shape
    val thumb = rememberSlidingThumbState()

    UnstyledTabGroup(
        selectedTab = selected,
        onSelectedTabChange = onSelect,
        tabs = choices.map { choice -> choice.value },
        modifier = modifier,
    ) {
        TabList {
            Box {
                SlidingThumb(
                    state = thumb,
                    selected = selected,
                    modifier = Modifier
                        .dropShadow(shape = pill, shadow = ShadowTokens.accent.shadow)
                        .clip(pill)
                        .background(GradientTokens.accent.brush),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    for (choice in choices) {
                        AccentTab(choice = choice, modifier = Modifier.thumbSlot(thumb, choice.value))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> TabListScope<T>.AccentTab(
    choice: Choice<T>,
    modifier: Modifier,
) {
    val pill = ShapeTokens.pill.shape
    val interactionSource = remember { MutableInteractionSource() }

    Tab(
        key = choice.value,
        indication = LocalIndication.current,
        interactionSource = interactionSource,
        modifier = modifier
            .controlFocusRing(interactionSource, pill)
            .clip(pill),
    ) {
        val content by animateColorAsState(
            targetValue = if (selected) {
                MaterialKolorTokens.onPrimary.color
            } else {
                MaterialKolorTokens.onSurfaceVariant.color
            },
            label = "tab",
        )
        val badge by animateColorAsState(
            targetValue = if (selected) {
                MaterialKolorTokens.onPrimary.color.copy(alpha = BADGE_ALPHA)
            } else {
                MaterialKolorTokens.surfaceContainerHighest.color
            },
            label = "badge",
        )

        ProvideContentColor(content) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
                    .padding(start = Spacing.Large, end = if (choice.badge == null) Spacing.Large else Spacing.Tight),
            ) {
                Text(text = choice.label, style = TasksType.Label, maxLines = 1)

                if (choice.badge != null) {
                    Text(
                        text = choice.badge,
                        style = TasksType.Small,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .widthIn(min = 24.dp)
                            .background(badge, pill)
                            .padding(horizontal = Spacing.Tight, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
