package com.materialkolor.builder.ui.home.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.Dispatcher
import com.materialkolor.builder.export.model.ExportOptions
import com.materialkolor.builder.ui.LocalWindowSizeClass
import com.materialkolor.builder.ui.home.HomeAction
import com.materialkolor.builder.ui.home.HomeAction.UpdateExportOptions
import com.materialkolor.builder.ui.home.LocalSnackbarHostState
import com.materialkolor.builder.ui.ktx.launch

@Composable
fun ExportScreenContent(
    options: ExportOptions,
    dispatcher: Dispatcher<HomeAction>,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            ExportExpandedContent(
                options = options,
                modifier = modifier,
                dispatcher = dispatcher,
            )
        }
        else -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text("Not supported", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please expand the window to view the export settings, or view on a larger screen.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp),
                )
            }
        }
    }
}

@Composable
fun ExportExpandedContent(
    options: ExportOptions,
    dispatcher: Dispatcher<HomeAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        ExportOptionsCard(
            options = options,
            toggleMode = dispatcher.rememberRelay(HomeAction.ToggleExportMode),
            updateOptions = dispatcher.rememberRelayOf(::UpdateExportOptions),
            modifier = Modifier.widthIn(max = 300.dp),
        )

        var selected by remember { mutableStateOf(options.files.first()) }
        LaunchedEffect(options.files) {
            selected = options.files.firstOrNull { it.name == selected.name } ?: options.files.first()
        }

        val clipboard = LocalClipboardManager.current
        val snackbar = LocalSnackbarHostState.current
        val scope = rememberCoroutineScope()
        FileListContainer(
            selected = selected,
            files = options.files,
            onSelected = { selected = it },
            onClick = {
                clipboard.setText(AnnotatedString(selected.content))
                snackbar.launch(scope, "Copied the contents of ${selected.name} to clipboard")
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )
    }
}
