package com.materialkolor.builder.feature.share

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_body
import com.materialkolor.builder.generated.resources.share_copy
import com.materialkolor.builder.generated.resources.share_manual
import com.materialkolor.builder.generated.resources.share_send
import com.materialkolor.builder.generated.resources.share_title
import com.materialkolor.builder.generated.resources.share_unavailable
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The share dialog (F-32), the link to the theme as it is now with a way to send it.
 *
 * On a touch screen with a share sheet Share comes first and Copy link sits beside it, elsewhere
 * Copy link is the one action. Each starts its platform call inside the click, undispatched, so the
 * browser still counts the click as the user's. When the clipboard or the sheet turns the link down
 * the dialog stays open and says to copy it by hand, and it never claims a copy that did not land.
 *
 * @param[visible] Whether the dialog is open.
 * @param[link] The link to share, or null when the theme cannot be put in one.
 * @param[sharesToSheet] Whether sharing goes to the share sheet here.
 * @param[copy] Puts a link on the clipboard.
 * @param[share] Hands a link to the share sheet.
 * @param[onDone] Called with how a copy or share that worked went, [ShareOutcome.Copied] or
 *   [ShareOutcome.Shared].
 * @param[onDismissRequest] Called when the dialog asks to close.
 * @param[returnFocusTo] The button that opened the dialog, which gets focus back once it closes (AR-09).
 */
@Composable
internal fun ShareDialog(
    visible: Boolean,
    link: String?,
    sharesToSheet: Boolean,
    copy: suspend (String) -> ShareOutcome,
    share: suspend (String) -> ShareOutcome,
    onDone: (ShareOutcome) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val scope = rememberCoroutineScope()
    var manual by remember(link, visible) { mutableStateOf(false) }

    fun send(call: suspend (String) -> ShareOutcome) {
        val url = link ?: return
        scope.launchSend(url, call) { outcome ->
            when (outcome) {
                ShareOutcome.Copied, ShareOutcome.Shared -> onDone(outcome)
                ShareOutcome.CopyFailed, ShareOutcome.ShareFailed -> manual = true
            }
        }
    }

    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.share_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            if (link != null) {
                BuilderButton(
                    onClick = { send(copy) },
                    label = stringResource(Res.string.share_copy),
                    emphasis = if (sharesToSheet) Emphasis.Secondary else Emphasis.Primary,
                    icon = IconId.Copy,
                )
            }
            if (link != null && sharesToSheet) {
                BuilderButton(
                    onClick = { send(share) },
                    label = stringResource(Res.string.share_send),
                    emphasis = Emphasis.Primary,
                    icon = IconId.Share,
                )
            }
        },
    ) {
        if (link == null) {
            BuilderText(text = stringResource(Res.string.share_unavailable))
        } else {
            BuilderText(text = stringResource(Res.string.share_body))
            SelectionContainer {
                BuilderText(text = link, style = BuilderTextStyle.Code)
            }
        }
        if (manual) {
            BuilderText(text = stringResource(Res.string.share_manual), emphasis = Emphasis.Danger)
        }
    }
}

/**
 * What Copy link and Share do when clicked. Hands [url] to [call], then hands [onOutcome] how it
 * went.
 *
 * Browsers only copy and share inside the click, so the platform call inside [call] is the first
 * suspension and it starts before this returns. Call it straight from `onClick`, with no hop through
 * a model before it.
 */
internal fun CoroutineScope.launchSend(
    url: String,
    call: suspend (String) -> ShareOutcome,
    onOutcome: (ShareOutcome) -> Unit,
) {
    launch(start = CoroutineStart.UNDISPATCHED) {
        onOutcome(call(url))
    }
}
