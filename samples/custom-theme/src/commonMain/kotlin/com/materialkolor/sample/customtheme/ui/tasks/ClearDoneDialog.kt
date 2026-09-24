package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.materialkolor.sample.customtheme.ui.component.Button
import com.materialkolor.sample.customtheme.ui.component.ButtonStyle
import com.materialkolor.sample.customtheme.ui.component.ModalDialog
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags

/**
 * Asks before Clear done removes the [count] finished tasks. Keep, Esc and a click outside all leave the list alone.
 */
@Composable
internal fun ClearDoneDialog(
    count: Int,
    dispatch: (SampleAction) -> Unit,
) {
    ModalDialog(
        title = SampleCopy.dialogTitle,
        body = SampleCopy.dialogBody(count),
        onDismissRequest = { dispatch(SampleAction.DismissClearDone) },
        modifier = Modifier.testTag(SampleTags.ClearDialog),
    ) {
        Button(
            text = SampleCopy.dialogCancel,
            onClick = { dispatch(SampleAction.DismissClearDone) },
            style = ButtonStyle.Quiet,
            modifier = Modifier.testTag(SampleTags.ClearCancel),
        )

        Button(
            text = SampleCopy.dialogConfirm,
            onClick = { dispatch(SampleAction.ConfirmClearDone) },
            style = ButtonStyle.Danger,
            modifier = Modifier.testTag(SampleTags.ClearConfirm),
        )
    }
}
