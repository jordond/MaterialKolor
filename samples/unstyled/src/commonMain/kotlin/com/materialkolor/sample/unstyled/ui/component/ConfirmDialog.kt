package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDialog
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private const val SCRIM_ALPHA = 0.32f
private const val PANEL_ENTER_SCALE = 0.94f

/**
 * A dialog that asks before something happens. It fades in over a scrim and a click on the scrim or Esc dismisses
 * it.
 *
 * It opens in the nearest `DialogHost`, in the app's own layout rather than a window of its own.
 *
 * @param[visible] Whether it is up.
 * @param[title] The question, which also names the dialog.
 * @param[body] What saying yes does.
 * @param[onDismiss] Called on a click outside the panel or on Esc.
 * @param[modifier] Applied to the panel.
 * @param[actions] The buttons, laid out from the end, the confirming one last.
 */
@Composable
internal fun ConfirmDialog(
    visible: Boolean,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val scrim = MaterialKolorTokens.scrim.color
    val shadow = MaterialKolorTokens.shadow.color
    UnstyledDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        overlay = {
            Scrim(scrimColor = scrim.copy(alpha = SCRIM_ALPHA), enter = fadeIn(), exit = fadeOut())
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.XLarge),
            contentAlignment = Alignment.Center,
        ) {
            DialogPanel(
                modifier = modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .shadow(elevation = 24.dp, shape = Shapes.Card, ambientColor = shadow, spotColor = shadow)
                    .clip(Shapes.Card)
                    .background(MaterialKolorTokens.surfaceContainerHigh.color)
                    .padding(Spacing.XLarge),
                paneTitle = title,
                enter = fadeIn() + scaleIn(initialScale = PANEL_ENTER_SCALE),
                exit = fadeOut() + scaleOut(targetScale = PANEL_ENTER_SCALE),
            ) {
                Column {
                    Text(text = title, style = TasksType.Heading)
                    Spacer(Modifier.height(Spacing.Small))
                    Text(text = body, color = MaterialKolorTokens.onSurfaceVariant.color)
                    Spacer(Modifier.height(Spacing.XLarge))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        content = actions,
                    )
                }
            }
        }
    }
}
