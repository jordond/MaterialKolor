package com.materialkolor.sample.customtheme.ui.palette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.theme.LocalAppPalettes
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Text

@Composable
internal fun PaletteSection(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val palettes = LocalAppPalettes.current

    Column(
        verticalArrangement = Arrangement.spacedBy(44.dp),
        modifier = modifier,
    ) {
        PaletteGroup(
            title = "Inks",
            note = "Each ink is a tonal ramp built from its seed and pulled towards yours. The theme prints one tone " +
                "off each ramp, marked with a target, and the halftone screens under it make the lighter tints.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                for (ink in palettes.inks(colors)) {
                    InkRow(ink = ink)
                }
            }
        }

        PaletteGroup(
            title = "Overprint",
            note = "Where two inks land on each other they mix. On light stock they multiply like real ink, and on " +
                "the dark stock they add up and glow.",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InkVenn()
                OverprintGrid(modifier = Modifier.weight(1f))
            }
        }

        PaletteGroup(
            title = "Stock and roles",
            note = "The paper comes off a cream ramp and the key ink off the primary ramp. The buttons and the " +
                "scrim use Material roles as they are.",
        ) {
            StockRow()
        }
    }
}

@Composable
private fun PaletteGroup(
    title: String,
    note: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                style = AppType.Display,
                color = colors.ink,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .drawBehind {
                        drawLine(
                            colors.ink,
                            Offset(0f, size.height / 2),
                            Offset(size.width, size.height / 2),
                            size.height,
                        )
                    },
            )
        }

        Text(
            text = note,
            style = AppType.Body,
            color = colors.inkSoft,
            modifier = Modifier.fillMaxWidth(NOTE_WIDTH),
        )

        content()
    }
}

private const val NOTE_WIDTH = 0.8f
