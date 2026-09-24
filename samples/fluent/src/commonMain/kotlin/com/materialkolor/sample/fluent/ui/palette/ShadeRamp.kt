package com.materialkolor.sample.fluent.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.toHex
import com.materialkolor.sample.fluent.theme.readableOn
import io.github.composefluent.FluentTheme
import io.github.composefluent.Shades
import io.github.composefluent.component.Text
import io.github.composefluent.generateShades

/**
 * The seven [shades] side by side, darkest first.
 *
 * @param[labelsFrom] The ramp the label colors are worked out from, or null to leave the tiles bare. Pass the ramp a
 *   fade is heading to rather than [shades] mid fade, so the contrast search runs once per seed and not every frame.
 */
@Composable
internal fun ShadeRamp(
    shades: Shades,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    labelsFrom: Shades? = null,
) {
    // Each slot keeps its tone from seed to seed, so a label picked for the target ramp reads on its tile all
    // through a fade.
    val onColors = remember(labelsFrom) { labelsFrom?.named()?.map { (_, color) -> color.readableOn() } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(FluentTheme.shapes.overlay),
    ) {
        shades.named().forEachIndexed { index, (name, color) ->
            ShadeTile(
                name = name,
                color = color,
                onColor = onColors?.get(index),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
internal fun WithoutAdapter(
    seed: Color,
    modifier: Modifier = Modifier,
) {
    // generateShades is a lookup with a single entry, so every seed other than Windows blue comes back as Windows blue.
    val fallback = remember(seed) { generateShades(seed) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComparisonRow(label = "rememberFluentColors", shades = FluentTheme.colors.shades)
        ComparisonRow(label = "generateShades", shades = fallback)
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    shades: Shades,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(176.dp),
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.secondary,
        )
        ShadeRamp(
            shades = shades,
            height = 32.dp,
        )
    }
}

@Composable
private fun ShadeTile(
    name: String,
    color: Color,
    onColor: Color?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color)
            .padding(8.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (onColor != null) {
            Column {
                Text(
                    text = name,
                    style = FluentTheme.typography.caption,
                    color = onColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = color.toHex(),
                    style = FluentTheme.typography.caption,
                    color = onColor,
                )
            }
        }
    }
}

private fun Shades.named(): List<Pair<String, Color>> =
    listOf(
        "dark3" to dark3,
        "dark2" to dark2,
        "dark1" to dark1,
        "base" to base,
        "light1" to light1,
        "light2" to light2,
        "light3" to light3,
    )
