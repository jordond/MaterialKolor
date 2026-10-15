package com.materialkolor.sample.material3.ui.palette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.ui.SampleCopy
import kotlin.reflect.KProperty1

private val AccentFamilies: List<List<Swatch>> = listOf(
    accentFamily(
        role = ColorScheme::primary,
        onRole = ColorScheme::onPrimary,
        container = ColorScheme::primaryContainer,
        onContainer = ColorScheme::onPrimaryContainer,
    ),
    accentFamily(
        role = ColorScheme::secondary,
        onRole = ColorScheme::onSecondary,
        container = ColorScheme::secondaryContainer,
        onContainer = ColorScheme::onSecondaryContainer,
    ),
    accentFamily(
        role = ColorScheme::tertiary,
        onRole = ColorScheme::onTertiary,
        container = ColorScheme::tertiaryContainer,
        onContainer = ColorScheme::onTertiaryContainer,
    ),
    accentFamily(
        role = ColorScheme::error,
        onRole = ColorScheme::onError,
        container = ColorScheme::errorContainer,
        onContainer = ColorScheme::onErrorContainer,
    ),
)

private val Surfaces: List<Swatch> = listOf(
    ColorScheme::surfaceDim,
    ColorScheme::surface,
    ColorScheme::surfaceBright,
    ColorScheme::surfaceContainerLowest,
    ColorScheme::surfaceContainerLow,
    ColorScheme::surfaceContainer,
    ColorScheme::surfaceContainerHigh,
    ColorScheme::surfaceContainerHighest,
).map { role -> Swatch(role = role, onRole = ColorScheme::onSurface) }

private val Outlines: List<Swatch> = listOf(
    Swatch(role = ColorScheme::outline, onRole = ColorScheme::surface),
    Swatch(role = ColorScheme::outlineVariant, onRole = ColorScheme::onSurface),
)

@Composable
internal fun PaletteSection(
    seed: SampleSeed,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "The MaterialTheme.colorScheme roles DynamicMaterialTheme generated from the " +
                "${SampleCopy.label(seed)} seed. Each name is written in the role meant to sit on it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PaletteGroup(title = "Accents") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (family in AccentFamilies) {
                    SwatchColumn(Modifier.weight(1f)) {
                        for ((index, swatch) in family.withIndex()) {
                            val height = if (index % 2 == 0) 64.dp else 44.dp
                            SwatchTile(
                                swatch = swatch,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(height),
                            )
                        }
                    }
                }
            }
        }

        PaletteGroup(title = "Surfaces") {
            SwatchColumn(Modifier.fillMaxWidth()) {
                for (swatch in Surfaces) SwatchStrip(swatch = swatch, modifier = Modifier.fillMaxWidth())
            }
        }

        PaletteGroup(title = "Outlines") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (swatch in Outlines) {
                    SwatchColumn(Modifier.weight(1f)) {
                        SwatchTile(
                            swatch = swatch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        content()
    }
}

@Composable
private fun SwatchColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(modifier = modifier, content = content)
}

private fun accentFamily(
    role: KProperty1<ColorScheme, Color>,
    onRole: KProperty1<ColorScheme, Color>,
    container: KProperty1<ColorScheme, Color>,
    onContainer: KProperty1<ColorScheme, Color>,
): List<Swatch> =
    listOf(
        Swatch(role = role, onRole = onRole),
        Swatch(role = onRole, onRole = role),
        Swatch(role = container, onRole = onContainer),
        Swatch(role = onContainer, onRole = container),
    )
