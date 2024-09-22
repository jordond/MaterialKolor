@file:OptIn(ExperimentalMaterial3Api::class)

package com.materialkolor.demo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.demo.theme.AppTheme
import com.materialkolor.ktx.darken
import com.materialkolor.ktx.from
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.lighten
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.rememberDynamicMaterialThemeState
import kotlin.math.round

val SampleColors = listOf(
    Color(0xFFD32F2F),
    Color(0xFFC2185B),
    Color(0xFF7B1FA2),
    Color(0xFF512DA8),
    Color(0xFF303F9F),
    Color(0xFF1976D2),
    Color(0xFF0288D1),
    Color(0xFF0097A7),
    Color(0xFF00796B),
    Color(0xFF388E3C),
    Color(0xFF689F38),
    Color(0xFFAFB42B),
    Color(0xFFFBC02D),
    Color(0xFFFFA000),
    Color(0xFFF57C00),
    Color(0xFFE64A19),
    Color(0xFF5D4037),
    Color(0xFF616161),
    Color(0xFF455A64),
    Color(0xFF263238),
)

@Composable
fun colorSchemePairs() = listOf(
    "Primary" to (colorScheme.primary to colorScheme.onPrimary),
    "PrimaryContainer" to (colorScheme.primaryContainer to colorScheme.onPrimaryContainer),
    "Secondary" to (colorScheme.secondary to colorScheme.onSecondary),
    "SecondaryContainer" to (colorScheme.secondaryContainer to colorScheme.onSecondaryContainer),
    "Tertiary" to (colorScheme.tertiary to colorScheme.onTertiary),
    "TertiaryContainer" to (colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer),
    "Error" to (colorScheme.error to colorScheme.onError),
    "ErrorContainer" to (colorScheme.errorContainer to colorScheme.onErrorContainer),
    "Background" to (colorScheme.background to colorScheme.onBackground),
    "Surface" to (colorScheme.surface to colorScheme.onSurface),
    "SurfaceVariant" to (colorScheme.surfaceVariant to colorScheme.onSurfaceVariant),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun App() {
    val isDarkTheme = isSystemInDarkTheme()
    val isAmoled by rememberSaveable { mutableStateOf(false) }
    var seedColor by rememberSaveable { mutableStateOf(SampleColors[0].toArgb()) }
    var style by rememberSaveable { (mutableStateOf(PaletteStyle.TonalSpot.name)) }
    val state = rememberDynamicMaterialThemeState(
        seedColor = Color(seedColor),
        isDark = isDarkTheme,
        primary = null,
        isAmoled = isAmoled,
        style = PaletteStyle.valueOf(style),
    )

    AppTheme(state) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                if (state.isDark) {
                    Box {
                        IconButton(onClick = { state.isAmoled = !state.isAmoled }) {
                            val mirror = if (state.isAmoled) {
                                Modifier.scale(scaleX = -1f, scaleY = 1f)
                            } else {
                                Modifier
                            }
                            Icon(
                                Icons.Filled.Contrast,
                                contentDescription = null,
                                modifier = mirror,
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { state.isDark = !state.isDark }) {
                        val icon = if (state.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode
                        Icon(icon, contentDescription = null)
                    }
                }
            }

            Text(text = "Palette Style")
            FlowRow(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaletteStyle.entries.forEach { paletteStyle ->
                    FilterChip(
                        label = { Text(text = paletteStyle.name) },
                        selected = state.style == paletteStyle,
                        onClick = { style = paletteStyle.name },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SeedColorRow()

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SampleColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(color)
                            .clickable { seedColor = color.toArgb() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Preview", style = MaterialTheme.typography.headlineSmall)
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column {
                    colorSchemePairs().forEach { (name, colors) ->
                        val (color, onColor) = colors

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ColorBox(text = name, color = color, modifier = Modifier.weight(1f))
                            ColorBox(text = "On$name", color = onColor, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Card(modifier = Modifier.size(height = 75.dp, width = 160.dp)) {
                            Text("Card", modifier = Modifier.padding(4.dp))
                        }

                        ElevatedCard(modifier = Modifier.size(height = 75.dp, width = 160.dp)) {
                            Text("Elevated Card", modifier = Modifier.padding(4.dp))
                        }

                        OutlinedCard(modifier = Modifier.size(height = 75.dp, width = 160.dp)) {
                            Text("Outlined Card", modifier = Modifier.padding(4.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = false, onCheckedChange = {})
                            Checkbox(checked = true, onCheckedChange = {})
                        }

                        TextField("TextField", onValueChange = {})
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExtendedFloatingActionButton({}) {
                            Text("Extended FAB")
                        }

                        FloatingActionButton({}) {
                            Text("FAB")
                        }

                        FilledTonalButton({}) {
                            Text("Filled Tonal Button")
                        }

                        ElevatedButton({}) {
                            Text("Elevated Button")
                        }

                        OutlinedButton({}) {
                            Text("Outlined Button")
                        }

                        Button({}) {
                            Text("Button")
                        }

                        // TODO: Broken in Compose 1.6
                        // LinearProgressIndicator()
                    }
                }
            }

            Column {
                Text(text = "Harmonized red palette", style = MaterialTheme.typography.headlineSmall)

                Row {
                    val harmonizedColor = Color.Red.harmonize(colorScheme.primary)
                    val harmonizedPalette = TonalPalette.from(harmonizedColor)

                    repeat(10) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(harmonizedPalette.tone(it * 10)))
                        )
                    }
                }
            }

            Column {
                Text(text = "Harmonized SL red palette", style = MaterialTheme.typography.headlineSmall)

                Row {
                    val harmonizedColor = Color.Red
                        .harmonize(colorScheme.primary, matchSaturation = true)

                    val harmonizedPalette = TonalPalette.from(harmonizedColor)

                    repeat(10) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(harmonizedPalette.tone(it * 10)))
                        )
                    }
                }
            }

            Column {
                Text(text = "Harmonized blue palette", style = MaterialTheme.typography.headlineSmall)

                Row {
                    val harmonizedColor = Color.Blue.harmonize(colorScheme.primary)
                    val harmonizedPalette = TonalPalette.from(harmonizedColor)

                    repeat(10) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(harmonizedPalette.tone(it * 10)))
                        )
                    }
                }
            }

            Column {
                Text(text = "Harmonized SL blue palette", style = MaterialTheme.typography.headlineSmall)

                Row {
                    val harmonizedColor = Color.Blue
                        .harmonize(colorScheme.primary, matchSaturation = true)

                    val harmonizedPalette = TonalPalette.from(harmonizedColor)

                    repeat(10) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(harmonizedPalette.tone(it * 10)))
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var color by remember(state.seedColor) { mutableStateOf(state.seedColor) }

                Button(onClick = { color = color.lighten(1.1f) }) {
                    Text("Lighten")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(height = 32.dp, width = 80.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = color.toArgb().toString(16))
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { color = color.darken(1.1f) }) {
                    Text("Darken")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(10) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val ratio = 1.1f + it / 10f
                        val darker by remember(state.seedColor) {
                            mutableStateOf(state.seedColor.darken(ratio))
                        }
                        val lighter by remember(state.seedColor) {
                            mutableStateOf(state.seedColor.lighten(ratio))
                        }

                        Text(text = "Darken ${ratio.roundToTwoDecimalPlaces()}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(height = 32.dp, width = 80.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(darker)
                        )

                        Text(text = "Lighten ${ratio.roundToTwoDecimalPlaces()}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(height = 32.dp, width = 80.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(lighter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorBox(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val textColor = if (color.luminance() < 0.5f) Color.White else Color.Black
    Box(
        modifier = modifier
            .background(color)
    ) {
        Text(
            text = text,
            color = animateColorAsState(targetValue = textColor).value,
            modifier = Modifier.padding(8.dp),
        )
    }
}

private fun Float.roundToTwoDecimalPlaces(): Float {
    return round(this * 100) / 100
}
