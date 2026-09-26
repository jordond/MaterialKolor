package com.materialkolor.sample.material3.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal actual fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    val thumb = MaterialTheme.colorScheme.onSurfaceVariant

    VerticalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(state),
        style = ScrollbarStyle(
            minimalHeight = 32.dp,
            thickness = 8.dp,
            shape = CircleShape,
            hoverDurationMillis = 300,
            unhoverColor = thumb.copy(alpha = 0.35f),
            hoverColor = thumb.copy(alpha = 0.7f),
        ),
    )
}
