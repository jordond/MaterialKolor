package com.materialkolor.builder.feature.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_copy
import com.materialkolor.builder.generated.resources.share_manual
import com.materialkolor.builder.generated.resources.share_manual_touch_again
import com.materialkolor.builder.generated.resources.share_manual_touch_instead
import com.materialkolor.builder.generated.resources.share_send
import com.materialkolor.builder.generated.resources.share_title
import com.materialkolor.builder.generated.resources.share_unavailable
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The width the share dialog grows to, room for the Link preview well beside the details.
 */
private val DialogWidth: Dp = 920.dp

/**
 * The narrowest body that still holds the well and the details side by side.
 */
private val SplitWidth: Dp = 760.dp

/**
 * How wide the Link preview well is beside the details.
 */
private val WellWidth: Dp = 480.dp

/**
 * The share dialog, the link to the theme as it is now with a way to send it.
 *
 * The Link preview well shows the card chats and posts draw for the link, and beside it the
 * details say what a link is, rename the project, list what the link carries and show the whole
 * link. On a narrow body the well sits over the details and the body scrolls, and on a phone the
 * dialog is a full screen sheet with Copy link and Share along its bottom.
 *
 * Copy link, and Share where the share sheet is, each start their platform call inside the click,
 * undispatched, so the browser still counts the click as the user's. When the clipboard or the
 * sheet turns the link down the dialog stays open and says to copy it by hand, or on a touch screen
 * which button to try, and it never claims a copy that did not land.
 *
 * @param[visible] Whether the dialog is open.
 * @param[link] The link to share, or null when the theme cannot be put in one.
 * @param[sharesToSheet] Whether sharing goes to the share sheet here.
 * @param[copy] Puts a link on the clipboard.
 * @param[share] Hands a link to the share sheet.
 * @param[onDone] Called with how a copy or share that worked went, [ShareOutcome.Copied] or
 *   [ShareOutcome.Shared].
 * @param[onDismissRequest] Called when the dialog asks to close.
 * @param[document] The theme the link carries, for the list of what is in it.
 * @param[card] Where the link's card is.
 * @param[name] The project name and what the dialog can do with it.
 * @param[returnFocusTo] The button that opened the dialog, which gets focus back once it closes.
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
    document: ThemeDocument,
    card: CardState,
    name: ShareName,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val scope = rememberCoroutineScope()
    // The copy and the share that failed, the latest last, while the dialog says so.
    var failed by remember(link, visible) { mutableStateOf(emptyList<ShareOutcome>()) }

    fun send(call: suspend (String) -> ShareOutcome) {
        val url = link ?: return
        scope.launchSend(url, call) { outcome ->
            when (outcome) {
                ShareOutcome.Copied, ShareOutcome.Shared -> onDone(outcome)
                ShareOutcome.CopyFailed, ShareOutcome.ShareFailed -> failed = failed - outcome + outcome
            }
        }
    }

    val copyFocus = remember { FocusRequester() }
    val actions: @Composable () -> Unit = {
        val spacing = LocalBuilderTokens.current.spacing
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            ShareButtons(
                sharesToSheet = sharesToSheet,
                onCopy = { send(copy) },
                onShare = { send(share) },
                copyFocus = copyFocus,
            )
            failed.lastOrNull()?.let { outcome ->
                BuilderText(text = manualText(outcome, failed.size > 1, sharesToSheet), emphasis = Emphasis.Danger)
            }
        }
        // Copy link takes focus as the dialog opens.
        LaunchedEffect(copyFocus) { copyFocus.requestFocus() }
    }
    val title = stringResource(Res.string.share_title)
    if (LocalLayout.current.windowClass == WindowClass.Compact) {
        BuilderSheet(
            visible = visible,
            onDismissRequest = onDismissRequest,
            title = title,
            presentation = SheetPresentation.FullScreen,
            modifier = modifier,
            returnFocusTo = returnFocusTo,
            footer = if (link != null) actions else null,
        ) {
            BuilderScrollArea(Modifier.weight(1f), tabStop = false, scrollbarInGutter = true) {
                if (link == null) {
                    BuilderText(text = stringResource(Res.string.share_unavailable))
                } else {
                    ShareBody(split = false, link = link, document = document, card = card, name = name)
                }
            }
        }
        return
    }
    if (link == null) {
        BuilderDialog(
            visible = visible,
            onDismissRequest = onDismissRequest,
            title = title,
            modifier = modifier,
            returnFocusTo = returnFocusTo,
            closeButton = true,
        ) { BuilderText(text = stringResource(Res.string.share_unavailable)) }
        return
    }
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        closeButton = true,
        maxWidth = DialogWidth,
    ) {
        BoxWithConstraints {
            val split = maxWidth >= SplitWidth
            BuilderScrollArea(tabStop = false, fitContent = true) {
                ShareBody(split = split, link = link, document = document, card = card, name = name, actions = actions)
            }
        }
    }
}

/**
 * The well and the details, side by side when [split] and else the well over the details, with
 * [actions] under the details.
 */
@Composable
private fun ShareBody(
    split: Boolean,
    link: String,
    document: ThemeDocument,
    card: CardState,
    name: ShareName,
    actions: (@Composable () -> Unit)? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val details: @Composable (Modifier) -> Unit = { detailsModifier ->
        Column(detailsModifier, verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
            ShareDetails(link = link, document = document, name = name)
            actions?.invoke()
        }
    }
    if (split) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
            ShareWell(card, Modifier.width(WellWidth))
            details(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
            ShareWell(card, Modifier.fillMaxWidth())
            details(Modifier.fillMaxWidth())
        }
    }
}

/**
 * What the dialog says when [outcome], a copy or share, did not land. With a mouse that is to copy
 * the link by hand. A finger on the web cannot select the wrapped link, so on a touch screen it
 * sends the finger to the other button when there are two, or back to Copy link. Once Copy link and
 * Share have [bothFailed], it sends the finger back to Copy link.
 */
@Composable
private fun manualText(
    outcome: ShareOutcome,
    bothFailed: Boolean,
    sharesToSheet: Boolean,
): String {
    // Only the web reports a coarse pointer, so this is a finger on the web.
    if (!LocalLayout.current.coarsePointer) return stringResource(Res.string.share_manual)
    val copy = stringResource(Res.string.share_copy)
    return when {
        bothFailed -> stringResource(Res.string.share_manual_touch_again, copy)
        outcome == ShareOutcome.ShareFailed -> stringResource(Res.string.share_manual_touch_instead, copy)
        sharesToSheet -> stringResource(Res.string.share_manual_touch_instead, stringResource(Res.string.share_send))
        else -> stringResource(Res.string.share_manual_touch_again, copy)
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
