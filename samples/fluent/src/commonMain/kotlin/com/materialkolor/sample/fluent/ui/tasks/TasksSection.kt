package com.materialkolor.sample.fluent.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.ui.component.Divider
import com.materialkolor.sample.fluent.ui.component.LabeledButton
import com.materialkolor.sample.fluent.ui.component.SectionCard
import com.materialkolor.sample.fluent.ui.component.SegmentedPicker
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.Text

@Composable
internal fun TasksSection(
    state: SampleState,
    onAction: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Progress(state = state)

        Composer(
            tag = state.composerTag,
            onTagChange = { tag -> onAction(SampleAction.SelectComposerTag(tag)) },
            onAdd = { title -> onAction(SampleAction.AddTask(title)) },
        )

        FilterPicker(
            state = state,
            onSelect = { filter -> onAction(SampleAction.SelectFilter(filter)) },
        )

        TaskList(
            state = state,
            onAction = onAction,
        )

        Divider()

        Footer(
            state = state,
            onClearDone = { onAction(SampleAction.RequestClearDone) },
        )
    }
}

@Composable
private fun Progress(state: SampleState) {
    val progress by animateFloatAsState(targetValue = state.progress, label = "TaskProgress")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = SampleCopy.summary(done = state.doneCount, total = state.totalCount),
            style = FluentTheme.typography.bodyStrong,
        )
        ProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FilterPicker(
    state: SampleState,
    onSelect: (TaskFilter) -> Unit,
) {
    SegmentedPicker(
        options = TaskFilter.entries,
        selected = state.filter,
        onSelect = onSelect,
    ) { filter ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = SampleCopy.label(filter))
            Text(
                text = state.count(filter).toString(),
                style = FluentTheme.typography.caption,
                color = FluentTheme.colors.text.text.secondary,
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
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = SampleCopy.remaining(state.activeCount),
            modifier = Modifier.weight(1f),
            color = FluentTheme.colors.text.text.secondary,
        )
        LabeledButton(
            label = SampleCopy.clearDone,
            onClick = onClearDone,
            enabled = state.canClearDone,
        )
    }
}
