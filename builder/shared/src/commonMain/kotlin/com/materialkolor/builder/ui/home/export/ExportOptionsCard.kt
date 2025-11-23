package com.materialkolor.builder.ui.home.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.export.model.ExportOptions
import com.materialkolor.builder.export.model.ExportType
import com.materialkolor.builder.ui.home.components.OptionSwitch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportOptionsCard(
    options: ExportOptions,
    materialKolorVersion: String,
    toggleMode: () -> Unit,
    updateOptions: (ExportOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .animateContentSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            TextField(
                value = options.settings.packageName,
                onValueChange = { value ->
                    updateOptions(options.copy(settings = options.settings.copy(packageName = value)))
                },
                label = { Text("Package Name") },
                isError = options.settings.packageName.isBlank(),
                singleLine = true,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                modifier = Modifier.width(250.dp),
            ) {
                ExportType.entries.forEach { mode ->
                    ToggleButton(
                        checked = mode == options.type,
                        onCheckedChange = { toggleMode() },
                        content = { Text(mode.displayName) },
                    )
                }
            }

            OptionSwitch(
                text = "Multiplatform",
                value = options.multiplatform,
                onValueChange = { updateOptions(options.copy(multiplatform = it)) },
            )

            OptionSwitch(
                text = "Material Expressive",
                value = options.settings.useMaterialExpressive,
                onValueChange = { value ->
                    updateOptions(
                        options.copy(settings = options.settings.copy(useMaterialExpressive = value)),
                    )
                },
            )

            AnimatedVisibility(visible = options.settings.useMaterialExpressive) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Material Expressive support is experimental and requires a " +
                            "pre-release version of MaterialKolor ($materialKolorVersion).",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            AnimatedVisibility(visible = options.type == ExportType.Standard) {
                OptionSwitch(
                    text = "Include misc colors",
                    value = options.settings.includeMiscColors,
                    onValueChange = { value ->
                        updateOptions(options.copy(settings = options.settings.copy(includeMiscColors = value)))
                    },
                )
            }

            AnimatedVisibility(visible = options.type == ExportType.MaterialKolor) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OptionSwitch(
                        text = "Animate",
                        value = options.animate,
                        onValueChange = { updateOptions(options.copy(animate = it)) },
                    )

                    OptionSwitch(
                        text = "Use version catalog",
                        value = options.useVersionCatalog,
                        onValueChange = { updateOptions(options.copy(useVersionCatalog = it)) },
                    )
                }
            }
        }
    }
}
