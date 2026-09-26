package com.materialkolor.sample.unstyled.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.model.canAddTask
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.ui.component.Button
import com.materialkolor.sample.unstyled.ui.component.Card
import com.materialkolor.sample.unstyled.ui.component.Choice
import com.materialkolor.sample.unstyled.ui.component.TagPicker
import com.materialkolor.sample.unstyled.ui.component.TextField

private val TagChoices: List<Choice<TaskTag>> = TaskTag.entries.map { tag ->
    Choice(value = tag, label = SampleCopy.label(tag))
}

@Composable
internal fun Composer(
    composerTag: TaskTag,
    onTagChange: (TaskTag) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    val canAdd = canAddTask(input.text.toString())
    val add: () -> Unit = {
        val title = input.text.toString()
        if (canAddTask(title)) {
            onAdd(title)
            input.clearText()
        }
        focusRequester.requestFocus()
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Small),
        ) {
            TextField(
                state = input,
                placeholder = SampleCopy.inputPlaceholder,
                onSubmit = add,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .weight(1f),
            )

            TagPicker(
                choices = TagChoices,
                selected = composerTag,
                onSelect = onTagChange,
                colors = { tag -> tag.colors() },
            )

            Button(
                label = SampleCopy.add,
                onClick = add,
                icon = Lucide.Plus,
                enabled = canAdd,
            )
        }
    }
}
