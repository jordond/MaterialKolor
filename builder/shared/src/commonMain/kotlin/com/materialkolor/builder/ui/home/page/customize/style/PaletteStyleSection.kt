package com.materialkolor.builder.ui.home.page.customize.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PaletteStyleSection(
    selected: PaletteStyle,
    onUpdate: (PaletteStyle) -> Unit,
    modifier: Modifier = Modifier,
    styles: PersistentList<PaletteStyle> = PaletteStyle.entries.toPersistentList(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Variant - ${selected.name()}",
            style = MaterialTheme.typography.titleLarge,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            styles.forEach { style ->
                FilterChip(
                    selected = style == selected,
                    label = { Text(text = style.name()) },
                    onClick = { onUpdate(style) },
                )
            }
        }

        Text(
            text = selected.description(),
            style = MaterialTheme.typography.bodySmall,
            minLines = 2,
        )
    }
}

@Composable
private fun PaletteStyle.name() = remember(this) {
    when (this) {
        PaletteStyle.TonalSpot -> "Tonal Spot"
        PaletteStyle.Neutral -> "Neutral"
        PaletteStyle.Vibrant -> "Vibrant"
        PaletteStyle.Expressive -> "Expressive"
        PaletteStyle.Rainbow -> "Rainbow"
        PaletteStyle.FruitSalad -> "Fruit Salad"
        PaletteStyle.Monochrome -> "Monochrome"
        PaletteStyle.Fidelity -> "Fidelity"
        PaletteStyle.Content -> "Content"
    }
}

@Composable
private fun PaletteStyle.description() = remember(this) {
    when (this) {
        PaletteStyle.TonalSpot -> "A calm theme, sedated colors that aren't particularly chromatic."
        PaletteStyle.Neutral -> "A theme that's slightly more chromatic than monochrome, which is purely black / white / gray."
        PaletteStyle.Vibrant -> "A loud theme, colorfulness is maximum for Primary palette, increased for others."
        PaletteStyle.Expressive -> "A playful theme - the source color's hue does not appear in the theme."
        PaletteStyle.Rainbow -> "A playful theme - the source color's hue does not appear in the theme."
        PaletteStyle.FruitSalad -> "A playful theme - the source color's hue does not appear in the theme."
        PaletteStyle.Monochrome -> "A monochrome theme, colors are purely black / white / gray."
        PaletteStyle.Fidelity -> "A scheme that places the source color in Scheme.primaryContainer."
        PaletteStyle.Content -> "Primary Container is the source color, adjusted for color relativity"
    }
}