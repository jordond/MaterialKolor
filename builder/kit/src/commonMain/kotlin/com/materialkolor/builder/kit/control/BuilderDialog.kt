package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessModal
import com.materialkolor.builder.kit.headless.ReturnFocusWhenGone
import com.materialkolor.builder.kit.headless.keepTaps
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.popoverExit
import com.materialkolor.builder.kit.skin.headless.unstyledOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialDialog
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A modal dialog with a title, a body and a row of actions.
 *
 * The title is the dialog's name. While it is open, focus starts inside it and Tab cannot leave
 * it. Esc and a click on the veil call [onDismissRequest], and once it has gone focus goes back to
 * [returnFocusTo] (AR-09). It scales up from 0.96 with a fade, or only fades under reduced motion.
 * Material3 draws its `AlertDialog`, the other skins the headless dialog.
 *
 * @param[visible] Whether the dialog is open.
 * @param[onDismissRequest] Called when the dialog asks to close.
 * @param[title] The dialog's name, shown at the top and read as its pane title.
 * @param[modifier] Applied to the dialog panel.
 * @param[returnFocusTo] The trigger that opened the dialog. Attach it to the trigger with
 * `Modifier.focusRequester`.
 * @param[actions] The buttons along the bottom, the confirming one last.
 * @param[content] The body.
 */
@Composable
public fun BuilderDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            ReturnFocusWhenGone(visible, returnFocusTo)
            MaterialDialog(visible, onDismissRequest, title, modifier, actions, content)
        }
        Library.Unstyled -> {
            HeadlessDialog(
                visible,
                onDismissRequest,
                title,
                unstyledOverlayStyle(tokens),
                returnFocusTo,
                modifier,
                actions,
                content,
            )
        }
        // fluent-placeholder
        Library.Fluent -> {
            HeadlessDialog(
                visible,
                onDismissRequest,
                title,
                fluentOverlayStyle(tokens),
                returnFocusTo,
                modifier,
                actions,
                content,
            )
        }
        Library.Custom -> {
            HeadlessDialog(
                visible,
                onDismissRequest,
                title,
                customOverlayStyle(tokens),
                returnFocusTo,
                modifier,
                actions,
                content,
            )
        }
    }
}

@Composable
private fun HeadlessDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    style: OverlayStyle,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    HeadlessModal(visible, onDismissRequest, style.scrim, Alignment.Center, returnFocusTo) {
        Column(
            modifier = modifier
                .animateEnterExit(enter = popoverEnter(), exit = popoverExit())
                .padding(tokens.spacing.large)
                .widthIn(min = DialogMinWidth, max = DialogMaxWidth)
                .shadow(style.shadow, style.dialogShape)
                .clip(style.dialogShape)
                .background(style.surface)
                .then(if (style.border != null) Modifier.border(style.border, style.dialogShape) else Modifier)
                .semantics { paneTitle = title }
                .keepTaps()
                .padding(tokens.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.large),
        ) {
            BuilderText(title, style = BuilderTextStyle.Title, color = style.content)
            Column(content = content)
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

private val DialogMinWidth = 280.dp
private val DialogMaxWidth = 560.dp
