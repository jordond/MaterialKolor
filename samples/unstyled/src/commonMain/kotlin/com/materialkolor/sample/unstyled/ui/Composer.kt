package com.materialkolor.sample.unstyled.ui

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
import androidx.compose.ui.platform.testTag
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.model.canAddTask
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.ui.component.Button
import com.materialkolor.sample.unstyled.ui.component.Choice
import com.materialkolor.sample.unstyled.ui.component.ChoiceChips
import com.materialkolor.sample.unstyled.ui.component.TextField

private val TagChoices: List<Choice<TaskTag>> = TaskTag.entries.map { tag ->
    Choice(value = tag, label = SampleCopy.label(tag), testTag = SampleTags.composerTag(tag))
}

/**
 * The row that adds a task. A field, the tag the task gets and the Add button, which Enter in the field presses too.
 *
 * What is typed is the only state the UI keeps. The title goes to the store once it is added, and the field clears
 * and keeps its focus for the next one.
 *
 * @param[composerTag] The tag a new task gets.
 * @param[onTagChange] Called with the tag picked for the next task.
 * @param[onAdd] Called with the typed title, never a blank one.
 * @param[modifier] Applied to the row.
 */
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
    val add = {
        val title = input.text.toString()
        if (canAddTask(title)) {
            onAdd(title)
            input.clearText()
        }
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            state = input,
            placeholder = SampleCopy.inputPlaceholder,
            onSubmit = add,
            modifier = Modifier
                .testTag(SampleTags.TaskInput)
                .focusRequester(focusRequester)
                .weight(1f),
        )
        ChoiceChips(
            choices = TagChoices,
            selected = composerTag,
            onSelect = onTagChange,
            colors = { tag -> tag.chipColors() },
        )
        Button(
            label = SampleCopy.add,
            onClick = add,
            modifier = Modifier.testTag(SampleTags.AddTask),
            icon = Lucide.Plus,
            enabled = canAdd,
        )
    }
}
