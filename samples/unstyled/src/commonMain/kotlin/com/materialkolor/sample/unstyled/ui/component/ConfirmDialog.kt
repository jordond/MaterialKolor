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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.UnstyledIcon
import com.materialkolor.sample.unstyled.theme.ShadowTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shadow
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.unstyled.MaterialKolorTokens

private const val SCRIM_ALPHA = 0.4f
private const val PANEL_ENTER_SCALE = 0.9f

@Composable
internal fun ConfirmDialog(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val scrim = MaterialKolorTokens.scrim.color
    UnstyledDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        overlay = {
            Scrim(scrimColor = scrim.copy(alpha = SCRIM_ALPHA), enter = fadeIn(), exit = fadeOut())
        },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.XLarge),
        ) {
            DialogPanel(
                enter = fadeIn() + scaleIn(initialScale = PANEL_ENTER_SCALE),
                exit = fadeOut() + scaleOut(targetScale = PANEL_ENTER_SCALE),
                modifier = modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .raised(shape = ShapeTokens.dialog.shape, shadow = ShadowTokens.lifted.shadow)
                    .padding(Spacing.XLarge),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialKolorTokens.errorContainer.color, ShapeTokens.pill.shape),
                    ) {
                        UnstyledIcon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialKolorTokens.onErrorContainer.color,
                        )
                    }

                    Spacer(Modifier.height(Spacing.Large))

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
