package com.materialkolor.sample.customtheme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.customtheme.ui.component.drawRegistrationMark
import com.materialkolor.sample.customtheme.ui.component.halftone
import com.materialkolor.sample.customtheme.ui.component.ink

@Composable
internal fun ColorBar(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val inks = listOf(colors.ink, colors.primary, colors.pink, colors.blue, colors.yellow)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        RegistrationTarget()

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (ink in inks) {
                Box(Modifier.size(PatchSize).ink(ink, colors))
                for (screen in Screens) {
                    Box(Modifier.size(PatchSize).halftone(color = ink, colors = colors, cell = 4.dp) { _, _ -> screen })
                }
            }
        }

        Text(
            text = "MATERIALKOLOR",
            style = AppType.Caption,
            color = colors.inkSoft,
            modifier = Modifier.weight(1f),
        )

        RegistrationTarget()
    }
}

@Composable
private fun RegistrationTarget() {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .size(PatchSize)
            .drawBehind { drawRegistrationMark(color = colors.ink, center = center, radius = size.minDimension / 3) },
    )
}

private val PatchSize = 16.dp
private val Screens = listOf(0.5f, 0.2f)
