package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.close
import com.materialkolor.builder.kit.headless.HeadlessInsetSheet
import com.materialkolor.builder.kit.headless.HeadlessInsetSheetHost
import com.materialkolor.builder.kit.headless.modalTitle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * The region an inset sheet rises in, with [sheet] laid over [content] in the same bounds.
 *
 * Put it around what scrolls rather than inside it, so a [BuilderInsetSheet] in [sheet] stays put
 * while [content] scrolls under it. While the sheet is open [content] leaves the semantics tree,
 * the way the page under a modal does.
 *
 * @param[sheet] The sheet, usually a [BuilderInsetSheet]. It is laid out in the host's bounds.
 * @param[modifier] Applied to the region.
 * @param[content] What the sheet rises over, such as a [BuilderScrollArea].
 */
@Composable
public fun BuilderInsetSheetHost(
    sheet: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    HeadlessInsetSheetHost(modifier, sheet, content)
}

/**
 * A modal sheet that rises from the bottom of the region it stands in and never leaves it, such
 * as the poster's Fine-tune sheet.
 *
 * It fills the width of its region and is as tall as what it holds, up to [maxHeight] and never
 * taller than the region, so what sits above it stays in view. A veil covers the rest of the
 * region. A handle, the [title] and a close button sit along its top, and the body under them
 * scrolls. It keeps focus inside while it is open, closes on Esc, on the veil and on the close
 * button, and hands focus back to [returnFocusTo] once it has gone. It slides up, or only fades
 * under reduced motion.
 *
 * It is drawn in the surrounding panel colour with the skin's own corners, edge and shadow, so
 * inside an `InversePosterSurface` it stands on the poster in the poster's ink. Put it in the
 * `sheet` slot of a [BuilderInsetSheetHost], which keeps it clear of anything that scrolls.
 *
 * @param[open] Whether the sheet is open.
 * @param[onDismiss] Called when the sheet asks to close.
 * @param[title] The sheet's name, shown in its header and read as its pane title.
 * @param[modifier] Applied to the sheet panel.
 * @param[returnFocusTo] The trigger that opened the sheet.
 * @param[maxHeight] The tallest the sheet gets.
 * @param[closeLabel] What the close button says to assistive technology.
 * @param[content] The sheet's body, which scrolls when it holds more than fits.
 */
@Composable
public fun BuilderInsetSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    maxHeight: Dp = Dp.Infinity,
    closeLabel: String = stringResource(Res.string.close),
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = overlayStyle(LocalSkin.current.library)
    HeadlessInsetSheet(
        open = open,
        onDismiss = onDismiss,
        title = title,
        maxHeight = maxHeight,
        style = style,
        returnFocusTo = returnFocusTo,
        modifier = modifier,
    ) {
        val spacing = LocalBuilderTokens.current.spacing
        val padding = OverlayMetrics.panelPadding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = padding, end = spacing.large, top = spacing.small, bottom = spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            BuilderText(
                text = title,
                modifier = Modifier.weight(1f).modalTitle(),
                style = BuilderTextStyle.Title,
                color = style.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BuilderIconButton(
                onClick = onDismiss,
                icon = IconId.ChevronDown,
                contentDescription = closeLabel,
                emphasis = Emphasis.Secondary,
            )
        }
        BuilderScrollArea(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = padding, end = padding, bottom = padding),
            fitContent = true,
            scrollbarInGutter = true,
            content = content,
        )
    }
}
