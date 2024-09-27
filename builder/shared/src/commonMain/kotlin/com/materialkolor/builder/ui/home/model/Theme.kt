package com.materialkolor.builder.ui.home.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.builder.ui.theme.LocalDynamicThemeState

object Theme {

    object Colors {
        val Primary = ThemeColor(
            title = "Primary",
            swatchNumber = "P-40",
            color = c { MaterialTheme.colorScheme.primary },
        )

        val OnPrimary = ThemeColor(
            title = "On Primary",
            swatchNumber = "P-100",
            color = c { MaterialTheme.colorScheme.onPrimary },
        )

        val PrimaryContainer = ThemeColor(
            title = "Primary Container",
            swatchNumber = "P-90",
            color = c { MaterialTheme.colorScheme.primaryContainer },
        )

        val OnPrimaryContainer = ThemeColor(
            title = "On Primary Container",
            swatchNumber = "P-10",
            color = c { MaterialTheme.colorScheme.onPrimaryContainer },
        )

        val Secondary = ThemeColor(
            title = "Secondary",
            swatchNumber = "S-40",
            color = c { MaterialTheme.colorScheme.secondary },
        )

        val OnSecondary = ThemeColor(
            title = "On Secondary",
            swatchNumber = "S-100",
            color = c { MaterialTheme.colorScheme.onSecondary },
        )

        val SecondaryContainer = ThemeColor(
            title = "Secondary Container",
            swatchNumber = "S-90",
            color = c { MaterialTheme.colorScheme.secondaryContainer },
        )

        val OnSecondaryContainer = ThemeColor(
            title = "On Secondary Container",
            swatchNumber = "S-10",
            color = c { MaterialTheme.colorScheme.onSecondaryContainer },
        )

        val Tertiary = ThemeColor(
            title = "Tertiary",
            swatchNumber = "T-40",
            color = c { MaterialTheme.colorScheme.tertiary },
        )

        val OnTertiary = ThemeColor(
            title = "On Tertiary",
            swatchNumber = "T-100",
            color = c { MaterialTheme.colorScheme.onTertiary },
        )

        val TertiaryContainer = ThemeColor(
            title = "Tertiary Container",
            swatchNumber = "T-90",
            color = c { MaterialTheme.colorScheme.tertiaryContainer },
        )

        val OnTertiaryContainer = ThemeColor(
            title = "On Tertiary Container",
            swatchNumber = "T-10",
            color = c { MaterialTheme.colorScheme.onTertiaryContainer },
        )

        val Error = ThemeColor(
            title = "Error",
            swatchNumber = "E-40",
            color = c { MaterialTheme.colorScheme.error },
        )

        val OnError = ThemeColor(
            title = "On Error",
            swatchNumber = "E-100",
            color = c { MaterialTheme.colorScheme.onError },
        )

        val ErrorContainer = ThemeColor(
            title = "Error Container",
            swatchNumber = "E-90",
            color = c { MaterialTheme.colorScheme.errorContainer },
        )

        val OnErrorContainer = ThemeColor(
            title = "On Error Container",
            swatchNumber = "E-10",
            color = c { MaterialTheme.colorScheme.onErrorContainer },
        )

        val SurfaceDim = ThemeColor(
            title = "Surface Dim",
            swatchNumber = "N-87",
            color = c { MaterialTheme.colorScheme.surfaceDim },
        )

        val Surface = ThemeColor(
            title = "Surface",
            swatchNumber = "N-98",
            color = c { MaterialTheme.colorScheme.surface },
        )

        val SurfaceBright = ThemeColor(
            title = "Surface Bright",
            swatchNumber = "N-98",
            color = c { MaterialTheme.colorScheme.surfaceBright },
        )

        val SurfaceContainerLowest = ThemeColor(
            title = "Surface Container Lowest",
            swatchNumber = "N-100",
            color = c { MaterialTheme.colorScheme.surfaceContainerLowest },
        )

        val SurfaceContainerLow = ThemeColor(
            title = "Surface Container Low",
            swatchNumber = "N-96",
            color = c { MaterialTheme.colorScheme.surfaceContainerLow },
        )

        val SurfaceContainer = ThemeColor(
            title = "Surface Container",
            swatchNumber = "N-94",
            color = c { MaterialTheme.colorScheme.surfaceContainer },
        )

        val SurfaceContainerHigh = ThemeColor(
            title = "Surface Container High",
            swatchNumber = "N-92",
            color = c { MaterialTheme.colorScheme.surfaceContainerHigh },
        )

        val SurfaceContainerHighest = ThemeColor(
            title = "Surface Container Highest",
            swatchNumber = "N-90",
            color = c { MaterialTheme.colorScheme.surfaceContainerHighest },
        )

        val OnSurface = ThemeColor(
            title = "On Surface",
            swatchNumber = "N-10",
            color = c { MaterialTheme.colorScheme.onSurface },
        )

        val OnSurfaceVariant = ThemeColor(
            title = "On Surface Variant",
            swatchNumber = "NV-30",
            color = c { MaterialTheme.colorScheme.onSurfaceVariant },
        )

        val Outline = ThemeColor(
            title = "Outline",
            swatchNumber = "NV-50",
            color = c { MaterialTheme.colorScheme.outline },
        )

        val OutlineVariant = ThemeColor(
            title = "Outline Variant",
            swatchNumber = "NV-80",
            color = c { MaterialTheme.colorScheme.outlineVariant },
        )

        val InverseSurface = ThemeColor(
            title = "Inverse Surface",
            swatchNumber = "N-20",
            color = c { MaterialTheme.colorScheme.inverseSurface },
        )

        val InverseOnSurface = ThemeColor(
            title = "Inverse On Surface",
            swatchNumber = "N-95",
            color = c { MaterialTheme.colorScheme.inverseOnSurface },
        )

        val InversePrimary = ThemeColor(
            title = "Inverse Primary",
            swatchNumber = "P-80",
            color = c { MaterialTheme.colorScheme.inversePrimary },
        )

        val Scrim = ThemeColor(
            title = "Scrim",
            swatchNumber = "N-0",
            color = c { MaterialTheme.colorScheme.scrim },
        )
    }

    object Groups {
        val Primary = ThemeGroup(
            main = ThemePair(Colors.Primary, Colors.OnPrimary),
            container = ThemePair(Colors.PrimaryContainer, Colors.OnPrimaryContainer),
        )

        val Secondary = ThemeGroup(
            main = ThemePair(Colors.Secondary, Colors.OnSecondary),
            container = ThemePair(Colors.SecondaryContainer, Colors.OnSecondaryContainer),
        )

        val Tertiary = ThemeGroup(
            main = ThemePair(Colors.Tertiary, Colors.OnTertiary),
            container = ThemePair(Colors.TertiaryContainer, Colors.OnTertiaryContainer),
        )

        val Error = ThemeGroup(
            main = ThemePair(Colors.Error, Colors.OnError),
            container = ThemePair(Colors.ErrorContainer, Colors.OnErrorContainer),
        )
    }
}

private fun c(
    selector: @Composable DynamicMaterialThemeState.() -> Color,
): @Composable () -> Color {
    return {
        val colors = LocalDynamicThemeState.current
        colors.selector()
    }
}