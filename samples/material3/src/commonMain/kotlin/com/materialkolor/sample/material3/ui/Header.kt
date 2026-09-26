package com.materialkolor.sample.material3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.material3.ui.component.SeedSwatch
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun Header(
    state: SampleState,
    onAction: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = SampleCopy.appTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )

            ModePicker(
                mode = state.mode,
                onSelect = { mode -> onAction(SampleAction.SelectMode(mode)) },
            )
        }

        SeedPicker(
            seed = state.seed,
            onSelect = { seed -> onAction(SampleAction.SelectSeed(seed)) },
        )

        SectionTabs(
            section = state.section,
            onSelect = { section -> onAction(SampleAction.SelectSection(section)) },
        )
    }
}

@Composable
private fun ModePicker(
    mode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    SingleChoiceSegmentedButtonRow {
        ThemeMode.entries.forEachIndexed { index, option ->
            val selected = option == mode
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selected) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
                label = { Text(text = SampleCopy.label(option)) },
            )
        }
    }
}

@Composable
private fun SeedPicker(
    seed: SampleSeed,
    onSelect: (SampleSeed) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = SampleCopy.seedGroup,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (option in SampleSeed.entries) {
                SeedSwatch(
                    seed = option,
                    selected = option == seed,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun SectionTabs(
    section: AppSection,
    onSelect: (AppSection) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = section.ordinal,
        edgePadding = 0.dp,
    ) {
        for (option in AppSection.entries) {
            LeadingIconTab(
                selected = option == section,
                onClick = { onSelect(option) },
                text = { Text(text = SampleCopy.label(option)) },
                icon = {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.System -> Icons.Outlined.DesktopWindows
        ThemeMode.Light -> Icons.Outlined.LightMode
        ThemeMode.Dark -> Icons.Outlined.DarkMode
    }

private val AppSection.icon: ImageVector
    get() = when (this) {
        AppSection.Tasks -> Icons.Outlined.Checklist
        AppSection.Palette -> Icons.Outlined.Palette
    }
