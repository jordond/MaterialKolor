package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalContentColor
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledIcon
import com.materialkolor.sample.unstyled.theme.ControlHeight
import com.materialkolor.sample.unstyled.theme.IconSize
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

internal enum class ButtonStyle {
    Filled,
    Quiet,
    Danger,
}

@Composable
internal fun Button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container = when (style) {
        ButtonStyle.Filled -> MaterialKolorTokens.primary.color
        ButtonStyle.Quiet -> Color.Transparent
        ButtonStyle.Danger -> MaterialKolorTokens.error.color
    }
    val content = when (style) {
        ButtonStyle.Filled -> MaterialKolorTokens.onPrimary.color
        ButtonStyle.Quiet -> MaterialKolorTokens.primary.color
        ButtonStyle.Danger -> MaterialKolorTokens.onError.color
    }

    ProvideContentColor(content) {
        UnstyledButton(
            onClick = onClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = Spacing.Large),
            indication = LocalIndication.current,
            interactionSource = interactionSource,
            modifier = modifier
                .heightIn(min = ControlHeight)
                .pressScale(interactionSource)
                .alpha(enabledAlpha(enabled))
                .controlFocusRing(interactionSource, Shapes.Control)
                .clip(Shapes.Control)
                .background(container),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    UnstyledIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize),
                        tint = content,
                    )
                }

                Text(text = label, style = TasksType.Label, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun IconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    ProvideContentColor(MaterialKolorTokens.onSurfaceVariant.color) {
        UnstyledButton(
            onClick = onClick,
            indication = LocalIndication.current,
            interactionSource = interactionSource,
            modifier = modifier
                .size(36.dp)
                .pressScale(interactionSource)
                .controlFocusRing(interactionSource, Shapes.Round)
                .clip(Shapes.Round),
        ) {
            UnstyledIcon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(IconSize),
            )
        }
    }
}
