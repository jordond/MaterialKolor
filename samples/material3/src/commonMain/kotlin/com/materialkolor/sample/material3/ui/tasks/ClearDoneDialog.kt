package com.materialkolor.sample.material3.ui.tasks

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun ClearDoneDialog(
    visible: Boolean,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = SampleCopy.dialogTitle) },
        text = { Text(text = SampleCopy.dialogBody(count)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = SampleCopy.dialogConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = SampleCopy.dialogCancel)
            }
        },
    )
}
