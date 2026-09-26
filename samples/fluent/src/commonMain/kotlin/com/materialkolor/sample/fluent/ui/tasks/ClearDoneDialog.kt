package com.materialkolor.sample.fluent.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.materialkolor.sample.shared.ui.SampleCopy
import io.github.composefluent.component.ContentDialog
import io.github.composefluent.component.ContentDialogButton
import io.github.composefluent.component.Text

@Composable
internal fun ClearDoneDialog(
    visible: Boolean,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Clearing drops the count to zero in the same step that hides the dialog, so the body keeps the last count it
    // showed while it fades out.
    val shownCount by produceState(initialValue = count, visible, count) {
        if (visible) value = count
    }

    ContentDialog(
        title = SampleCopy.dialogTitle,
        visible = visible,
        content = { Text(text = SampleCopy.dialogBody(shownCount)) },
        primaryButtonText = SampleCopy.dialogConfirm,
        closeButtonText = SampleCopy.dialogCancel,
        onButtonClick = { button ->
            when (button) {
                ContentDialogButton.Primary -> onConfirm()
                ContentDialogButton.Secondary, ContentDialogButton.Close -> onDismiss()
            }
        },
    )
}
