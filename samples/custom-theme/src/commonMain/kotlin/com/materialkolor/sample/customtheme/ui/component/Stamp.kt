package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun Stamp(
    text: String,
    accent: Accent,
    modifier: Modifier = Modifier,
    tilt: Float = 0f,
) {
    val colors = LocalAppColors.current

    Box(
        modifier = modifier
            .rotate(tilt)
            .ink(accent.container, colors)
            .border(1.5.dp, accent.content)
            .padding(2.dp)
            .border(1.dp, accent.content)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = AppType.Caption,
            color = accent.content,
        )
    }
}
