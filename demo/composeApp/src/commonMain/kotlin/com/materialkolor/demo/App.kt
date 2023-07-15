@file:OptIn(ExperimentalMaterial3Api::class)

package com.materialkolor.demo

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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.demo.theme.AppTheme

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

    var seedColor: Color? by remember { mutableStateOf(null) }
    var style by remember { mutableStateOf(PaletteStyle.TonalSpot) }
    var darkTheme by remember { mutableStateOf(isDarkTheme) }

    AppTheme(seedColor, style, darkTheme) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Box(
                modifier = Modifier.align(Alignment.End)
            ) {
                IconButton(
                    onClick = { darkTheme = !darkTheme },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    val icon = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode
                    Icon(icon, contentDescription = null)
                }
            }

            Text(text = "Palette Style")
            FlowRow(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaletteStyle.values().forEach { paletteStyle ->
                    FilterChip(
                        label = { Text(text = paletteStyle.name) },
                        selected = style == paletteStyle,
                        onClick = { style = paletteStyle },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Seed Color")
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(height = 32.dp, width = 80.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(seedColor ?: Color.Transparent)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SampleColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(color)
                            .clickable { seedColor = color }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Preview", style = MaterialTheme.typography.headlineSmall)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
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
                        modifier = Modifier.weight(1f),
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

                        LinearProgressIndicator()
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
            color = textColor,
            modifier = Modifier.padding(8.dp),
        )
    }
}

private fun Modifier.conditional(
    condition: Boolean,
    block: @Composable (Modifier) -> Modifier,
): Modifier = composed { if (condition) then(block(this)) else this }