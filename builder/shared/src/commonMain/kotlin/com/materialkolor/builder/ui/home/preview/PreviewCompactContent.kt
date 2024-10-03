package com.materialkolor.builder.ui.home.preview

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.materialkolor.builder.ui.home.customize.CustomizeSection
import com.materialkolor.builder.ui.home.preview.device.DeviceSection
import com.materialkolor.builder.ui.home.preview.gallery.GallerySection
import com.materialkolor.builder.ui.home.preview.palette.PaletteSection
import com.materialkolor.builder.ui.home.preview.theme.ThemeSection
import com.materialkolor.builder.ui.ktx.widthIsCompact

@Composable
fun PreviewCompactContent(
    settings: Settings,
    selectedSection: PreviewSection,
    processingImage: Boolean,
    dispatcher: Dispatcher<HomeAction>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    Crossfade(selectedSection) { section ->
        when (section) {
            PreviewSection.Customize -> {
                CustomizeSection(
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
            PreviewSection.Preview -> {
                WrappedContent {
                    DeviceSection()
                }
            }
            PreviewSection.Components -> {
                val isCompact = windowSizeClass.widthIsCompact()
                WrappedContent {
                    GallerySection(
                        showTitle = false,
                        defaultExpanded = !isCompact,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            PreviewSection.Themes -> {
                WrappedContent {
                    ThemeSection(
                        settings = settings,
                        onCopyColor = dispatcher.rememberRelayOf(::CopyColor),
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    PaletteSection(modifier = Modifier.padding(bottom = 16.dp))

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

        Spacer(modifier = Modifier.height(100.dp))
    }
}
