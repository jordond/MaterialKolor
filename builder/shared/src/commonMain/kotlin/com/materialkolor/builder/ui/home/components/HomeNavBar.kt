package com.materialkolor.builder.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.materialkolor.builder.ui.home.preview.PreviewSection

@Composable
fun HomeBottomBar(
    selected: PreviewSection,
    onSelected: (PreviewSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        PreviewSection.All.forEach { section ->
            val name = section.name()
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = section.icon(),
                        contentDescription = name,
                    )
                },
                label = { Text(name) },
                selected = section == selected,
                onClick = { onSelected(section) },
            )
        }
    }
}

@Composable
fun HomeNavRail(
    selected: PreviewSection,
    onSelected: (PreviewSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
    ) {
        PreviewSection.All.forEach { section ->
            val name = section.name()
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = section.icon(),
                        contentDescription = name,
                    )
                },
                label = { Text(name) },
                selected = section == selected,
                onClick = { onSelected(section) },
            )
        }
    }
}

@Composable
private fun PreviewSection.icon(): ImageVector = remember(this) {
    when (this) {
        PreviewSection.Customize -> Icons.Default.Tune
        PreviewSection.Themes -> Icons.Default.Contrast
        PreviewSection.Preview -> Icons.Default.Smartphone
        PreviewSection.Components -> Icons.Default.Preview
    }
}

@Composable
private fun PreviewSection.name(): String = remember(this) {
    when (this) {
        PreviewSection.Customize,
        PreviewSection.Themes,
        PreviewSection.Preview -> this.name
        PreviewSection.Components -> "Gallery"
    }
}
