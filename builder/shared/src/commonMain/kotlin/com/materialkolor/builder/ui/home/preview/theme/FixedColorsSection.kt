package com.materialkolor.builder.ui.home.preview.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.SectionDivider

@Composable
fun FixedColorsSection(
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
                text = "Fixed Colors (independent of theme)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = SectionDivider),
            )

            CompositionLocalProvider(LocalTextStyle provides ThemeSectionDefaults.TextStyle) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SectionDivider),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ThemeSectionDefaults.FixedColors.forEach { group ->
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            ColorBox(group.main.color, onClick, textColor = group.main.onColor.color())
                            ColorBox(group.dim, onClick, textColor = group.main.onColor.color())
                            SingleLineColorBox(
                                themeColor = group.main.onColor,
                                onClick = onClick,
                                textColor = group.main.color.color(),
                            )
                            SingleLineColorBox(
                                themeColor = group.onVariant,
                                onClick = onClick,
                                textColor = group.main.color.color(),
                            )
                        }
                    }
                }
            }
        }
    }
}
