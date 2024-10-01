package com.materialkolor.builder.ui.home.preview.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.home.model.Theme
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.InnerDivider
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.InverseSurfacePair
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.MainColors
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.MiscColors
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.SectionDivider
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.Surface
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.SurfaceContainer
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.inverse
import com.materialkolor.builder.ui.theme.AppTypography
import com.materialkolor.builder.ui.theme.createThemeState

private enum class ThemeMode {
    Light,
    Dark,
}

@Composable
fun ThemeSection(
    settings: Settings,
    onCopyColor: (String, Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        LightTheme(settings, onClick = onCopyColor)
        DarkTheme(settings, onClick = onCopyColor)
    }
}

@Composable
private fun LightTheme(
    settings: Settings,
    modifier: Modifier = Modifier,
    onClick: (String, Color) -> Unit = { _, _ -> },
) {
    ThemeSection(ThemeMode.Light, createThemeState(settings, isDark = false), onClick, modifier)
}

@Composable
private fun DarkTheme(
    settings: Settings,
    modifier: Modifier = Modifier,
    onClick: (String, Color) -> Unit = { _, _ -> },
) {
    ThemeSection(ThemeMode.Dark, createThemeState(settings, isDark = true), onClick, modifier)
}

@Composable
private fun ThemeSection(
    theme: ThemeMode,
    state: DynamicMaterialThemeState,
    onClick: (String, Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    DynamicMaterialTheme(
        state = state,
        animate = true,
        typography = AppTypography,
    ) {
        ThemeSection(theme, onClick, modifier)
    }
}

@Composable
private fun ThemeSection(
    theme: ThemeMode,
    onClick: (String, Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "${theme.name} Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = SectionDivider),
            )

            CompositionLocalProvider(LocalTextStyle provides ThemeSectionDefaults.TextStyle) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SectionDivider),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SectionDivider),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(InnerDivider),
                        ) {
                            MainColors.forEach { group ->
                                ColorGroupContainer(group, onClick, modifier = Modifier.weight(1f))
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(InnerDivider),
                        ) {
                            Row {
                                Surface.forEach { themeColor ->
                                    ColorBox(
                                        themeColor = themeColor,
                                        onClick = onClick,
                                        lines = 4,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            Row {
                                SurfaceContainer.forEach { themeColor ->
                                    ColorBox(
                                        themeColor = themeColor,
                                        onClick = onClick,
                                        lines = 4,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            Row {
                                MiscColors.forEach { (themeColor, onColor) ->
                                    SingleLineColorBox(
                                        themeColor = themeColor,
                                        onClick = onClick,
                                        textColor = onColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(SectionDivider),
                        modifier = Modifier.weight(0.3f),
                    ) {
                        ColorGroupContainer(Theme.Groups.Error, onClick)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(InnerDivider),
                        ) {
                            ColorPairContainer(InverseSurfacePair, onClick)

                            SingleLineColorBox(
                                themeColor = Theme.Colors.InversePrimary,
                                onClick = onClick,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            SingleLineColorBox(
                                themeColor = Theme.Colors.Scrim,
                                onClick = onClick,
                                textColor = Theme.Colors.Scrim.color().inverse(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
