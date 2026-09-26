package com.materialkolor.sample.unstyled.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.unstyled.ui.component.Button
import com.materialkolor.sample.unstyled.ui.component.ButtonStyle
import com.materialkolor.sample.unstyled.ui.component.ConfirmDialog

@Composable
internal fun ClearDoneDialog(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val visible = state.isClearDialogVisible
    var askedCount by remember { mutableIntStateOf(state.doneCount) }
    SideEffect { if (visible) askedCount = state.doneCount }

    val count = if (visible) state.doneCount else askedCount

    ConfirmDialog(
        visible = visible,
        icon = Lucide.Trash2,
        title = SampleCopy.dialogTitle,
        body = SampleCopy.dialogBody(count),
        onDismiss = { dispatch(SampleAction.DismissClearDone) },
    ) {
        Button(
            label = SampleCopy.dialogCancel,
            onClick = { dispatch(SampleAction.DismissClearDone) },
            style = ButtonStyle.Quiet,
        )

        Button(
            label = SampleCopy.dialogConfirm,
            onClick = { dispatch(SampleAction.ConfirmClearDone) },
            style = ButtonStyle.Danger,
        )
    }
}
