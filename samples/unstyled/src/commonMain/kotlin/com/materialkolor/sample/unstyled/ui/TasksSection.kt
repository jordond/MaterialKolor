package com.materialkolor.sample.unstyled.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composeunstyled.Text
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.ui.component.AccentTabs
import com.materialkolor.sample.unstyled.ui.component.Choice
import com.materialkolor.sample.unstyled.ui.component.ProgressBar

@Composable
internal fun TasksSection(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.XLarge),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Text(
                text = SampleCopy.summary(done = state.doneCount, total = state.totalCount),
                style = TasksType.Emphasis,
            )
            ProgressBar(progress = state.progress)
        }

        Composer(
            composerTag = state.composerTag,
            onTagChange = { tag -> dispatch(SampleAction.SelectComposerTag(tag)) },
            onAdd = { title -> dispatch(SampleAction.AddTask(title)) },
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            AccentTabs(
                choices = TaskFilter.entries.map { filter ->
                    Choice(
                        value = filter,
                        label = SampleCopy.label(filter),
                        badge = state.count(filter).toString(),
                    )
                },
                selected = state.filter,
                onSelect = { filter -> dispatch(SampleAction.SelectFilter(filter)) },
            )

            TaskList(state = state, dispatch = dispatch)
        }
    }
}
