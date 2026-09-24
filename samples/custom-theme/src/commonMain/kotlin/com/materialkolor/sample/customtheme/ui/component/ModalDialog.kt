package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * A dialog that asks one question, with a [title], a line of [body] and the answers in [buttons].
 *
 * It sits on the platform `Dialog`, so it gets the window layer, focus, Esc and outside clicks for free, and draws a
 * panel from the theme on top.
 *
 * @param[modifier] Applied to the panel.
 * @param[buttons] The answers, laid out from the end, so the last one sits in the corner.
 */
@Composable
internal fun ModalDialog(
    title: String,
    body: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = AppShapes.Card

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties(scrim = colors.scrim.copy(alpha = SCRIM_ALPHA)),
    ) {
        Column(
            modifier = modifier
                .width(400.dp)
                .shadow(elevation = 24.dp, shape = shape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .clip(shape)
                .background(colors.surfaceRaised)
                .border(1.dp, colors.borderFaint, shape)
                .padding(24.dp),
        ) {
            Text(
                text = title,
                style = AppType.Heading,
                color = colors.textStrong,
            )

            Text(
                text = body,
                style = AppType.Body,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                content = buttons,
            )
        }
    }
}

/** Enough scrim to push the page back without hiding it. */
private const val SCRIM_ALPHA = 0.4f
