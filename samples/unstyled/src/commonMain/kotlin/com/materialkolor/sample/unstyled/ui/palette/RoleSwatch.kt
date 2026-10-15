package com.materialkolor.sample.unstyled.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Text
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color

@Immutable
internal data class Swatch(
    val role: ThemeToken<Color>,
    val onRole: ThemeToken<Color>,
)

@Composable
internal fun SwatchTile(
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    val fill = swatch.role.color
    ProvideContentColor(swatch.onRole.color) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .background(fill)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        ) {
            SwatchName(swatch)
            SwatchHex(fill)
        }
    }
}

@Composable
internal fun SwatchStrip(
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    val fill = swatch.role.color
    ProvideContentColor(swatch.onRole.color) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(fill)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        ) {
            SwatchName(swatch, Modifier.weight(1f))
            SwatchHex(fill)
        }
    }
}

@Composable
private fun SwatchName(
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    Text(
        text = swatch.role.name,
        modifier = modifier,
        style = TasksType.Small,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SwatchHex(fill: Color) {
    Text(text = fill.hex(), style = TasksType.Small, maxLines = 1)
}

private fun Color.hex(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()
