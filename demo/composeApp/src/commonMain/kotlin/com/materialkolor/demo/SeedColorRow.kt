package com.materialkolor.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.materialkolor.LocalDynamicMaterialThemeSeed

@Composable
fun SeedColorRow() {
    val seedColor = LocalDynamicMaterialThemeSeed.current

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
                .background(seedColor)
        )
    }
}