package com.materialkolor.sample.unstyled.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleDashed
import com.composables.icons.lucide.ClipboardList
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.Trash2
import com.composeunstyled.Text
import com.composeunstyled.UnstyledIcon
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShadowTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.brush
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shadow
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.sample.unstyled.ui.component.Button
import com.materialkolor.sample.unstyled.ui.component.ButtonStyle
import com.materialkolor.sample.unstyled.ui.component.Card
import com.materialkolor.sample.unstyled.ui.component.Checkbox
import com.materialkolor.sample.unstyled.ui.component.IconButton
import com.materialkolor.sample.unstyled.ui.component.Tag
import com.materialkolor.unstyled.MaterialKolorTokens

private val LiftDistance = 2.dp

@Composable
internal fun TaskList(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        modifier = modifier.fillMaxWidth(),
    ) {
        val tasks = state.visibleTasks
        if (tasks.isEmpty()) {
            EmptyState(filter = state.filter)
        } else {
            for (task in tasks) {
                key(task.id) {
                    TaskTile(
                        task = task,
                        onToggle = { dispatch(SampleAction.ToggleTask(task.id)) },
                        onDelete = { dispatch(SampleAction.DeleteTask(task.id)) },
                    )
                }
            }
        }

        Footer(
            remaining = state.activeCount,
            canClearDone = state.canClearDone,
            onClearDone = { dispatch(SampleAction.RequestClearDone) },
        )
    }
}

@Composable
private fun TaskTile(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val lift by animateFloatAsState(targetValue = if (hovered) 1f else 0f, label = "lift")
    val resting = ShadowTokens.resting.shadow
    val shadow = lerp(resting, ShadowTokens.lifted.shadow, lift) ?: resting

    Card(
        shadow = shadow,
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .graphicsLayer { translationY = -LiftDistance.toPx() * lift },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(start = Spacing.Small, end = Spacing.Medium),
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })

            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                color = if (task.isDone) MaterialKolorTokens.onSurfaceVariant.color else Color.Unspecified,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            )

            Tag(label = SampleCopy.label(task.tag), colors = task.tag.colors())

            IconButton(icon = Lucide.Trash2, onClick = onDelete)
        }
    }
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    val round = ShapeTokens.pill.shape
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large, vertical = Spacing.XXLarge),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .dropShadow(shape = round, shadow = ShadowTokens.accent.shadow)
                    .background(GradientTokens.accent.brush, round),
            ) {
                UnstyledIcon(
                    imageVector = filter.emptyIcon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialKolorTokens.onPrimary.color,
                )
            }

            Text(
                text = SampleCopy.empty(filter),
                color = MaterialKolorTokens.onSurfaceVariant.color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Footer(
    remaining: Int,
    canClearDone: Boolean,
    onClearDone: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.Large),
    ) {
        Text(
            text = SampleCopy.remaining(remaining),
            modifier = Modifier.weight(1f),
            style = TasksType.Caption,
            color = MaterialKolorTokens.onSurfaceVariant.color,
        )

        Button(
            label = SampleCopy.clearDone,
            onClick = onClearDone,
            style = ButtonStyle.Quiet,
            enabled = canClearDone,
        )
    }
}

private val TaskFilter.emptyIcon: ImageVector
    get() = when (this) {
        TaskFilter.All -> Lucide.ClipboardList
        TaskFilter.Active -> Lucide.PartyPopper
        TaskFilter.Done -> Lucide.CircleDashed
    }
