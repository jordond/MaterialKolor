package com.materialkolor.builder.feature.workspace

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.workspace_manual_copy_done
import com.materialkolor.builder.generated.resources.workspace_manual_copy_hint
import com.materialkolor.builder.generated.resources.workspace_manual_copy_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.layout.LocalLayout
import org.jetbrains.compose.resources.stringResource

/** How much of the window the manual copy dialog's text may take before it scrolls. */
private const val MANUAL_COPY_HEIGHT_FRACTION = 0.5f

/**
 * The text a copy the browser refused was for, to select and copy by hand (F-26). The export sheet
 * and the poster's copy buttons both open it, and neither says Copied when it does.
 */
@Composable
internal fun ManualCopyDialog(
    visible: Boolean,
    text: String,
    onDismissRequest: () -> Unit,
) {
    val layout = LocalLayout.current
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.workspace_manual_copy_title),
        actions = {
            BuilderButton(
                onClick = onDismissRequest,
                label = stringResource(Res.string.workspace_manual_copy_done),
                emphasis = Emphasis.Primary,
            )
        },
    ) {
        BuilderText(text = stringResource(Res.string.workspace_manual_copy_hint), emphasis = Emphasis.Secondary)
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * MANUAL_COPY_HEIGHT_FRACTION)) {
            SelectionContainer {
                BuilderText(text = text, style = BuilderTextStyle.Value)
            }
        }
    }
}
