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

/**
 * A MaterialKolor role to show, and the role its name is written in.
 *
 * @property[role] The token whose color fills the swatch.
 * @property[onRole] The token the label is written in, the role meant to sit on [role].
 */
@Immutable
internal data class Swatch(
    val role: ThemeToken<Color>,
    val onRole: ThemeToken<Color>,
)

/**
 * A block of the role's color with its token name over its hex value.
 */
@Composable
internal fun SwatchTile(
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    val fill = swatch.role.color
    ProvideContentColor(swatch.onRole.color) {
        Column(
            modifier = modifier
                .background(fill)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            SwatchName(swatch)
            SwatchHex(fill)
        }
    }
}

/**
 * A band of the role's color with its token name at the start and its hex value at the end.
 */
@Composable
internal fun SwatchStrip(
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    val fill = swatch.role.color
    ProvideContentColor(swatch.onRole.color) {
        Row(
            modifier = modifier
                .background(fill)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
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

/** The color as `#RRGGBB`. */
private fun Color.hex(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()
