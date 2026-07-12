package com.materialkolor.builder.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ExpressiveIcon(
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = "Supports 2025 spec",
        modifier = modifier,
    )
}
