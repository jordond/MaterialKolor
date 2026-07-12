package com.materialkolor.builder.ui.home.preview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.LocalWindowSizeClass
import com.materialkolor.builder.ui.home.HomeAction
import com.materialkolor.builder.ui.home.components.HomeNavRail
import dev.stateholder.dispatcher.Dispatcher

@Composable
fun PreviewScreenContent(
    settings: Settings,
    processingImage: Boolean,
    selectedSection: PreviewSection,
    updateSelectedSection: (PreviewSection) -> Unit,
    dispatcher: Dispatcher<HomeAction>,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            PreviewExpandedContent(
                settings = settings,
                dispatcher = dispatcher,
                windowSizeClass = windowSizeClass,
                modifier = modifier,
            )
        }
        WindowWidthSizeClass.Medium -> {
            Row(modifier = Modifier.fillMaxSize()) {
                HomeNavRail(
                    selected = selectedSection,
                    onSelected = { updateSelectedSection(it) },
                )
                PreviewCompactContent(
                    settings = settings,
                    selectedSection = selectedSection,
                    processingImage = processingImage,
                    dispatcher = dispatcher,
                    windowSizeClass = windowSizeClass,
                    modifier = modifier,
                )
            }
        }
        WindowWidthSizeClass.Compact -> {
            PreviewCompactContent(
                settings = settings,
                selectedSection = selectedSection,
                processingImage = processingImage,
                dispatcher = dispatcher,
                windowSizeClass = windowSizeClass,
                modifier = modifier,
            )
        }
    }
}
