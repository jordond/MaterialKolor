package com.materialkolor.sample.fluent.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.ui.component.Divider
import com.materialkolor.sample.fluent.ui.component.LabeledButton
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.DialogSize
import io.github.composefluent.component.FluentDialog
import io.github.composefluent.component.Text

/**
 * Asks before clearing finished tasks, laid out the way Fluent's `ContentDialog` is.
 *
 * `ContentDialog` builds its buttons itself and takes no modifier for them, so they could not carry test tags or
 * semantics. This is the same title, body, divider and button row on [FluentDialog], the surface `ContentDialog`
 * sits on, with Clear as the primary button and Keep as the close button. Escape keeps every task.
 *
 * @param[visible] Whether the dialog is up.
 * @param[count] How many finished tasks Clear would remove.
 * @param[onConfirm] Called when Clear is pressed.
 * @param[onDismiss] Called when Keep or Escape is pressed.
 */
@Composable
internal fun ClearDoneDialog(
    visible: Boolean,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Holds on to the count while the dialog fades out, so the body does not drop to zero on its way out.
    val shownCount by produceState(initialValue = count, visible, count) {
        if (visible) value = count
    }

    FluentDialog(
        visible = visible,
        size = DialogSize.Standard,
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

        Column(
            modifier = Modifier
                .testTag(SampleTags.ClearDialog)
                .onPreviewKeyEvent { event ->
                    val isEscape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                    if (isEscape) onDismiss()
                    isEscape
                }
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FluentTheme.colors.background.layer.alt)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = SampleCopy.dialogTitle,
                    style = FluentTheme.typography.subtitle,
                )
                Text(text = SampleCopy.dialogBody(shownCount))
            }

            Divider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledButton(
                    label = SampleCopy.dialogConfirm,
                    onClick = onConfirm,
                    testTag = SampleTags.ClearConfirm,
                    modifier = Modifier.weight(1f),
                    accent = true,
                )
                LabeledButton(
                    label = SampleCopy.dialogCancel,
                    onClick = onDismiss,
                    testTag = SampleTags.ClearCancel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
