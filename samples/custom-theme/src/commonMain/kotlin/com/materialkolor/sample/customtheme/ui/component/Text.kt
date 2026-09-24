package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * One line or paragraph of text in a style from [AppType] and a color from `AppColors`.
 */
@Composable
internal fun Text(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textDecoration = textDecoration),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * The small muted heading over or beside a group of controls.
 */
@Composable
internal fun GroupLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = AppType.Caption,
        color = LocalAppColors.current.textMuted,
        modifier = modifier,
    )
}
