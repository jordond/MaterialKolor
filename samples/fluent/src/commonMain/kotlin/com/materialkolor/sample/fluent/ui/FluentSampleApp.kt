package com.materialkolor.sample.fluent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.FluentSampleTheme
import com.materialkolor.sample.fluent.ui.palette.PaletteSection
import com.materialkolor.sample.fluent.ui.tasks.ClearDoneDialog
import com.materialkolor.sample.fluent.ui.tasks.TasksSection
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleStore
import com.materialkolor.sample.shared.state.rememberSampleStore
import io.github.composefluent.background.Mica
import io.github.composefluent.component.ScrollbarContainer
import io.github.composefluent.component.rememberScrollbarAdapter

/**
 * The Tasks app on Compose Fluent, themed from a seed color by material-kolor-fluent.
 *
 * Run it with `./gradlew :samples:fluent:run`.
 *
 * @param[store] Holds the app state. Everything the user does goes through it.
 */
@Composable
public fun FluentSampleApp(store: SampleStore = rememberSampleStore()) {
    val state = store.state

    FluentSampleTheme(
        seed = state.seed,
        mode = state.mode,
    ) {
        SampleWindow(store = store)
    }
}

@Composable
private fun SampleWindow(store: SampleStore) {
    val state = store.state
    val onAction: (SampleAction) -> Unit = store::dispatch
    val scrollState = rememberScrollState()

    Mica(modifier = Modifier.fillMaxSize()) {
        ScrollbarContainer(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Header(
                        state = state,
                        onAction = onAction,
                    )
                    when (state.section) {
                        AppSection.Tasks -> TasksSection(state = state, onAction = onAction)
                        AppSection.Palette -> PaletteSection(seed = state.seed)
                    }
                }
            }
        }

        ClearDoneDialog(
            visible = state.isClearDialogVisible,
            count = state.doneCount,
            onConfirm = { onAction(SampleAction.ConfirmClearDone) },
            onDismiss = { onAction(SampleAction.DismissClearDone) },
        )
    }
}
