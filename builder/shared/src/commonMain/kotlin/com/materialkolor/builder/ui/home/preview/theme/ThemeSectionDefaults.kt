package com.materialkolor.builder.ui.home.preview.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.home.model.FixedThemeGroup
import com.materialkolor.builder.ui.home.model.Theme
import com.materialkolor.builder.ui.home.model.ThemePair
import com.materialkolor.ktx.isLight
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

object ThemeSectionDefaults {

    val SectionDivider = 16.dp
    val InnerDivider = 6.dp
    val BoxPadding = 12.dp
    val TextStyle
        @Composable
        get() = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light)

    val MainColors =
        persistentListOf(Theme.Groups.Primary, Theme.Groups.Secondary, Theme.Groups.Tertiary)

    val Surface =
        persistentListOf(Theme.Colors.SurfaceDim, Theme.Colors.Surface, Theme.Colors.SurfaceBright)

    val SurfaceContainer = persistentListOf(
        Theme.Colors.SurfaceContainerLowest,
        Theme.Colors.SurfaceContainerLow,
        Theme.Colors.SurfaceContainer,
        Theme.Colors.SurfaceContainerHigh,
        Theme.Colors.SurfaceContainerHighest,
    )

    val MiscColors
        @Composable
        get() = persistentListOf(
            Theme.Colors.OnSurface to MaterialTheme.colorScheme.surface,
            Theme.Colors.OnSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant,
            Theme.Colors.Outline to MaterialTheme.colorScheme.outline.inverse(),
            Theme.Colors.OutlineVariant to MaterialTheme.colorScheme.outlineVariant.inverse(),
        )

    val InverseSurfacePair = ThemePair(
        Theme.Colors.InverseSurface,
        Theme.Colors.InverseOnSurface,
    )

    val FixedColors: PersistentList<FixedThemeGroup> =
        persistentListOf(
            Theme.Groups.PrimaryFixed,
            Theme.Groups.SecondaryFixed,
            Theme.Groups.TertiaryFixed,
        )

    @Composable
    fun Color.inverse(): Color {
        return if (isLight()) Color.Black else Color.White
    }
}
