package com.materialkolor.sample.fluent.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.ui.component.SectionLabel
import com.materialkolor.sample.fluent.ui.component.SeedSwatch
import com.materialkolor.sample.fluent.ui.component.SegmentedPicker
import com.materialkolor.sample.fluent.ui.component.focusStroke
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.SelectorBar
import io.github.composefluent.component.SelectorBarItem
import io.github.composefluent.component.Text

/**
 * The top of the window. The title and the mode picker, the seed swatches, then the section tabs.
 *
 * @param[state] What to show.
 * @param[onAction] Where the header sends what the user picks.
 * @param[modifier] The modifier for the header.
 */
@Composable
internal fun Header(
    state: SampleState,
    onAction: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
            testTag = { option -> SampleTags.mode(option) },
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
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
    SelectorBar(modifier = Modifier.selectableGroup()) {
        for (option in AppSection.entries) {
            key(option) {
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()

                SelectorBarItem(
                    selected = option == section,
                    onSelectedChange = { onSelect(option) },
                    text = { Text(text = SampleCopy.label(option)) },
                    modifier = Modifier
                        .testTag(SampleTags.section(option))
                        .focusStroke(
                            visible = focused,
                            color = FluentTheme.colors.stroke.focus.outer,
                            shape = FluentTheme.shapes.control,
                        ),
                    interactionSource = interaction,
                )
            }
        }
    }
}
