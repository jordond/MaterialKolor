package com.materialkolor.sample.material3.ui

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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.material3.DynamicMaterialTheme
import com.materialkolor.sample.material3.ui.component.PageScrollbar
import com.materialkolor.sample.material3.ui.palette.PaletteSection
import com.materialkolor.sample.material3.ui.tasks.ClearDoneDialog
import com.materialkolor.sample.material3.ui.tasks.TasksSection
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.state.SampleStore
import com.materialkolor.sample.shared.state.rememberSampleStore
import com.materialkolor.sample.shared.theme.isDark

@Composable
public fun Material3SampleApp(store: SampleStore = rememberSampleStore()) {
    val state = store.state
    val onAction: (SampleAction) -> Unit = store::dispatch

    DynamicMaterialTheme(
        seedColor = state.seed.color,
        isDark = state.mode.isDark(),
        animate = true,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SamplePage(state = state, onAction = onAction)
        }

        ClearDoneDialog(
            visible = state.isClearDialogVisible,
            count = state.doneCount,
            onConfirm = { onAction(SampleAction.ConfirmClearDone) },
            onDismiss = { onAction(SampleAction.DismissClearDone) },
        )
    }
}

@Composable
private fun SamplePage(
    state: SampleState,
    onAction: (SampleAction) -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
            ) {
                Header(state = state, onAction = onAction)

                when (state.section) {
                    AppSection.Tasks -> TasksSection(state = state, onAction = onAction)
                    AppSection.Palette -> PaletteSection(seed = state.seed)
                }
            }
        }

        PageScrollbar(
            state = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}
