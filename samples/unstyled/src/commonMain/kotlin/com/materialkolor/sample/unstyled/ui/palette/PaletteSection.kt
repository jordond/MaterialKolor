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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.unstyled.theme.Gradient
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShadowStyle
import com.materialkolor.sample.unstyled.theme.ShadowTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shape
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

private val Gradients: List<ThemeToken<Gradient>> = listOf(
    GradientTokens.accent,
    GradientTokens.raised,
    GradientTokens.sunken,
    GradientTokens.backdrop,
)

private val Shadows: List<ThemeToken<ShadowStyle>> = listOf(
    ShadowTokens.resting,
    ShadowTokens.lifted,
    ShadowTokens.accent,
    ShadowTokens.inset,
)

private val ShapeSet: List<ThemeToken<Shape>> = listOf(
    ShapeTokens.control,
    ShapeTokens.card,
    ShapeTokens.dialog,
    ShapeTokens.pill,
    ShapeTokens.tag,
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
            text = "The theme tokens for the ${SampleCopy.label(seed)} seed. The colors are the MaterialKolorTokens " +
                "roles, each name written in the role meant to sit on it. The gradients and shadows point at those " +
                "roles, and the dark scheme swaps in its own.",
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

        PaletteGroup(title = "Gradients") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                for (token in Gradients) GradientTile(token = token, modifier = Modifier.weight(1f))
            }
        }

        PaletteGroup(title = "Shadows") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                for (token in Shadows) ShadowTile(token = token, modifier = Modifier.weight(1f))
            }
        }

        PaletteGroup(title = "Shapes") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                for (token in ShapeSet) ShapeTile(token = token, modifier = Modifier.weight(1f))
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
            .clip(ShapeTokens.card.shape)
            .border(width = 1.dp, color = MaterialKolorTokens.outlineVariant.color, shape = ShapeTokens.card.shape),
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
