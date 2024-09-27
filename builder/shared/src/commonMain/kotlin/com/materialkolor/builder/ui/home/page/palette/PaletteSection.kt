package com.materialkolor.builder.ui.home.page.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.builder.settings.model.KeyColor
import com.materialkolor.builder.ui.theme.LocalDynamicThemeState
import com.materialkolor.ktx.isLight
import com.materialkolor.ktx.m3Colors
import com.materialkolor.ktx.toneColor
import kotlinx.collections.immutable.persistentListOf

private val keyColors = (KeyColor.entries - KeyColor.Seed)

private val steps = persistentListOf(
    100, 99, 98, 95, 90, 80, 70, 60, 50, 40, 35, 30, 25, 20, 15, 10, 5, 0,
)

@Composable
fun PaletteSection(
    modifier: Modifier = Modifier,
) {
    val state = LocalDynamicThemeState.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        keyColors.forEach { palette ->
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = palette.name, fontWeight = FontWeight.Normal)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    steps.forEach { tone ->
                        val color = state.tone(palette, tone)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(color = color)
                                .weight(1f)
                                .aspectRatio(9f / 16f)
                                .heightIn(max = 150.dp),
                        ) {
                            val textColor = if (color.isLight()) Color.Black else Color.White
                            Text(
                                text = "$tone",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = textColor,
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicMaterialThemeState.tone(palette: KeyColor, tone: Int): Color {
    // TODO: Once MaterialKolor is released, replace this with referencing the m3Colors itself.
    val colors = m3Colors
    val scheme = dynamicScheme
    return remember(this, palette, tone) {
        when (palette) {
            KeyColor.Primary -> colors.primary().palette(scheme).toneColor(tone)
            KeyColor.Secondary -> colors.secondary().palette(scheme).toneColor(tone)
            KeyColor.Tertiary -> colors.tertiary().palette(scheme).toneColor(tone)
            KeyColor.Neutral -> colors.neutralPaletteKeyColor().palette(scheme).toneColor(tone)
            KeyColor.NeutralVariant ->
                colors.neutralVariantPaletteKeyColor().palette(scheme).toneColor(tone)
            KeyColor.Error -> colors.error().palette(scheme).toneColor(tone)
            else -> Color.Unspecified
        }
    }
}
