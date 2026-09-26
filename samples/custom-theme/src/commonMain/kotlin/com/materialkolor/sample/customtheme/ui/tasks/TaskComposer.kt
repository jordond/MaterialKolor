package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.Button
import com.materialkolor.sample.customtheme.ui.component.SegmentedControl
import com.materialkolor.sample.customtheme.ui.component.TextField
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.model.canAddTask
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun TaskComposer(
    composerTag: TaskTag,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val field = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    val canAdd = canAddTask(field.text.toString())

    val add: () -> Unit = {
        val title = field.text.toString()
        if (canAddTask(title)) {
            dispatch(SampleAction.AddTask(title))
            field.clearText()
        }
        focusRequester.requestFocus()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            state = field,
            placeholder = SampleCopy.inputPlaceholder,
            onSubmit = add,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )

        SegmentedControl(
            options = TaskTag.entries,
            selected = composerTag,
            onSelect = { tag -> dispatch(SampleAction.SelectComposerTag(tag)) },
            label = SampleCopy::label,
            accent = { tag -> tag.accent(colors) },
        )

        Button(
            text = SampleCopy.add,
            onClick = add,
            enabled = canAdd,
        )
    }
}
