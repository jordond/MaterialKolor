package com.materialkolor.sample.unstyled.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleDashed
import com.composables.icons.lucide.ClipboardList
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.Trash2
import com.composeunstyled.Text
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.ui.component.Button
import com.materialkolor.sample.unstyled.ui.component.ButtonStyle
import com.materialkolor.sample.unstyled.ui.component.Card
import com.materialkolor.sample.unstyled.ui.component.Checkbox
import com.materialkolor.sample.unstyled.ui.component.Chip
import com.materialkolor.sample.unstyled.ui.component.IconButton
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * The card that holds the tasks the filter lets through, or the empty state, over a footer with the count left and
 * Clear done.
 */
@Composable
internal fun TaskList(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val divider = MaterialKolorTokens.outlineVariant.color
    Card(
        modifier = modifier
            .testTag(SampleTags.TaskList)
            .fillMaxWidth(),
    ) {
        val tasks = state.visibleTasks
        if (tasks.isEmpty()) {
            EmptyState(filter = state.filter)
        } else {
            tasks.forEachIndexed { index, task ->
                key(task.id) {
                    if (index > 0) UnstyledHorizontalSeparator(color = divider)
                    TaskRow(
                        task = task,
                        onToggle = { dispatch(SampleAction.ToggleTask(task.id)) },
                        onDelete = { dispatch(SampleAction.DeleteTask(task.id)) },
                    )
                }
            }
        }
        UnstyledHorizontalSeparator(color = divider)
        Footer(
            remaining = state.activeCount,
            canClearDone = state.canClearDone,
            onClearDone = { dispatch(SampleAction.RequestClearDone) },
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .testTag(SampleTags.taskRow(task.id))
            .fillMaxWidth()
            .hoverable(interactionSource)
            .background(if (hovered) MaterialKolorTokens.surfaceContainer.color else Color.Transparent)
            .heightIn(min = 52.dp)
            .padding(horizontal = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle() },
            accessibilityLabel = task.title,
            modifier = Modifier.testTag(SampleTags.taskCheck(task.id)),
        )
        Text(
            text = task.title,
            modifier = Modifier
                .testTag(SampleTags.taskTitle(task.id))
                .weight(1f),
            color = if (task.isDone) MaterialKolorTokens.onSurfaceVariant.color else Color.Unspecified,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
        )
        Chip(
            label = SampleCopy.label(task.tag),
            colors = task.tag.chipColors(),
            modifier = Modifier.testTag(SampleTags.taskTag(task.id)),
        )
        IconButton(
            icon = Lucide.Trash2,
            contentDescription = SampleCopy.deleteTask(task.title),
            onClick = onDelete,
            modifier = Modifier.testTag(SampleTags.taskDelete(task.id)),
        )
    }
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    Column(
        modifier = Modifier
            .testTag(SampleTags.EmptyState)
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large, vertical = Spacing.XXLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(Shapes.Round)
                .background(MaterialKolorTokens.surfaceContainerHighest.color),
            contentAlignment = Alignment.Center,
        ) {
            UnstyledIcon(
                imageVector = filter.emptyIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialKolorTokens.onSurfaceVariant.color,
            )
        }
        Text(
            text = SampleCopy.empty(filter),
            color = MaterialKolorTokens.onSurfaceVariant.color,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Footer(
    remaining: Int,
    canClearDone: Boolean,
    onClearDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.Large, end = Spacing.Small, top = Spacing.Small, bottom = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = SampleCopy.remaining(remaining),
            modifier = Modifier
                .testTag(SampleTags.Remaining)
                .weight(1f),
            style = TasksType.Caption,
            color = MaterialKolorTokens.onSurfaceVariant.color,
        )
        Button(
            label = SampleCopy.clearDone,
            onClick = onClearDone,
            modifier = Modifier.testTag(SampleTags.ClearDone),
            style = ButtonStyle.Quiet,
            enabled = canClearDone,
        )
    }
}

/** What the empty state shows for this filter. */
private val TaskFilter.emptyIcon: ImageVector
    get() = when (this) {
        TaskFilter.All -> Lucide.ClipboardList
        TaskFilter.Active -> Lucide.PartyPopper
        TaskFilter.Done -> Lucide.CircleDashed
    }
