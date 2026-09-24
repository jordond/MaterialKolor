package com.materialkolor.sample.customtheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.AppTheme
import com.materialkolor.sample.customtheme.theme.AppThemeMode
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.PageScrollbar
import com.materialkolor.sample.customtheme.ui.palette.PaletteSection
import com.materialkolor.sample.customtheme.ui.tasks.TasksSection
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.state.SampleStore
import com.materialkolor.sample.shared.state.rememberSampleStore
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleTags

/**
 * The Tasks app on Compose Foundation alone, drawn with the components in `ui.component` and colored by [AppColors].
 *
 * The theme is generated from the store's seed and mode, so picking either one fades the whole app to the new colors.
 *
 * @param[store] Where the state comes from and where every action goes.
 */
@Composable
public fun SampleApp(store: SampleStore = rememberSampleStore()) {
    val state = store.state

    AppTheme(
        seed = state.seed.color,
        mode = state.mode.toAppThemeMode(),
    ) {
        val colors = LocalAppColors.current
        val selection = TextSelectionColors(
            handleColor = colors.primary,
            backgroundColor = colors.primary.copy(alpha = SELECTION_ALPHA),
        )

        CompositionLocalProvider(LocalTextSelectionColors provides selection) {
            Page(
                state = state,
                dispatch = store::dispatch,
            )
        }
    }
}

@Composable
private fun Page(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
            ) {
                Header(
                    state = state,
                    dispatch = dispatch,
                )

                when (state.section) {
                    AppSection.Tasks -> TasksSection(
                        state = state,
                        dispatch = dispatch,
                    )
                    AppSection.Palette -> PaletteSection(
                        modifier = Modifier.testTag(SampleTags.Palette),
                    )
                }
            }
        }

        PageScrollbar(
            state = scroll,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 2.dp),
        )
    }
}

/**
 * The shared mode, in the theme's own words. The theme keeps its own enum so it does not depend on the sample.
 */
private fun ThemeMode.toAppThemeMode(): AppThemeMode =
    when (this) {
        ThemeMode.System -> AppThemeMode.System
        ThemeMode.Light -> AppThemeMode.Light
        ThemeMode.Dark -> AppThemeMode.Dark
    }

/** Selected text gets a see-through wash of the primary accent. */
private const val SELECTION_ALPHA = 0.3f
