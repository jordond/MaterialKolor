package com.materialkolor.builder.ui.home.preview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.home.HomeAction
import com.materialkolor.builder.ui.home.HomeAction.CopyColor
import com.materialkolor.builder.ui.home.preview.preview.PreviewSection
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberRelayOf

@Composable
fun PreviewExpandedContent(
    settings: Settings,
    dispatcher: Dispatcher<HomeAction>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        PreviewSection(
            settings = settings,
            onCopyColor = dispatcher.rememberRelayOf(::CopyColor),
            modifier = Modifier.weight(1f),
            windowSizeClass = windowSizeClass,
        )
    }
}
