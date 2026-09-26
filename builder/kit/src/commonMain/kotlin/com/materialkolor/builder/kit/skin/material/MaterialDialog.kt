package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.builder.kit.control.DialogFrame
import com.materialkolor.builder.kit.control.DialogStart
import com.materialkolor.builder.kit.control.DialogTitleRow
import com.materialkolor.builder.kit.control.StartPastCloseButton
import com.materialkolor.builder.kit.headless.HeadlessModal
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.headless.ReturnFocusWhenGone
import com.materialkolor.builder.kit.headless.keepTaps
import com.materialkolor.builder.kit.headless.modalPane
import com.materialkolor.builder.kit.headless.modalTitle
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.popoverExit
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * Material's `AlertDialog`. Its window keeps focus inside, focus starts on the first action unless
 * something inside has already taken it, a field that asks for it as it opens, and Esc closes it
 * even where the platform does not turn Esc into back.
 *
 * `AlertDialog` holds its panel to Material's widest dialog, so a dialog allowed wider than that
 * draws the panel of [MaterialPageDialog] in a plain dialog window instead. `AlertDialog` always
 * opens a window of its own, so where overlays render in the page it gives way to
 * [MaterialPageDialog].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MaterialDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    frame: DialogFrame,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalOverlaysInTree.current) {
        MaterialPageDialog(visible, onDismissRequest, title, returnFocusTo, modifier, frame, content)
        return
    }
    ReturnFocusWhenGone(visible, returnFocusTo)
    if (!visible) return
    val firstAction = remember { FocusRequester() }
    val focusInside = remember { mutableStateOf(false) }
    val pane = Modifier
        .modalPane(title)
        .onFocusChanged { state -> focusInside.value = state.hasFocus }
        .onKeyEvent { event ->
            val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
            if (escape) onDismissRequest()
            escape
        }
    if (frame.maxWidth > OverlayMetrics.dialogMaxWidth) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                animateTransition = !LocalReducedMotion.current,
            ),
        ) {
            MaterialDialogPanel(
                title = title,
                onDismissRequest = onDismissRequest,
                modifier = modifier
                    .padding(LocalBuilderTokens.current.spacing.large)
                    .widthIn(min = OverlayMetrics.dialogMinWidth, max = frame.maxWidth)
                    .then(pane),
                frame = frame,
                actionsModifier = Modifier.focusRequester(firstAction),
                content = content,
            )
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Row(
                    modifier = Modifier.focusRequester(firstAction),
                    horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
                    content = frame.actions ?: {},
                )
            },
            modifier = modifier.then(pane),
            title = if (frame.titleShown || frame.closeButton) {
                {
                    DialogTitleRow(frame, onDismissRequest) { titleModifier ->
                        Text(title, titleModifier.modalTitle())
                    }
                }
            } else {
                null
            },
            text = { Column(content = content) },
            properties = DialogProperties(animateTransition = !LocalReducedMotion.current),
        )
    }
    LaunchedEffect(Unit) { if (!focusInside.value) firstAction.requestFocus() }
}

/**
 * Material's dialog container over the headless modal, for where overlays render in the page.
 *
 * It moves with the kit's motion. The headless modal holds focus inside, starting on the first
 * action, or on the panel when there is none.
 */
@Composable
private fun MaterialPageDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    frame: DialogFrame,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    HeadlessModal(visible, onDismissRequest, tokens.scrim, Alignment.Center, returnFocusTo) {
        val start = remember { DialogStart() }
        MaterialDialogPanel(
            title = title,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .animateEnterExit(enter = popoverEnter(), exit = popoverExit())
                .padding(tokens.spacing.large)
                .widthIn(min = OverlayMetrics.dialogMinWidth, max = frame.maxWidth)
                .modalPane(title)
                .keepTaps(),
            frame = frame,
            bodyModifier = Modifier.focusRequester(start.body),
            actionsModifier = Modifier.focusRequester(start.actions),
            content = content,
        )
        StartPastCloseButton(frame, start)
    }
}

/**
 * Material's dialog panel, the one [MaterialPageDialog] draws in the page and [MaterialDialog]
 * draws in a window when it is wider than `AlertDialog` allows.
 *
 * It takes the container colour, shape and tonal elevation of `AlertDialogDefaults` and Material's
 * headline for the title.
 */
@Composable
private fun MaterialDialogPanel(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    frame: DialogFrame,
    content: @Composable ColumnScope.() -> Unit,
    bodyModifier: Modifier = Modifier,
    actionsModifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val typography = MaterialTheme.typography
    Surface(
        modifier = modifier,
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.large),
        ) {
            DialogTitleRow(frame, onDismissRequest) { titleModifier ->
                Text(
                    text = title,
                    modifier = titleModifier.modalTitle(),
                    color = AlertDialogDefaults.titleContentColor,
                    style = typography.headlineSmall,
                )
            }
            CompositionLocalProvider(
                LocalContentColor provides AlertDialogDefaults.textContentColor,
                LocalTextStyle provides typography.bodyMedium,
            ) { Column(Modifier.weight(1f, fill = false).then(bodyModifier), content = content) }
            val actions = frame.actions
            if (actions != null) {
                Row(
                    modifier = Modifier.align(Alignment.End).then(actionsModifier),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
