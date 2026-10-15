package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Button
import com.materialkolor.sample.customtheme.ui.component.ButtonStyle
import com.materialkolor.sample.customtheme.ui.component.Glyph
import com.materialkolor.sample.customtheme.ui.component.Icon
import com.materialkolor.sample.customtheme.ui.component.ProgressBar
import com.materialkolor.sample.customtheme.ui.component.Rule
import com.materialkolor.sample.customtheme.ui.component.SegmentedControl
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.customtheme.ui.component.halftone
import com.materialkolor.sample.customtheme.ui.component.perforation
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
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = SampleCopy.summary(done = state.doneCount, total = state.totalCount).uppercase(),
                style = AppType.Heading,
                color = colors.ink,
            )

            ProgressBar(progress = state.progress)
        }

        TaskComposer(
            composerTag = state.composerTag,
            dispatch = dispatch,
        )

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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

/**
 * The list is a sheet of coupons on a blue halftone shadow, each task torn off along a perforation.
 */
@Composable
private fun TaskList(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .halftone(color = colors.blue, colors = colors, offset = SheetShadow) { _, _ -> SHADOW_COVERAGE }
            .background(colors.paper)
            .border(Rule, colors.ink),
    ) {
        if (state.visibleTasks.isEmpty()) {
            EmptyState(filter = state.filter)
        }

        for (task in state.visibleTasks) {
            key(task.id) {
                TaskRow(
                    task = task,
                    dispatch = dispatch,
                    modifier = Modifier.perforation(colors.inkSoft),
                )
            }
        }

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .perforation(colors.inkSoft),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .halftone(color = colors.pink, colors = colors, shape = CircleShape, cell = 5.dp) { _, _ ->
                    EMPTY_COVERAGE
                },
        ) {
            Icon(
                glyph = glyph,
                color = colors.ink,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = SampleCopy.empty(filter).uppercase(),
            style = AppType.Label,
            color = colors.inkSoft,
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = SampleCopy.remaining(state.activeCount).uppercase(),
            style = AppType.Label,
            color = colors.inkSoft,
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

private val SheetShadow = DpOffset(10.dp, 10.dp)

private const val SHADOW_COVERAGE = 0.35f
private const val EMPTY_COVERAGE = 0.3f
