package com.materialkolor.sample.unstyled.ui.palette

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private val AccentFamilies: List<List<Swatch>> = listOf(
    accentFamily(
        role = MaterialKolorTokens.primary,
        onRole = MaterialKolorTokens.onPrimary,
        container = MaterialKolorTokens.primaryContainer,
        onContainer = MaterialKolorTokens.onPrimaryContainer,
    ),
    accentFamily(
        role = MaterialKolorTokens.secondary,
        onRole = MaterialKolorTokens.onSecondary,
        container = MaterialKolorTokens.secondaryContainer,
        onContainer = MaterialKolorTokens.onSecondaryContainer,
    ),
    accentFamily(
        role = MaterialKolorTokens.tertiary,
        onRole = MaterialKolorTokens.onTertiary,
        container = MaterialKolorTokens.tertiaryContainer,
        onContainer = MaterialKolorTokens.onTertiaryContainer,
    ),
    accentFamily(
        role = MaterialKolorTokens.error,
        onRole = MaterialKolorTokens.onError,
        container = MaterialKolorTokens.errorContainer,
        onContainer = MaterialKolorTokens.onErrorContainer,
    ),
)

private val Surfaces: List<Swatch> = listOf(
    MaterialKolorTokens.surfaceDim,
    MaterialKolorTokens.surface,
    MaterialKolorTokens.surfaceBright,
    MaterialKolorTokens.surfaceContainerLowest,
    MaterialKolorTokens.surfaceContainerLow,
    MaterialKolorTokens.surfaceContainer,
    MaterialKolorTokens.surfaceContainerHigh,
    MaterialKolorTokens.surfaceContainerHighest,
).map { role -> Swatch(role = role, onRole = MaterialKolorTokens.onSurface) }

private val Outlines: List<Swatch> = listOf(
    Swatch(role = MaterialKolorTokens.outline, onRole = MaterialKolorTokens.surface),
    Swatch(role = MaterialKolorTokens.outlineVariant, onRole = MaterialKolorTokens.onSurface),
)

@Composable
internal fun PaletteSection(
    seed: SampleSeed,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.XLarge),
    ) {
        Text(
            text = "The MaterialKolorTokens roles generated from the ${SampleCopy.label(seed)} seed. " +
                "Each name is written in the role meant to sit on it.",
            color = MaterialKolorTokens.onSurfaceVariant.color,
        )

        PaletteGroup(title = "Accents") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(text = title, style = TasksType.Caption, color = MaterialKolorTokens.onSurfaceVariant.color)

        content()
    }
}

@Composable
private fun SwatchColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        content = content,
        modifier = modifier
            .clip(Shapes.Card)
            .border(width = 1.dp, color = MaterialKolorTokens.outlineVariant.color, shape = Shapes.Card),
    )
}

private fun accentFamily(
    role: ThemeToken<Color>,
    onRole: ThemeToken<Color>,
    container: ThemeToken<Color>,
    onContainer: ThemeToken<Color>,
): List<Swatch> =
    listOf(
        Swatch(role = role, onRole = onRole),
        Swatch(role = onRole, onRole = role),
        Swatch(role = container, onRole = onContainer),
        Swatch(role = onContainer, onRole = container),
    )
