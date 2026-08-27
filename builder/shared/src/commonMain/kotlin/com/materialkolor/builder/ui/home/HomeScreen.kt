package com.materialkolor.builder.ui.home

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.builder.ui.home.HomeAction.CancelExport
import com.materialkolor.builder.ui.home.HomeAction.ColorPicker
import com.materialkolor.builder.ui.home.HomeAction.CopyColor
import com.materialkolor.builder.ui.home.HomeAction.Export
import com.materialkolor.builder.ui.home.HomeAction.Nav
import com.materialkolor.builder.ui.home.HomeAction.RandomColor
import com.materialkolor.builder.ui.home.HomeAction.Reset
import com.materialkolor.builder.ui.home.HomeAction.SelectImage
import com.materialkolor.builder.ui.home.HomeAction.Share
import com.materialkolor.builder.ui.home.HomeAction.ToggleDarkMode
import com.materialkolor.builder.ui.home.HomeAction.ToggleExportMode
import com.materialkolor.builder.ui.home.HomeAction.UpdateContrast
import com.materialkolor.builder.ui.home.HomeAction.UpdateExportOptions
import com.materialkolor.builder.ui.home.HomeAction.UpdatePaletteStyle
import com.materialkolor.builder.ui.home.HomeAction.UpdateSpecVersion
import com.materialkolor.builder.ui.home.preview.PreviewSection
import com.materialkolor.builder.ui.home.preview.gallery.NavigationDrawerContent
import com.materialkolor.builder.ui.ktx.launch
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import dev.stateholder.dispatcher.rememberDebounceDispatcher
import dev.stateholder.extensions.HandleEvents

val LocalSnackbarHostState =
    compositionLocalOf<SnackbarHostState> { error("SnackbarHostState is not found") }

val LocalDrawerState =
    compositionLocalOf<DrawerState> { error("DrawerState is not found") }

val LocalBottomSheetScaffoldState =
    compositionLocalOf<BottomSheetScaffoldState> { error("BottomSheetScaffoldState is not found") }

@Composable
fun HomeScreen(destination: String? = null) {
    val model = viewModel { HomeModel() }
    val state by model.state.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scaffoldState = rememberBottomSheetScaffoldState(
        rememberStandardBottomSheetState(SheetValue.Hidden, skipHiddenState = false),
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val pickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Single,
        onResult = { files -> model.handleImage(files.firstOrNull()) },
    )

    HandleEvents(model) { event ->
        when (event) {
            is HomeModel.Event.ShowSnackbar -> snackbar.launch(scope, event.message)
            is HomeModel.Event.PickImage -> pickerLauncher.launch()
        }
    }

    var screen by remember { mutableStateOf(HomeScreens.Preview) }

    val initialSection = remember {
        destination?.let { runCatching { PreviewSection.valueOf(it) }.getOrNull() }
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbar,
        LocalDrawerState provides drawerState,
        LocalBottomSheetScaffoldState provides scaffoldState,
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet { NavigationDrawerContent() }
            },
        ) {
            HomeScreenScaffold(
                options = state.exportOptions,
                materialKolorVersion = state.materialKolorVersion,
                colorPickerState = state.colorPickerState,
                snackbarState = snackbar,
                initialSection = initialSection,
                processingImage = state.processingImage,
                exporting = state.exporting,
                screen = screen,
                dispatcher = rememberDebounceDispatcher { action ->
                    when (action) {
                        is UpdateContrast -> model.updateContrast(action.contrast)
                        is UpdatePaletteStyle -> model.updatePaletteStyle(action.style)
                        is SelectImage -> {
                            if (action.image == null) pickerLauncher.launch()
                            else model.selectImagePreset(action.image)
                        }
                        is ToggleDarkMode -> model.toggleDarkMode()
                        is HomeAction.ToggleExpressive -> model.toggleExpressive()
                        is RandomColor -> model.randomColor()
                        is Reset -> model.reset()
                        is CopyColor -> model.copyColorToClipboard(action.name, action.color)
                        is ColorPicker -> model.handleColorPickerAction(action)
                        is Nav -> screen = action.screen
                        is Share -> model.share(action.section)
                        is ToggleExportMode -> model.toggleExportMode()
                        is UpdateExportOptions -> model.updateExportOptions(action.options)
                        is Export -> model.export()
                        is CancelExport -> model.cancelExport()
                        is UpdateSpecVersion -> model.updateSpecVersion(action.version)
                    }
                },
            )
        }
    }
}
