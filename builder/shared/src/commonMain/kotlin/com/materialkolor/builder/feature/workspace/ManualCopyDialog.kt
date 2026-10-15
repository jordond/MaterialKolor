package com.materialkolor.builder.feature.workspace

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.workspace_manual_copy_done
import com.materialkolor.builder.generated.resources.workspace_manual_copy_hint
import com.materialkolor.builder.generated.resources.workspace_manual_copy_hint_touch
import com.materialkolor.builder.generated.resources.workspace_manual_copy_hint_touch_save
import com.materialkolor.builder.generated.resources.workspace_manual_copy_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.widget.SelectableText
import org.jetbrains.compose.resources.stringResource

/**
 * How much of the window the manual copy dialog's text may take before it scrolls.
 */
private const val MANUAL_COPY_HEIGHT_FRACTION = 0.5f

/**
 * The text a copy the browser refused was for, to select and copy by hand. The export sheet and the
 * poster's copy buttons both open it, and neither says Copied when it does.
 *
 * A hex or a Kotlin literal from the poster is one line, which a finger selects too. A file from
 * the export sheet is several, which on the web only a mouse selects. So on a touch screen the hint
 * sends a finger to [saveLabel], the sheet's zip button, or else back to Copy.
 *
 * @param[returnFocusTo] The copy button that opened it, which gets focus back once it closes.
 * @param[saveLabel] The label of the button that saves the files another way, when there is one.
 */
@Composable
internal fun ManualCopyDialog(
    visible: Boolean,
    text: String,
    onDismissRequest: () -> Unit,
    returnFocusTo: FocusRequester? = null,
    saveLabel: String? = null,
) {
    val layout = LocalLayout.current
    // Only the web reports a coarse pointer, so this is a finger on the web.
    val fingerCannotSelect = layout.coarsePointer && text.any { char -> char == '\n' || char == '\r' }
    val hint = when {
        !fingerCannotSelect -> stringResource(Res.string.workspace_manual_copy_hint)
        saveLabel != null -> stringResource(Res.string.workspace_manual_copy_hint_touch_save, saveLabel)
        else -> stringResource(Res.string.workspace_manual_copy_hint_touch)
    }
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.workspace_manual_copy_title),
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(
                onClick = onDismissRequest,
                label = stringResource(Res.string.workspace_manual_copy_done),
                emphasis = Emphasis.Primary,
            )
        },
    ) {
        BuilderText(text = hint, emphasis = Emphasis.Secondary)
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * MANUAL_COPY_HEIGHT_FRACTION)) {
            SelectableText(text = text, style = BuilderTextStyle.Value)
        }
    }
}
