package com.materialkolor.sample.unstyled.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.composeunstyled.Text
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.ui.component.Choice
import com.materialkolor.sample.unstyled.ui.component.PillTabs
import com.materialkolor.sample.unstyled.ui.component.ProgressBar

/**
 * The Tasks section. How far along the list is, the composer, the filter and the list itself.
 */
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
                modifier = Modifier.testTag(SampleTags.Summary),
                style = TasksType.Emphasis,
            )
            ProgressBar(
                progress = state.progress,
                modifier = Modifier.testTag(SampleTags.Progress),
            )
        }

        Composer(
            composerTag = state.composerTag,
            onTagChange = { tag -> dispatch(SampleAction.SelectComposerTag(tag)) },
            onAdd = { title -> dispatch(SampleAction.AddTask(title)) },
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            PillTabs(
                choices = TaskFilter.entries.map { filter ->
                    Choice(
                        value = filter,
                        label = SampleCopy.label(filter),
                        testTag = SampleTags.filter(filter),
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
