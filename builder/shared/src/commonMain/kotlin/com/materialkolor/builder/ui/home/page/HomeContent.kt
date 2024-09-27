package com.materialkolor.builder.ui.home.page

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.Dispatcher
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.home.HomeAction
import com.materialkolor.builder.ui.home.HomeAction.CopyColor
import com.materialkolor.builder.ui.home.HomeAction.OpenColorPicker
import com.materialkolor.builder.ui.home.HomeAction.RandomColor
import com.materialkolor.builder.ui.home.HomeAction.SelectImage
import com.materialkolor.builder.ui.home.HomeAction.UpdateContrast
import com.materialkolor.builder.ui.home.HomeAction.UpdatePaletteStyle
import com.materialkolor.builder.ui.home.page.customize.CustomizePage
import com.materialkolor.builder.ui.home.page.device.DeviceSection
import com.materialkolor.builder.ui.home.page.gallery.GallerySection
import com.materialkolor.builder.ui.home.page.palette.PaletteSection
import com.materialkolor.builder.ui.home.page.preview.PreviewSection
import com.materialkolor.builder.ui.home.page.theme.ThemeSection
import com.materialkolor.builder.ui.ktx.widthIsCompact

@Composable
fun ExpandedContent(
    settings: Settings,
    processingImage: Boolean,
    dispatcher: Dispatcher<HomeAction>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        CustomizePage(
            settings = settings,
            onSelectImage = dispatcher.rememberRelayOf(::SelectImage),
            onRandomColor = dispatcher.rememberRelay(RandomColor),
            openColorPicker = dispatcher.rememberRelayOf(::OpenColorPicker),
            onUpdatePaletteStyle = dispatcher.rememberRelayOf(::UpdatePaletteStyle),
            onUpdateContrast = dispatcher.rememberRelayOf(::UpdateContrast),
            processingImage = processingImage,
            windowSizeClass = windowSizeClass,
            modifier = Modifier.weight(0.5f),
        )

        PreviewSection(
            settings = settings,
            onUpdateContrast = dispatcher.rememberRelayOf(::UpdateContrast),
            onCopyColor = dispatcher.rememberRelayOf(::CopyColor),
            modifier = Modifier.weight(1f),
            windowSizeClass = windowSizeClass,
        )
    }
}

@Composable
fun CompactContent(
    settings: Settings,
    selectedSection: HomeSection,
    processingImage: Boolean,
    dispatcher: Dispatcher<HomeAction>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    Crossfade(selectedSection) { section ->
        when (section) {
            HomeSection.Customize -> {
                CustomizePage(
                    settings = settings,
                    onSelectImage = dispatcher.rememberRelayOf(::SelectImage),
                    onRandomColor = dispatcher.rememberRelay(RandomColor),
                    openColorPicker = dispatcher.rememberRelayOf(::OpenColorPicker),
                    onUpdatePaletteStyle = dispatcher.rememberRelayOf(::UpdatePaletteStyle),
                    onUpdateContrast = dispatcher.rememberRelayOf(::UpdateContrast),
                    processingImage = processingImage,
                    windowSizeClass = windowSizeClass,
                    modifier = modifier,
                )
            }
            HomeSection.Preview -> {
                WrappedContent {
                    DeviceSection()
                }
            }
            HomeSection.Components -> {
                val isCompact = windowSizeClass.widthIsCompact()
                WrappedContent {
                    GallerySection(
                        showTitle = false,
                        defaultExpanded = !isCompact,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            HomeSection.Themes -> {
                WrappedContent {
                    ThemeSection(
                        settings = settings,
                        onCopyColor = dispatcher.rememberRelayOf(::CopyColor),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            HomeSection.Palettes -> {
                WrappedContent {
                    PaletteSection(modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun WrappedContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        content()
    }
}
