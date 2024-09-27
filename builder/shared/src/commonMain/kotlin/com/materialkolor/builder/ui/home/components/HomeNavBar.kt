package com.materialkolor.builder.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.materialkolor.builder.ui.home.page.HomeSection

@Composable
fun HomeBottomBar(
    selected: HomeSection,
    onSelected: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        HomeSection.All.forEach { section ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = section.icon(),
                        contentDescription = section.name,
                    )
                },
                label = { Text(section.name) },
                selected = section == selected,
                onClick = { onSelected(section) },
            )
        }
    }
}

@Composable
fun HomeNavRail(
    selected: HomeSection,
    onSelected: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
    ) {
        HomeSection.All.forEach { section ->
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = section.icon(),
                        contentDescription = section.name,
                    )
                },
                label = { Text(section.name) },
                selected = section == selected,
                onClick = { onSelected(section) },
            )
        }
    }
}

private fun HomeSection.icon(): ImageVector = when (this) {
    HomeSection.Customize -> Icons.Default.Tune
    HomeSection.Themes -> Icons.Default.Contrast
    HomeSection.Palettes -> Icons.Default.Palette
    HomeSection.Preview -> Icons.Default.Smartphone
    HomeSection.Components -> Icons.Default.Preview
}
