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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.SampleTheme
import com.materialkolor.sample.fluent.ui.component.LabeledButton
import com.materialkolor.sample.fluent.ui.component.SegmentedPicker
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.model.canAddTask
import com.materialkolor.sample.shared.ui.SampleCopy
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextField
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Add

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
    val submit: () -> Unit = {
        if (canAdd) {
            onAdd(title)
            title = ""
        }
        focusRequester.requestFocus()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            value = title,
            onValueChange = { value -> title = value },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            placeholder = { Text(text = SampleCopy.inputPlaceholder) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )

        SegmentedPicker(
            options = TaskTag.entries,
            selected = tag,
            onSelect = onTagChange,
            icon = { option -> TagDot(tag = option) },
            text = { option -> Text(text = SampleCopy.label(option)) },
        )

        LabeledButton(
            label = SampleCopy.add,
            onClick = submit,
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
