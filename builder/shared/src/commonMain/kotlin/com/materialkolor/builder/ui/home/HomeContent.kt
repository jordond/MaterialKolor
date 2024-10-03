package com.materialkolor.builder.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.Dispatcher
import com.materialkolor.builder.export.model.ExportOptions
import com.materialkolor.builder.ui.components.SideSheet
import com.materialkolor.builder.ui.components.SideSheetPosition
import com.materialkolor.builder.ui.home.HomeAction.OpenColorPicker
import com.materialkolor.builder.ui.home.HomeAction.RandomColor
import com.materialkolor.builder.ui.home.HomeAction.SelectImage
import com.materialkolor.builder.ui.home.HomeAction.UpdateContrast
import com.materialkolor.builder.ui.home.HomeAction.UpdatePaletteStyle
import com.materialkolor.builder.ui.home.components.ContrastSelector
import com.materialkolor.builder.ui.home.export.ExportScreenContent
import com.materialkolor.builder.ui.home.preview.PreviewScreenContent
import com.materialkolor.builder.ui.home.preview.PreviewSection
import com.materialkolor.builder.ui.home.customize.CustomizeSection
import com.materialkolor.builder.ui.ktx.widthIsExpanded
import com.materialkolor.builder.ui.ktx.windowSizeClass

@Composable
fun HomeContent(
    screen: HomeScreens,
    options: ExportOptions,
    selectedSection: PreviewSection,
    updateSelectedSection: (PreviewSection) -> Unit,
    processingImage: Boolean,
    dispatcher: Dispatcher<HomeAction>,
    windowSizeClass: WindowSizeClass = windowSizeClass(),
) {
    if (windowSizeClass.widthIsExpanded()) {
        SideSheet(
            position = SideSheetPosition.Start,
            initialExpanded = true,
            isFloating = true,
            displayOverContent = false,
            maxWidthFraction = 0.3f,
            sheetContent = {
                CustomizeSection(
                    settings = options.settings,
                    onSelectImage = dispatcher.rememberRelayOf(::SelectImage),
                    onRandomColor = dispatcher.rememberRelay(RandomColor),
                    openColorPicker = dispatcher.rememberRelayOf(::OpenColorPicker),
                    onUpdatePaletteStyle = dispatcher.rememberRelayOf(::UpdatePaletteStyle),
                    onUpdateContrast = dispatcher.rememberRelayOf(::UpdateContrast),
                    processingImage = processingImage,
                    windowSizeClass = windowSizeClass,
                )
            },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Crossfade(targetState = screen) { targetState ->
                    Content(
                        screen = targetState,
                        options = options,
                        selectedSection = selectedSection,
                        updateSelectedSection = updateSelectedSection,
                        processingImage = processingImage,
                        dispatcher = dispatcher,
                        windowSizeClass = windowSizeClass,
                    )
                }

                ContrastSelector(
                    selected = options.settings.contrast,
                    onUpdate = dispatcher.rememberRelayOf(::UpdateContrast),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp),
                )
            }
        }
    } else {
        Content(
            screen = screen,
            options = options,
            selectedSection = selectedSection,
            updateSelectedSection = updateSelectedSection,
            processingImage = processingImage,
            dispatcher = dispatcher,
            windowSizeClass = windowSizeClass,
        )
    }
}

@Composable
fun Content(
    screen: HomeScreens,
    options: ExportOptions,
    selectedSection: PreviewSection,
    updateSelectedSection: (PreviewSection) -> Unit,
    processingImage: Boolean,
    dispatcher: Dispatcher<HomeAction>,
    windowSizeClass: WindowSizeClass = windowSizeClass(),
) {
    if (screen == HomeScreens.Preview) {
        PreviewScreenContent(
            settings = options.settings,
            selectedSection = selectedSection,
            updateSelectedSection = updateSelectedSection,
            dispatcher = dispatcher,
            processingImage = processingImage,
            windowSizeClass = windowSizeClass,
        )
    } else {
        ExportScreenContent(
            options = options,
            dispatcher = dispatcher,
            windowSizeClass = windowSizeClass,
        )
    }
}
