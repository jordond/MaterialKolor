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
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessModal
import com.materialkolor.builder.kit.headless.keepTaps
import com.materialkolor.builder.kit.headless.modalPane
import com.materialkolor.builder.kit.headless.modalTitle
import com.materialkolor.builder.kit.headless.overlayLibrary
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
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
 * it, even with no actions to focus. Esc and a click on the veil call
 * [onDismissRequest], and once it has gone focus goes back to [returnFocusTo] (AR-09). It scales up
 * from 0.96 with a fade, or only fades under reduced motion. Material3 draws its `AlertDialog`, the
 * other skins the headless dialog. Where overlays render in the page (D40) Material3 draws its own
 * dialog container over the headless modal instead.
 *
 * @param[visible] Whether the dialog is open.
 * @param[onDismissRequest] Called when the dialog asks to close.
 * @param[title] The dialog's name, shown at the top and read as its pane title.
 * @param[modifier] Applied to the dialog panel.
 * @param[returnFocusTo] The trigger that opened the dialog. Attach it to the trigger with
 * `Modifier.focusRequester`.
 * @param[titleShown] Whether the title shows at the top. A dialog whose first control says what it
 * is, such as a search field, hides it and still goes by it.
 * @param[actions] The buttons along the bottom, the confirming one last, or null for no row of
 * actions.
 * @param[content] The body. It takes at most the height the title and the actions leave, in every
 * skin, so a picture or a list in it gets what is left and the actions stay on screen, on a phone on
 * its side as well. A list that sits over more of the body takes `Modifier.weight(1f, fill = false)`
 * from the body's column, so what is under it keeps its room.
 */
@Composable
public fun BuilderDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    titleShown: Boolean = true, // b-511
    actions: (@Composable RowScope.() -> Unit)? = null, // b-511
    content: @Composable ColumnScope.() -> Unit,
) {
    val frame = DialogFrame(titleShown, actions)
    val tokens = LocalBuilderTokens.current
    // b-221b
    when (overlayLibrary()) {
        Library.Material3 -> {
            MaterialDialog(visible, onDismissRequest, title, returnFocusTo, modifier, frame, content)
        }
        Library.Unstyled -> {
            HeadlessDialog(
                visible,
                onDismissRequest,
                title,
                unstyledOverlayStyle(tokens),
                returnFocusTo,
                modifier,
                frame,
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
                frame,
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
                frame,
                content,
            )
        }
    }
}

/** The headless dialog in [style]. */
@Composable
private fun HeadlessDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    style: OverlayStyle,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    frame: DialogFrame,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    HeadlessModal(visible, onDismissRequest, style.scrim, Alignment.Center, returnFocusTo) {
        Column(
            modifier = modifier
                .animateEnterExit(enter = popoverEnter(), exit = popoverExit())
                .padding(tokens.spacing.large)
                .widthIn(min = OverlayMetrics.dialogMinWidth, max = OverlayMetrics.dialogMaxWidth)
                .shadow(style.shadow, style.dialogShape)
                .clip(style.dialogShape)
                .background(style.surface)
                .then(if (style.border != null) Modifier.border(style.border, style.dialogShape) else Modifier)
                .modalPane(title)
                .keepTaps()
                .padding(tokens.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.large),
        ) {
            if (frame.titleShown) {
                BuilderText(title, Modifier.modalTitle(), style = BuilderTextStyle.Title, color = style.content)
            }
            Column(Modifier.weight(1f, fill = false), content = content) // b-230c
            val actions = frame.actions
            if (actions != null) {
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

// b-511

/**
 * What a dialog shows around its body, whether its title shows and the row of actions, if any.
 *
 * @property[titleShown] Whether the title shows at the top.
 * @property[actions] The buttons along the bottom, or null for none.
 */
internal class DialogFrame(
    val titleShown: Boolean,
    val actions: (@Composable RowScope.() -> Unit)?,
)
