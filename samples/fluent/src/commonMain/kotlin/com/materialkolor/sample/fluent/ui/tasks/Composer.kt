package com.materialkolor.sample.fluent.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.SampleTheme
import com.materialkolor.sample.fluent.ui.component.LabeledButton
import com.materialkolor.sample.fluent.ui.component.SegmentedPicker
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.model.canAddTask
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextField
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Add

/**
 * The row that adds a task. A text field, the tag picker and Add.
 *
 * The typed text is the only state the UI keeps. Add and Enter both hand it to [onAdd], clear the field and keep
 * the focus in it, so the next task can be typed straight away.
 *
 * @param[tag] The tag the next task gets.
 * @param[onTagChange] Called with the tag the user picks.
 * @param[onAdd] Called with the typed title.
 * @param[modifier] The modifier for the row.
 */
@Composable
internal fun Composer(
    tag: TaskTag,
    onTagChange: (TaskTag) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val canAdd = canAddTask(title)
    val submit = {
        if (canAdd) {
            onAdd(title)
            title = ""
        }
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = title,
            onValueChange = { value -> title = value },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag(SampleTags.TaskInput),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            placeholder = { Text(text = SampleCopy.inputPlaceholder) },
        )

        SegmentedPicker(
            options = TaskTag.entries,
            selected = tag,
            onSelect = onTagChange,
            testTag = { option -> SampleTags.composerTag(option) },
            icon = { option -> TagDot(tag = option) },
            text = { option -> Text(text = SampleCopy.label(option)) },
        )

        LabeledButton(
            label = SampleCopy.add,
            onClick = submit,
            testTag = SampleTags.AddTask,
            enabled = canAdd,
            accent = true,
            icon = Icons.Default.Add,
        )
    }
}

@Composable
private fun TagDot(tag: TaskTag) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(SampleTheme.colors.tint(tag).content),
    )
}
