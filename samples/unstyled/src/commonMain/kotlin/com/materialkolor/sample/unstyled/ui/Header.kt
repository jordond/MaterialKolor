package com.materialkolor.sample.unstyled.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Sun
import com.composeunstyled.Text
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.ui.component.Choice
import com.materialkolor.sample.unstyled.ui.component.SegmentedControl
import com.materialkolor.sample.unstyled.ui.component.SwatchPicker
import com.materialkolor.sample.unstyled.ui.component.UnderlineTabs
import com.materialkolor.unstyled.MaterialKolorTokens

private val ModeChoices: List<Choice<ThemeMode>> = ThemeMode.entries.map { mode ->
    val icon = when (mode) {
        ThemeMode.System -> Lucide.Monitor
        ThemeMode.Light -> Lucide.Sun
        ThemeMode.Dark -> Lucide.Moon
    }
    Choice(value = mode, label = SampleCopy.label(mode), testTag = SampleTags.mode(mode), icon = icon)
}

private val SeedChoices: List<Choice<SampleSeed>> = SampleSeed.entries.map { seed ->
    Choice(value = seed, label = SampleCopy.label(seed), testTag = SampleTags.seed(seed))
}

private val SectionChoices: List<Choice<AppSection>> = AppSection.entries.map { section ->
    val icon = when (section) {
        AppSection.Tasks -> Lucide.ListTodo
        AppSection.Palette -> Lucide.Palette
    }
    Choice(value = section, label = SampleCopy.label(section), testTag = SampleTags.section(section), icon = icon)
}

/**
 * The top of the page. The title and the mode picker, the seed swatches, then the section tabs.
 */
@Composable
internal fun Header(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = SampleCopy.appTitle,
                modifier = Modifier.weight(1f),
                style = TasksType.Title,
            )
            SegmentedControl(
                choices = ModeChoices,
                selected = state.mode,
                onSelect = { mode -> dispatch(SampleAction.SelectMode(mode)) },
                label = SampleCopy.modeGroup,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = SampleCopy.seedGroup,
                style = TasksType.Caption,
                color = MaterialKolorTokens.onSurfaceVariant.color,
            )
            SwatchPicker(
                choices = SeedChoices,
                selected = state.seed,
                onSelect = { seed -> dispatch(SampleAction.SelectSeed(seed)) },
                color = { seed -> seed.color },
                label = SampleCopy.seedGroup,
            )
        }

        UnderlineTabs(
            choices = SectionChoices,
            selected = state.section,
            onSelect = { section -> dispatch(SampleAction.SelectSection(section)) },
        )
    }
}
