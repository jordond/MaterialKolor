package com.materialkolor.sample.customtheme.ui.palette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.GroupLabel

/**
 * Every slot [AppColors] generates for the current seed and mode, grouped the way the app uses them.
 */
@Composable
internal fun PaletteSection(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        PaletteGroup(title = "Accent families") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (family in colors.accentFamilies()) {
                    AccentFamilyRow(family = family)
                }
            }
        }

        PaletteGroup(title = "Primary interaction") {
            SwatchRow(
                swatches = listOf(
                    Swatch("primary", colors.primary),
                    Swatch("pressed", colors.primaryPressed),
                    Swatch("raised", colors.primaryRaised),
                    Swatch("focusRing", colors.focusRing),
                ),
            )
        }

        PaletteGroup(title = "Surfaces") {
            SwatchRow(
                swatches = listOf(
                    Swatch("sunken", colors.surfaceSunken),
                    Swatch("surface", colors.surface),
                    Swatch("raised", colors.surfaceRaised),
                    Swatch("inverse", colors.surfaceInverse),
                ),
            )
        }

        PaletteGroup(title = "Ink and borders") {
            SwatchRow(
                swatches = listOf(
                    Swatch("textStrong", colors.textStrong),
                    Swatch("textMuted", colors.textMuted),
                    Swatch("borderFaint", colors.borderFaint),
                    Swatch("borderSoft", colors.borderSoft),
                    Swatch("borderStrong", colors.borderStrong),
                ),
            )
        }

        PaletteGroup(title = "Drinks") {
            SwatchRow(
                swatches = listOf(
                    Swatch("coffee", colors.drinkCoffee),
                    Swatch("matcha", colors.drinkMatcha),
                    Swatch("iced", colors.drinkIced),
                    Swatch("tea", colors.drinkTea),
                    Swatch("choc", colors.drinkChoc),
                ),
            )
        }
    }
}

@Composable
private fun PaletteGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GroupLabel(text = title)
        content()
    }
}
