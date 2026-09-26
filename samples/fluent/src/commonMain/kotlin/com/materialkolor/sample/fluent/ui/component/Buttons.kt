package com.materialkolor.sample.fluent.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text

@Composable
internal fun LabeledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    icon: ImageVector? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }

        Text(text = label)
    }

    if (accent) {
        AccentButton(
            onClick = onClick,
            modifier = modifier,
            disabled = !enabled,
            content = content,
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            disabled = !enabled,
            content = content,
        )
    }
}
