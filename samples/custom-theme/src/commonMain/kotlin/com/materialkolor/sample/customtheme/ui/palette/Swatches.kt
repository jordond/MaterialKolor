package com.materialkolor.sample.customtheme.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppShapes
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Text

/**
 * One accent family, flattened out of [AppColors] so the palette can loop over the seven of them.
 */
internal data class AccentFamily(
    val name: String,
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

/**
 * The four Material families and the three the app owns on top, in that order.
 */
internal fun AppColors.accentFamilies(): List<AccentFamily> =
    listOf(
        AccentFamily("primary", primary, onPrimary, primaryContainer, onPrimaryContainer),
        AccentFamily("secondary", secondary, onSecondary, secondaryContainer, onSecondaryContainer),
        AccentFamily("tertiary", tertiary, onTertiary, tertiaryContainer, onTertiaryContainer),
        AccentFamily("error", error, onError, errorContainer, onErrorContainer),
        AccentFamily("love", love, onLove, loveContainer, onLoveContainer),
        AccentFamily("cold", cold, onCold, coldContainer, onColdContainer),
        AccentFamily("warm", warm, onWarm, warmContainer, onWarmContainer),
    )

/**
 * One named color in a [SwatchRow].
 */
internal data class Swatch(
    val name: String,
    val color: Color,
)

/**
 * A family as two cells side by side, the accent and its container, each named in its own on-color.
 */
@Composable
internal fun AccentFamilyRow(family: AccentFamily) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .border(1.dp, colors.borderFaint, AppShapes.Card),
    ) {
        FamilyCell(
            label = family.name,
            background = family.color,
            foreground = family.onColor,
            modifier = Modifier.weight(1f),
        )

        FamilyCell(
            label = "${family.name} container",
            background = family.container,
            foreground = family.onContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Colors that nothing is written on, as named squares.
 */
@Composable
internal fun SwatchRow(swatches: List<Swatch>) {
    val colors = LocalAppColors.current

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (swatch in swatches) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(AppShapes.Control)
                        .background(swatch.color)
                        .border(1.dp, colors.borderSoft, AppShapes.Control),
                )

                Text(
                    text = swatch.name,
                    style = AppType.Caption,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun FamilyCell(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = AppType.Label,
        color = foreground,
        modifier = modifier
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
