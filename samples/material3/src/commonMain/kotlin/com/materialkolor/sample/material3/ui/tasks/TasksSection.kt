package com.materialkolor.sample.material3.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun TasksSection(
    state: SampleState,
    onAction: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Progress(state = state)

        Composer(
            tag = state.composerTag,
            onTagChange = { tag -> onAction(SampleAction.SelectComposerTag(tag)) },
            onAdd = { title -> onAction(SampleAction.AddTask(title)) },
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            FilterTabs(
                state = state,
                onSelect = { filter -> onAction(SampleAction.SelectFilter(filter)) },
            )

            TaskList(state = state, onAction = onAction)

            HorizontalDivider()

            Footer(
                state = state,
                onClearDone = { onAction(SampleAction.RequestClearDone) },
            )
        }
    }
}

@Composable
private fun Progress(state: SampleState) {
    val progress by animateFloatAsState(targetValue = state.progress, label = "TaskProgress")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = SampleCopy.summary(done = state.doneCount, total = state.totalCount),
            style = MaterialTheme.typography.titleMedium,
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FilterTabs(
    state: SampleState,
    onSelect: (TaskFilter) -> Unit,
) {
    SecondaryTabRow(
        selectedTabIndex = state.filter.ordinal,
        containerColor = Color.Transparent,
    ) {
        for (filter in TaskFilter.entries) {
            Tab(
                selected = filter == state.filter,
                onClick = { onSelect(filter) },
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = SampleCopy.label(filter))

                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text(text = state.count(filter).toString())
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TaskList(
    state: SampleState,
    onAction: (SampleAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 8.dp),
    ) {
        if (state.visibleTasks.isEmpty()) {
            EmptyState(filter = state.filter)
        }

        for (task in state.visibleTasks) {
            key(task.id) {
                TaskRow(
                    task = task,
                    onToggle = { onAction(SampleAction.ToggleTask(task.id)) },
                    onDelete = { onAction(SampleAction.DeleteTask(task.id)) },
                )
            }
        }
    }
}

@Composable
private fun Footer(
    state: SampleState,
    onClearDone: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Text(
            text = SampleCopy.remaining(state.activeCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        TextButton(
            onClick = onClearDone,
            enabled = state.canClearDone,
        ) {
            Text(text = SampleCopy.clearDone)
        }
    }
}
