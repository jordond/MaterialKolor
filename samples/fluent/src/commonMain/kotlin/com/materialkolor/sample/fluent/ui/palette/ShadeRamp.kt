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
 * @param[shades] The ramp to show.
 * @param[modifier] The modifier for the ramp.
 * @param[height] How tall the ramp is.
 * @param[labeled] Whether each shade shows its name and hex value.
 */
@Composable
internal fun ShadeRamp(
    shades: Shades,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    labeled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(FluentTheme.shapes.overlay),
    ) {
        for ((name, color) in shades.named()) {
            ShadeTile(
                name = name,
                color = color,
                labeled = labeled,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * The ramp material-kolor-fluent generated from [seed], over the one Fluent's own `generateShades` returns.
 *
 * @param[seed] The seed both ramps start from.
 * @param[modifier] The modifier for the comparison.
 */
@Composable
internal fun WithoutAdapter(
    seed: Color,
    modifier: Modifier = Modifier,
) {
    // generateShades is a lookup with a single entry, so every seed that is not Windows blue comes back as Windows
    // blue. Showing it under the generated ramp is the point.
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
            labeled = false,
        )
    }
}

@Composable
private fun ShadeTile(
    name: String,
    color: Color,
    labeled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color)
            .padding(8.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (labeled) {
            val onColor = remember(color) { color.readableOn() }
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
