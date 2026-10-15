package com.materialkolor.sample.material3.ui.palette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.toHex
import kotlin.reflect.KProperty1

@Immutable
internal data class Swatch(
    val role: KProperty1<ColorScheme, Color>,
    val onRole: KProperty1<ColorScheme, Color>,
)

@Composable
internal fun SwatchTile(
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = swatch.role.get(scheme)

    Surface(
        color = fill,
        contentColor = swatch.onRole.get(scheme),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
    val scheme = MaterialTheme.colorScheme
    val fill = swatch.role.get(scheme)

    Surface(
        color = fill,
        contentColor = swatch.onRole.get(scheme),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun SwatchHex(fill: Color) {
    Text(
        text = fill.toHex().uppercase(),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}
