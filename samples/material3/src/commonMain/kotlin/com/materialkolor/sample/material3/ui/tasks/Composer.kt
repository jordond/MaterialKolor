package com.materialkolor.sample.material3.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.material3.ui.component.colors
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.model.canAddTask
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun Composer(
    tag: TaskTag,
    onTagChange: (TaskTag) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    val canAdd = canAddTask(input.text.toString())
    val submit: () -> Unit = {
        val title = input.text.toString()
        if (canAddTask(title)) {
            onAdd(title)
            input.clearText()
        }
        focusRequester.requestFocus()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            state = input,
            placeholder = { Text(text = SampleCopy.inputPlaceholder) },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            onKeyboardAction = KeyboardActionHandler { submit() },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )

        TagPicker(tag = tag, onSelect = onTagChange)

        Button(
            onClick = submit,
            enabled = canAdd,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = SampleCopy.add)
        }
    }
}

@Composable
private fun TagPicker(
    tag: TaskTag,
    onSelect: (TaskTag) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in TaskTag.entries) {
            val selected = option == tag
            val colors = option.colors
            FilterChip(
                selected = selected,
                onClick = { onSelect(option) },
                label = { Text(text = SampleCopy.label(option)) },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.container,
                    selectedLabelColor = colors.content,
                    selectedLeadingIconColor = colors.content,
                ),
            )
        }
    }
}
