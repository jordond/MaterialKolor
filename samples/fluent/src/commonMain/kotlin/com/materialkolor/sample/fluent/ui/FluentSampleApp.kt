package com.materialkolor.sample.fluent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.fluent.animateFluentColors
import com.materialkolor.fluent.rememberFluentColors
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.Text
import io.github.composefluent.generateShades

/**
 * The seeds the sample offers. Any colour works; these are just enough of a spread to show that the
 * ramp follows the seed rather than snapping back to Windows blue.
 */
private val seeds = listOf(
    "Windows" to Color(0xFF0078D4),
    "Violet" to Color(0xFF6750A4),
    "Crimson" to Color(0xFFB3261E),
    "Forest" to Color(0xFF2E7D32),
    "Amber" to Color(0xFFF0A202),
    "Teal" to Color(0xFF00695C),
    "Slate" to Color(0xFF7A7A7E),
)

/**
 * A Fluent window themed from a MaterialKolor ramp.
 *
 * Run it with `./gradlew :samples:fluent:run`.
 */
@Composable
public fun FluentSampleApp() {
    var selected by remember { mutableStateOf(seeds.first()) }
    val seed = selected.second
    var isDark by remember { mutableStateOf(true) }
    var animate by remember { mutableStateOf(true) }

    val generated = rememberFluentColors(seedColor = seed, isDark = isDark)
    val colors = if (animate) animateFluentColors(generated) else generated

    FluentTheme(colors = colors) {
        Mica(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Heading("Seed, ${selected.first}")
                SeedPicker(
                    selected = seed,
                    onSelect = { picked -> selected = picked },
                )

                Heading("Shades")
                ShadeRamp()

                Heading("What Fluent gives you without this adapter")
                WithoutAdapter(seed = seed)

                Heading("Components")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentButton(onClick = { isDark = !isDark }) {
                        Text(if (isDark) "Switch to light" else "Switch to dark")
                    }
                    Button(onClick = { animate = !animate }) {
                        Text(if (animate) "Animation on" else "Animation off")
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        color = FluentTheme.colors.text.text.secondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SeedPicker(
    selected: Color,
    onSelect: (Pair<String, Color>) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (entry in seeds) {
            val (name, color) = entry
            // The whole cell is the target. Putting the click on the swatch alone leaves the label
            // looking tappable and doing nothing.
            Column(
                modifier = Modifier.clickable { onSelect(entry) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                        .border(
                            width = if (color == selected) 3.dp else 1.dp,
                            color = if (color == selected) {
                                FluentTheme.colors.text.accent.primary
                            } else {
                                FluentTheme.colors.stroke.control.default
                            },
                            shape = RoundedCornerShape(6.dp),
                        ),
                )
                Text(text = name, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ShadeRamp() {
    val shades = FluentTheme.colors.shades
    val ramp = listOf(
        "dark3" to shades.dark3,
        "dark2" to shades.dark2,
        "dark1" to shades.dark1,
        "base" to shades.base,
        "light1" to shades.light1,
        "light2" to shades.light2,
        "light3" to shades.light3,
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(6.dp)),
        ) {
            for ((_, color) in ramp) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            for ((name, _) in ramp) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = FluentTheme.colors.text.text.tertiary,
                )
            }
        }
    }
}

@Composable
private fun WithoutAdapter(seed: Color) {
    // generateShades is Fluent's own lookup. It has a single entry, so every seed that is not
    // Windows blue comes back as Windows blue. Showing it beside the generated ramp is the point.
    val fallback = generateShades(seed)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(fallback.base),
        )
        Text(
            text = "Fluent's generateShades(seed) for this seed",
            fontSize = 11.sp,
            color = FluentTheme.colors.text.text.tertiary,
        )
    }
}
