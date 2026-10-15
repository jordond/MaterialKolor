package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.close
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.material.MaterialDialog
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * A modal dialog with a title, a body and a row of actions.
 *
 * The title is the dialog's name. While it is open, focus starts inside it and Tab cannot leave
 * it, even with no actions to focus. Esc and a click on the veil call
 * [onDismissRequest], and once it has gone focus goes back to [returnFocusTo]. It scales up
 * from 0.96 with a fade, or only fades under reduced motion. It is Material's `AlertDialog`, or
 * where overlays render in the page Material's own dialog container over the headless modal.
 *
 * @param[visible] Whether the dialog is open.
 * @param[onDismissRequest] Called when the dialog asks to close.
 * @param[title] The dialog's name, shown at the top and read as its pane title.
 * @param[modifier] Applied to the dialog panel.
 * @param[returnFocusTo] The trigger that opened the dialog. Attach it to the trigger with
 * `Modifier.focusRequester`.
 * @param[titleShown] Whether the title shows at the top. A dialog whose first control says what it
 * is, such as a search field, hides it and still goes by it.
 * @param[closeButton] Whether a close button sits at the end of the title row. It calls
 * [onDismissRequest] and takes its turn in Tab order, while focus still starts where it would without
 * it.
 * @param[maxWidth] The widest the panel grows. The window and the dialog's outer padding still narrow
 * it, and it never gets narrower than Material's smallest dialog.
 * @param[actions] The buttons along the bottom, the confirming one last, or null for no row of
 * actions.
 * @param[hero] A band across the top of the panel in place of the title row, such as the colour a
 * picker is picking, or null for the title row. It reaches the panel's edges and is clipped to its
 * rounded corners, so it brings its own room inside. The title and the close button do not show
 * with it, and [title] still names the dialog to assistive technology. A dialog with a hero takes
 * the panel that grows past Material's widest dialog, whatever [maxWidth] is, and focus starts past
 * it as it would past a close button.
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
    titleShown: Boolean = true,
    closeButton: Boolean = false,
    maxWidth: Dp = OverlayMetrics.dialogMaxWidth,
    actions: (@Composable RowScope.() -> Unit)? = null,
    hero: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val frame = DialogFrame(titleShown, closeButton, maxWidth, actions, hero)
    MaterialDialog(visible, onDismissRequest, title, returnFocusTo, modifier, frame, content)
}

/**
 * What a dialog shows around its body and how wide it grows, the title, the close button, the row of
 * actions, the hero and the panel's widest.
 *
 * @property[titleShown] Whether the title shows at the top.
 * @property[closeButton] Whether a close button sits at the end of the title row.
 * @property[maxWidth] The widest the panel grows.
 * @property[actions] The buttons along the bottom, or null for none.
 * @property[hero] The band across the top in place of the title row, or null for the title row.
 */
internal class DialogFrame(
    val titleShown: Boolean,
    val closeButton: Boolean,
    val maxWidth: Dp,
    val actions: (@Composable RowScope.() -> Unit)?,
    val hero: (@Composable () -> Unit)? = null,
) {
    /**
     * Whether the title row shows anything, which it never does under a hero.
     */
    val hasTitleRow: Boolean
        get() = hero == null && (titleShown || closeButton)

    /**
     * Whether the panel grows past Material's widest dialog, as a dialog with a hero always does.
     */
    val wide: Boolean
        get() = hero != null || maxWidth > OverlayMetrics.dialogMaxWidth
}

/**
 * The top of a dialog, the title drawn by [title] and, when [frame] asks for it, the close button at
 * the end. The title takes the room the button leaves, and the button reaches into the panel's
 * padding so its glyph lines up with the edge of the body. It draws nothing when neither shows, or
 * when a hero takes its place.
 *
 * The row is as tall as the title, so the body starts at the same place with or without the button.
 * The button sits centred on the title and the rest of its target reaches into the panel's padding
 * above and below, where it still takes clicks. With the title hidden the row is as tall as the glyph.
 */
@Composable
internal fun DialogTitleRow(
    frame: DialogFrame,
    onDismissRequest: () -> Unit,
    title: @Composable (titleModifier: Modifier) -> Unit,
) {
    if (!frame.hasTitleRow) return
    if (!frame.closeButton) {
        if (frame.titleShown) title(Modifier)
        return
    }
    val iconSize = LocalBuilderTokens.current.iconSize
    Layout(
        content = {
            if (frame.titleShown) title(Modifier.layoutId(TITLE_SLOT))
            BuilderIconButton(
                onClick = onDismissRequest,
                icon = IconId.Close,
                contentDescription = stringResource(Res.string.close),
                modifier = Modifier.layoutId(CLOSE_SLOT),
                emphasis = Emphasis.Subtle,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val button = measurables.first { measurable -> measurable.layoutId == CLOSE_SLOT }.measure(loose)
        val glyph = iconSize.roundToPx()
        // The room around the glyph at the end, which reaches past the row's end.
        val reach = ((button.width - glyph) / 2).coerceAtLeast(0)
        val buttonWidth = button.width - reach
        val bounded = constraints.hasBoundedWidth
        val titleMax = if (bounded) (constraints.maxWidth - buttonWidth).coerceAtLeast(0) else loose.maxWidth
        val titlePlaceable = measurables
            .firstOrNull { measurable -> measurable.layoutId == TITLE_SLOT }
            ?.measure(loose.copy(maxWidth = titleMax))
        val width = if (bounded) constraints.maxWidth else (titlePlaceable?.width ?: 0) + buttonWidth
        val height = constraints.constrainHeight(titlePlaceable?.height ?: glyph)
        layout(width, height) {
            titlePlaceable?.placeRelative(0, 0)
            button.placeRelative(width - buttonWidth, (height - button.height) / 2)
        }
    }
}

/**
 * The title's slot in [DialogTitleRow].
 */
private const val TITLE_SLOT = "title"

/**
 * The close button's slot in [DialogTitleRow].
 */
private const val CLOSE_SLOT = "close"

/**
 * The parts of a dialog over the headless modal that focus can start on, the [body] and the row of
 * [actions].
 */
internal class DialogStart {
    val body: FocusRequester = FocusRequester()
    val actions: FocusRequester = FocusRequester()
}

/**
 * The headless modal focuses the first thing inside as it opens, which with a close button or a hero
 * would be the button or something in the hero. So this moves focus on to the body, or else the
 * actions, as it would start without them. With nothing else to take it, focus stays where it is.
 */
@Composable
internal fun StartPastTheTop(
    frame: DialogFrame,
    start: DialogStart,
) {
    if (frame.hero == null && !frame.closeButton) return
    LaunchedEffect(Unit) {
        if (!start.body.requestFocus() && frame.actions != null) start.actions.requestFocus()
    }
}
