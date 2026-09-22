package com.materialkolor.sample.customtheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.AppTheme
import com.materialkolor.sample.customtheme.theme.AppThemeMode
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
public fun SampleApp(seed: Color = Color(0xFF6750A4)) {
    var mode by remember { mutableStateOf(AppThemeMode.Light) }

    AppTheme(seed = seed, mode = mode) {
        val colors = LocalAppColors.current

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            ModeSwitch(
                mode = mode,
                onModeChange = { next -> mode = next },
            )

            SectionLabel("Accent families")
            for (family in colors.accentFamilies()) {
                AccentFamilyRow(family)
            }

            SectionLabel("Primary interaction")
            SwatchRow(
                swatches = listOf(
                    "primary" to colors.primary,
                    "pressed" to colors.primaryPressed,
                    "raised" to colors.primaryRaised,
                    "focusRing" to colors.focusRing,
                ),
            )

            SectionLabel("Surfaces")
            SwatchRow(
                swatches = listOf(
                    "sunken" to colors.surfaceSunken,
                    "surface" to colors.surface,
                    "raised" to colors.surfaceRaised,
                    "inverse" to colors.surfaceInverse,
                ),
            )

            SectionLabel("Ink and borders")
            SwatchRow(
                swatches = listOf(
                    "textStrong" to colors.textStrong,
                    "textMuted" to colors.textMuted,
                    "borderFaint" to colors.borderFaint,
                    "borderSoft" to colors.borderSoft,
                    "borderStrong" to colors.borderStrong,
                ),
            )

            SectionLabel("Drinks")
            SwatchRow(
                swatches = listOf(
                    "coffee" to colors.drinkCoffee,
                    "matcha" to colors.drinkMatcha,
                    "iced" to colors.drinkIced,
                    "tea" to colors.drinkTea,
                    "choc" to colors.drinkChoc,
                ),
            )
        }
    }
}

@Composable
private fun ModeSwitch(
    mode: AppThemeMode,
    onModeChange: (AppThemeMode) -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (option in AppThemeMode.entries) {
            val selected = option == mode
            BasicText(
                text = option.name,
                style = TextStyle(
                    color = if (selected) colors.onPrimary else colors.textMuted,
                    fontSize = 13.sp,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) colors.primary else colors.surfaceRaised)
                    .border(1.dp, colors.borderSoft, RoundedCornerShape(8.dp))
                    .clickable { onModeChange(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = LocalAppColors.current.textStrong, fontSize = 15.sp),
    )
}

@Composable
private fun AccentFamilyRow(family: AccentFamily) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.borderFaint, RoundedCornerShape(12.dp)),
    ) {
        FamilyCell(
            label = family.name,
            background = family.color,
            foreground = family.onColor,
            modifier = Modifier.weight(1f),
        )

        FamilyCell(
            label = "container",
            background = family.container,
            foreground = family.onContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FamilyCell(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = label,
        style = TextStyle(color = foreground, fontSize = 13.sp),
        modifier = modifier
            .background(background)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    )
}

@Composable
private fun SwatchRow(swatches: List<Pair<String, Color>>) {
    val colors = LocalAppColors.current

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for ((label, color) in swatches) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .border(1.dp, colors.borderSoft, RoundedCornerShape(10.dp)),
                )

                BasicText(
                    text = label,
                    style = TextStyle(color = colors.textMuted, fontSize = 11.sp),
                )
            }
        }
    }
}

/**
 * One accent family, flattened out of [AppColors] so the UI can loop over the seven of them.
 */
private data class AccentFamily(
    val name: String,
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

private fun AppColors.accentFamilies(): List<AccentFamily> =
    listOf(
        AccentFamily("primary", primary, onPrimary, primaryContainer, onPrimaryContainer),
        AccentFamily("secondary", secondary, onSecondary, secondaryContainer, onSecondaryContainer),
        AccentFamily("tertiary", tertiary, onTertiary, tertiaryContainer, onTertiaryContainer),
        AccentFamily("error", error, onError, errorContainer, onErrorContainer),
        AccentFamily("love", love, onLove, loveContainer, onLoveContainer),
        AccentFamily("cold", cold, onCold, coldContainer, onColdContainer),
        AccentFamily("warm", warm, onWarm, warmContainer, onWarmContainer),
    )
