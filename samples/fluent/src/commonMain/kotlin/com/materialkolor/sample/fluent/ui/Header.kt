package com.materialkolor.sample.fluent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.ui.component.SectionLabel
import com.materialkolor.sample.fluent.ui.component.SeedSwatch
import com.materialkolor.sample.fluent.ui.component.SegmentedPicker
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.SelectorBar
import io.github.composefluent.component.SelectorBarItem
import io.github.composefluent.component.Text

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
                modifier = Modifier.weight(1f),
                style = FluentTheme.typography.title,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel(text = SampleCopy.modeGroup)

        SegmentedPicker(
            options = ThemeMode.entries,
            selected = mode,
            onSelect = onSelect,
            text = { option -> Text(text = SampleCopy.label(option)) },
        )
    }
}

@Composable
private fun SeedPicker(
    seed: SampleSeed,
    onSelect: (SampleSeed) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel(text = SampleCopy.seedGroup)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    SelectorBar(modifier = Modifier.offset(x = (-12).dp)) {
        for (option in AppSection.entries) {
            SelectorBarItem(
                selected = option == section,
                onSelectedChange = { onSelect(option) },
                text = { Text(text = SampleCopy.label(option)) },
            )
        }
    }
}
