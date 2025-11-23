package com.materialkolor.builder.ui.home.customize.spec

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.home.components.ExpressiveIcon
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
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "Choose the ColorSpec version to use for the dynamic color palette. " +
                "The 2025 spec is part of Material 3 Expressive and currently only supports " +
                "Tonal Spot, Neutral, Vibrant, and Expressive variants.",
            style = MaterialTheme.typography.bodyMedium,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorSpec.SpecVersion.entries.forEach { version ->
                val name = remember(version) { version.name() }
                val trailingIcon =
                    if (version != ColorSpec.SpecVersion.SPEC_2025) null
                    else {
                        @Composable {
                            ExpressiveIcon(Modifier.size(FilterChipDefaults.IconSize))
                        }
                    }

                FilterChip(
                    selected = version == selected,
                    label = { Text(text = name) },
                    onClick = { onSelected(version) },
                    trailingIcon = trailingIcon,
                )
            }
        }
    }
}

private fun ColorSpec.SpecVersion.name(): String = when (this) {
    ColorSpec.SpecVersion.SPEC_2021 -> "2021"
    ColorSpec.SpecVersion.SPEC_2025 -> "2025"
}
