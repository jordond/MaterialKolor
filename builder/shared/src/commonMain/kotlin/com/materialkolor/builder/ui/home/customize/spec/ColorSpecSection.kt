package com.materialkolor.builder.ui.home.customize.spec

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.dynamiccolor.ColorSpec

@Composable
fun ColorSpecSection(
    selected: ColorSpec.SpecVersion,
    onSelected: (ColorSpec.SpecVersion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = "ColorSpec Version",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Choose the ColorSpec version to use for the dynamic color palette. " +
                "By default, the 2021 version is used.",
            style = MaterialTheme.typography.bodyMedium,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorSpec.SpecVersion.entries.forEach { version ->
                val name = remember(version) { version.name() }
                FilterChip(
                    selected = version == selected,
                    label = { Text(text = name) },
                    onClick = { onSelected(version) },
                )
            }
        }
    }
}

private fun ColorSpec.SpecVersion.name(): String = when (this) {
    ColorSpec.SpecVersion.SPEC_2021 -> "2021"
    ColorSpec.SpecVersion.SPEC_2025 -> "2025"
}
