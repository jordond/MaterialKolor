package com.materialkolor.builder.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.materialkolor.builder.core.exportSupported
import com.materialkolor.builder.export.model.ExportOptions
import com.materialkolor.builder.ui.about.AboutInfo
import com.materialkolor.builder.ui.components.AppSnackbarHost
import com.materialkolor.builder.ui.components.AppTopBar
import com.materialkolor.builder.ui.components.ColorPickerDialog
import com.materialkolor.builder.ui.components.ColorPickerState
import com.materialkolor.builder.ui.home.HomeAction.Share
import com.materialkolor.builder.ui.home.HomeAction.ToggleDarkMode
import com.materialkolor.builder.ui.home.HomeAction.UpdateColor
import com.materialkolor.builder.ui.home.components.ExportDialog
import com.materialkolor.builder.ui.home.components.HomeBottomBar
import com.materialkolor.builder.ui.home.preview.PreviewSection
import com.materialkolor.builder.ui.ktx.widthIsCompact
import com.materialkolor.builder.ui.ktx.widthIsExpanded
import com.materialkolor.builder.ui.ktx.windowSizeClass
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberRelay
import dev.stateholder.dispatcher.rememberRelayOf

@Composable
fun HomeScreenScaffold(
    options: ExportOptions,
    materialKolorVersion: String,
    colorPickerState: ColorPickerState?,
    dispatcher: Dispatcher<HomeAction>,
    modifier: Modifier = Modifier,
    exporting: Boolean = false,
    screen: HomeScreens = HomeScreens.Preview,
    initialSection: PreviewSection? = null,
    snackbarState: SnackbarHostState = remember { SnackbarHostState() },
    processingImage: Boolean = false,
    windowSizeClass: WindowSizeClass = windowSizeClass(),
) {
    var aboutDialogVisible by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(initialSection ?: PreviewSection.Customize) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { AppSnackbarHost(snackbarState) },
        topBar = {
            AppTopBar(
                settings = options.settings,
                toggleDarkMode = dispatcher.relay(ToggleDarkMode),
                toggleAboutDialog = { aboutDialogVisible = true },
                showBackButton = screen == HomeScreens.Export,
                onBack = dispatcher.relay(HomeAction.Nav(HomeScreens.Preview)),
            )
        },
        bottomBar = {
            AnimatedVisibility(screen == HomeScreens.Preview && windowSizeClass.widthIsCompact()) {
                HomeBottomBar(
                    selected = selectedSection,
                    onSelected = { selectedSection = it },
                )
            }
        },
        floatingActionButton = {
            if (exportSupported) {
                Crossfade(windowSizeClass.widthIsExpanded()) { isExpanded ->
                    if (isExpanded) {
                        val action =
                            if (screen == HomeScreens.Export) HomeAction.Export
                            else HomeAction.Nav(HomeScreens.Export)

                        ExtendedFloatingActionButton(
                            onClick = dispatcher.rememberRelay(action),
                            icon = {
                                if (screen == HomeScreens.Export) {
                                    Icon(Icons.Default.Download, contentDescription = "Export")
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Default.KeyboardArrowRight,
                                        contentDescription = "Next",
                                    )
                                }
                            },
                            text = {
                                val text = if (screen == HomeScreens.Export) "Export" else "Next"
                                Text(text = text)
                            },
                        )
                    } else {
                        FloatingActionButton(
                            onClick = dispatcher.rememberRelay(Share(selectedSection)),
                            content = { Icon(Icons.Default.Share, contentDescription = "Share") },
                        )
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = dispatcher.rememberRelay(Share(selectedSection)),
                    content = { Icon(Icons.Default.Share, contentDescription = "Share") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding),
        ) {
            HomeContent(
                screen = screen,
                options = options,
                materialKolorVersion = materialKolorVersion,
                selectedSection = selectedSection,
                updateSelectedSection = { selectedSection = it },
                dispatcher = dispatcher,
                processingImage = processingImage,
                windowSizeClass = windowSizeClass,
            )
        }

        AboutInfo(
            visible = aboutDialogVisible,
            onDismiss = { aboutDialogVisible = false },
            windowSizeClass = windowSizeClass,
        )

        ColorPickerDialog(
            state = colorPickerState,
            onDismiss = dispatcher.rememberRelay(HomeAction.CloseColorPicker),
            onColorChanged = dispatcher.rememberRelayOf(::UpdateColor),
            toggleMode = dispatcher.rememberRelay(HomeAction.TogglePickerMode),
            selectImage = dispatcher.rememberRelay(HomeAction.PickImageForColor),
        )

        if (exporting) {
            ExportDialog(
                onDismiss = dispatcher.relay(HomeAction.CancelExport),
            )
        }
    }
}
