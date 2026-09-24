package com.materialkolor.sample.unstyled.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.composeunstyled.DialogHost
import com.composeunstyled.FocusVisibilityProvider
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.state.SampleStore
import com.materialkolor.sample.shared.state.rememberSampleStore
import com.materialkolor.sample.shared.theme.isDark
import com.materialkolor.sample.shared.ui.SampleTags
import com.materialkolor.sample.unstyled.theme.ContentMaxWidth
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksTheme
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.ui.component.VerticalScrollbar
import com.materialkolor.sample.unstyled.ui.palette.PaletteSection
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * The Tasks app built on Compose Unstyled and themed by `material-kolor-unstyled`.
 *
 * Every control is an Unstyled primitive painted from the MaterialKolor tokens, the way an app on the adapter would
 * draw. Picking a seed or a mode regenerates the theme and every color eases to its new value.
 *
 * Run it with `./gradlew :samples:unstyled:run`.
 *
 * @param[store] Holds the tasks, the filter, the seed and the mode. Every change goes through it.
 */
@Composable
public fun UnstyledSampleApp(store: SampleStore = rememberSampleStore()) {
    val state = store.state
    TasksTheme(seed = state.seed.color, isDark = state.mode.isDark()) {
        // A focus ring shows when the keyboard moves focus and stays hidden after a click.
        FocusVisibilityProvider(Modifier.fillMaxSize()) {
            // The dialog opens here, in the app's own layout, instead of in a window of its own.
            DialogHost(
                Modifier
                    .fillMaxSize()
                    .background(MaterialKolorTokens.surface.color),
            ) {
                TasksPage(state = state, dispatch = store::dispatch)
                ClearDoneDialog(state = state, dispatch = store::dispatch)
            }
        }
    }
}

@Composable
private fun TasksPage(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        // Rows are few, so a plain scrolling column keeps every one of them composed.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(Spacing.XLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.XLarge),
            ) {
                Header(state = state, dispatch = dispatch)
                when (state.section) {
                    AppSection.Tasks -> TasksSection(state = state, dispatch = dispatch)
                    AppSection.Palette -> PaletteSection(
                        seed = state.seed,
                        modifier = Modifier.testTag(SampleTags.Palette),
                    )
                }
            }
        }
        VerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}
