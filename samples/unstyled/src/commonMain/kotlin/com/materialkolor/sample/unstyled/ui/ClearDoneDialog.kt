package com.materialkolor.sample.unstyled.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import com.materialkolor.sample.unstyled.ui.component.Button
import com.materialkolor.sample.unstyled.ui.component.ButtonStyle
import com.materialkolor.sample.unstyled.ui.component.ConfirmDialog

/**
 * Asks before Clear done removes the finished tasks. Keep closes it and changes nothing.
 */
@Composable
internal fun ClearDoneDialog(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val visible = state.isClearDialogVisible
    // Clearing drops the count to zero while the panel fades out, so it keeps the count it asked about.
    var askedCount by remember { mutableIntStateOf(state.doneCount) }
    SideEffect { if (visible) askedCount = state.doneCount }
    val count = if (visible) state.doneCount else askedCount

    ConfirmDialog(
        visible = visible,
        title = SampleCopy.dialogTitle,
        body = SampleCopy.dialogBody(count),
        onDismiss = { dispatch(SampleAction.DismissClearDone) },
        modifier = Modifier.testTag(SampleTags.ClearDialog),
    ) {
        Button(
            label = SampleCopy.dialogCancel,
            onClick = { dispatch(SampleAction.DismissClearDone) },
            modifier = Modifier.testTag(SampleTags.ClearCancel),
            style = ButtonStyle.Quiet,
        )
        Button(
            label = SampleCopy.dialogConfirm,
            onClick = { dispatch(SampleAction.ConfirmClearDone) },
            modifier = Modifier.testTag(SampleTags.ClearConfirm),
            style = ButtonStyle.Danger,
        )
    }
}
