package com.materialkolor.sample.customtheme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.GroupLabel
import com.materialkolor.sample.customtheme.ui.component.SeedSwatch
import com.materialkolor.sample.customtheme.ui.component.SegmentedControl
import com.materialkolor.sample.customtheme.ui.component.TabStrip
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags

/**
 * The top of the page. The title and the mode picker, the seed swatches, then the section tabs.
 */
@Composable
internal fun Header(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = SampleCopy.appTitle,
                style = AppType.Title,
                color = colors.textStrong,
                modifier = Modifier.weight(1f),
            )

            LabeledGroup(label = SampleCopy.modeGroup) {
                SegmentedControl(
                    options = ThemeMode.entries,
                    selected = state.mode,
                    onSelect = { mode -> dispatch(SampleAction.SelectMode(mode)) },
                    label = SampleCopy::label,
                    optionModifier = { mode -> Modifier.testTag(SampleTags.mode(mode)) },
                )
            }
        }

        LabeledGroup(label = SampleCopy.seedGroup) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.selectableGroup(),
            ) {
                for (seed in SampleSeed.entries) {
                    SeedSwatch(
                        color = seed.color,
                        label = SampleCopy.label(seed),
                        isSelected = seed == state.seed,
                        onClick = { dispatch(SampleAction.SelectSeed(seed)) },
                        modifier = Modifier.testTag(SampleTags.seed(seed)),
                    )
                }
            }
        }

        TabStrip(
            tabs = AppSection.entries,
            selected = state.section,
            onSelect = { section -> dispatch(SampleAction.SelectSection(section)) },
            label = SampleCopy::label,
            tabModifier = { section -> Modifier.testTag(SampleTags.section(section)) },
        )
    }
}

@Composable
private fun LabeledGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupLabel(text = label)
        content()
    }
}
