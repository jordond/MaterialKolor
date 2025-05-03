package com.materialkolor.builder.ui.home.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.export.model.ExportOptions
import com.materialkolor.builder.export.model.ExportType
import com.materialkolor.builder.ui.ktx.clickableWithoutRipple

@Composable
fun ExportOptionsCard(
    options: ExportOptions,
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

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.width(250.dp),
            ) {
                ExportType.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ExportType.entries.size,
                        ),
                        onClick = toggleMode,
                        selected = mode == options.type,
                        icon = {},
                        label = { Text(mode.displayName) },
                    )
                }
            }

            OptionSwitch(
                text = "Multiplatform",
                value = options.multiplatform,
                onValueChange = { updateOptions(options.copy(multiplatform = it)) },
            )

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

@Composable
private fun OptionSwitch(
    text: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics { contentDescription = "Toggle $text" }
            .fillMaxWidth()
            .clickableWithoutRipple {
                onValueChange(!value)
            },
    ) {
        Text(text = text)

        Switch(
            checked = value,
            onCheckedChange = { onValueChange(it) },
            thumbContent = {
                if (value) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.8f),
                    )
                }
            },
        )
    }
}
