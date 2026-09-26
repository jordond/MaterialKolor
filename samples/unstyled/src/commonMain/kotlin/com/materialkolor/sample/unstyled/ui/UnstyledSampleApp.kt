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
import com.composeunstyled.DialogHost
import com.composeunstyled.FocusVisibilityProvider
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.state.SampleStore
import com.materialkolor.sample.shared.state.rememberSampleStore
import com.materialkolor.sample.shared.theme.isDark
import com.materialkolor.sample.unstyled.theme.ContentMaxWidth
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksTheme
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.ui.component.VerticalScrollbar
import com.materialkolor.sample.unstyled.ui.palette.PaletteSection
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * The Tasks app built on Compose Unstyled and themed by `material-kolor-unstyled`. Every control is an Unstyled
 * primitive painted from the MaterialKolor tokens.
 */
@Composable
public fun UnstyledSampleApp(store: SampleStore = rememberSampleStore()) {
    val state = store.state
    TasksTheme(seed = state.seed.color, isDark = state.mode.isDark()) {
        FocusVisibilityProvider(Modifier.fillMaxSize()) {
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
                    AppSection.Palette -> PaletteSection(seed = state.seed)
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
