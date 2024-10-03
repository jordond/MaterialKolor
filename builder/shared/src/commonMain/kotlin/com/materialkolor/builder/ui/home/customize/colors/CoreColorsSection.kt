package com.materialkolor.builder.ui.home.customize.colors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.settings.model.KeyColor
import com.materialkolor.builder.ui.home.components.ColorCard
import kotlinx.collections.immutable.PersistentList

@Composable
fun CoreColorsSection(
    onClickColor: (KeyColor, Color) -> Unit,
    modifier: Modifier = Modifier,
    coreColors: PersistentList<CoreColor> = CoreColors,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Core colors",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Override or set key colors that will be used to generate tonal palettes and schemes.",
            style = MaterialTheme.typography.bodySmall,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            coreColors.forEach { coreColor ->
                val color = coreColor.color()
                ColorCard(
                    color = color,
                    title = coreColor.title,
                    subtitle = coreColor.subtitle,
                    onClick = { onClickColor(coreColor.type, color) },
                )
            }
        }
    }
}
