package com.materialkolor.builder.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.materialkolor.Contrast
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun ContrastSelector(
    selected: Contrast,
    onUpdate: (Contrast) -> Unit,
    modifier: Modifier = Modifier,
    options: PersistentList<Contrast> = Contrast.entries.sortedBy { it.value }.toPersistentList(),
    containerColor: Color = CardDefaults.elevatedCardColors().containerColor,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        shape = CircleShape,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            options.forEach { contrast ->
                ContrastButton(
                    contrast = contrast,
                    isSelected = contrast == selected,
                    onClick = { onUpdate(contrast) },
                )
            }
        }
    }
}

@Composable
private fun ContrastButton(
    contrast: Contrast,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.tertiaryContainer,
    )

    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onTertiary
        else MaterialTheme.colorScheme.onTertiaryContainer,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = !isSelected, onClick = onClick),
    ) {
        Icon(
            imageVector = contrast.icon(),
            contentDescription = contrast.name(),
            tint = contentColor,
            modifier = Modifier.padding(6.dp),
        )
    }
}

@Composable
private fun Contrast.icon(): ImageVector = remember(this) {
    when (this) {
        Contrast.Default -> Icons.Outlined.BrightnessLow
        Contrast.Medium -> Icons.Outlined.BrightnessMedium
        Contrast.High -> Icons.Outlined.BrightnessHigh
        Contrast.Reduced -> Icons.Default.KeyboardArrowDown
    }
}

@Composable
private fun Contrast.name(): String = remember(this) {
    when (this) {
        Contrast.Default -> "Standard"
        Contrast.Medium -> "Medium"
        Contrast.High -> "High"
        Contrast.Reduced -> "Reduced"
    }
}
