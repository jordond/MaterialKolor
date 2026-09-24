package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppShapes
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Button
import com.materialkolor.sample.customtheme.ui.component.ButtonStyle
import com.materialkolor.sample.customtheme.ui.component.Glyph
import com.materialkolor.sample.customtheme.ui.component.Icon
import com.materialkolor.sample.customtheme.ui.component.ProgressBar
import com.materialkolor.sample.customtheme.ui.component.SegmentedControl
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun TasksSection(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = SampleCopy.summary(done = state.doneCount, total = state.totalCount),
                style = AppType.BodyStrong,
                color = colors.textStrong,
            )

            ProgressBar(progress = state.progress)
        }

        TaskComposer(
            composerTag = state.composerTag,
            dispatch = dispatch,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedControl(
                options = TaskFilter.entries,
                selected = state.filter,
                onSelect = { filter -> dispatch(SampleAction.SelectFilter(filter)) },
                label = SampleCopy::label,
                badge = { filter -> state.count(filter).toString() },
            )

            TaskList(
                state = state,
                dispatch = dispatch,
            )
        }
    }

    if (state.isClearDialogVisible) {
        ClearDoneDialog(
            count = state.doneCount,
            dispatch = dispatch,
        )
    }
}

@Composable
private fun TaskList(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderFaint, AppShapes.Card),
    ) {
        if (state.visibleTasks.isEmpty()) {
            EmptyState(filter = state.filter)
        }

        state.visibleTasks.forEachIndexed { index, task ->
            key(task.id) {
                if (index > 0) Divider()
                TaskRow(
                    task = task,
                    dispatch = dispatch,
                )
            }
        }

        Divider()
        Footer(
            state = state,
            dispatch = dispatch,
        )
    }
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    val colors = LocalAppColors.current
    val glyph = when (filter) {
        TaskFilter.All -> Glyph.Plus
        TaskFilter.Active -> Glyph.Check
        TaskFilter.Done -> Glyph.Ring
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surfaceSunken),
        ) {
            Icon(
                glyph = glyph,
                color = colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = SampleCopy.empty(filter),
            style = AppType.Body,
            color = colors.textMuted,
        )
    }
}

@Composable
private fun Footer(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = SampleCopy.remaining(state.activeCount),
            style = AppType.Body,
            color = colors.textMuted,
            modifier = Modifier.weight(1f),
        )

        Button(
            text = SampleCopy.clearDone,
            onClick = { dispatch(SampleAction.RequestClearDone) },
            style = ButtonStyle.Quiet,
            enabled = state.canClearDone,
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalAppColors.current.borderFaint),
    )
}
