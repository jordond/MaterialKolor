package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A small read-only label on an [accent] fill, like the tag on a task.
 */
@Composable
internal fun Chip(
    text: String,
    accent: Accent,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Inner)
            .background(accent.container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = AppType.Caption,
            color = accent.content,
        )
    }
}
