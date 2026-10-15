package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.runtime.Composable
import com.materialkolor.sample.customtheme.ui.component.Button
import com.materialkolor.sample.customtheme.ui.component.ButtonStyle
import com.materialkolor.sample.customtheme.ui.component.ModalDialog
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun ClearDoneDialog(
    count: Int,
    dispatch: (SampleAction) -> Unit,
) {
    ModalDialog(
        title = SampleCopy.dialogTitle,
        body = SampleCopy.dialogBody(count),
        onDismissRequest = { dispatch(SampleAction.DismissClearDone) },
    ) {
        Button(
            text = SampleCopy.dialogCancel,
            onClick = { dispatch(SampleAction.DismissClearDone) },
            style = ButtonStyle.Quiet,
        )

        Button(
            text = SampleCopy.dialogConfirm,
            onClick = { dispatch(SampleAction.ConfirmClearDone) },
            style = ButtonStyle.Danger,
        )
    }
}
