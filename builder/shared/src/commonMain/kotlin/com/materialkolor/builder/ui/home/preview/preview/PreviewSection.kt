package com.materialkolor.builder.ui.home.preview.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.home.preview.device.DeviceSection
import com.materialkolor.builder.ui.home.preview.gallery.GallerySection
import com.materialkolor.builder.ui.home.preview.palette.PaletteSection
import com.materialkolor.builder.ui.home.preview.theme.ThemeSection
import com.materialkolor.builder.ui.ktx.sidePadding
import com.materialkolor.builder.ui.ktx.windowSizeClass

@Composable
fun PreviewSection(
    settings: Settings,
    modifier: Modifier = Modifier,
    onCopyColor: (String, Color) -> Unit = { _, _ -> },
    windowSizeClass: WindowSizeClass = windowSizeClass(),
) {
    Box(
        modifier = modifier
            .padding(horizontal = windowSizeClass.sidePadding())
            .fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PreviewSectionContainer(title = "Preview") {
                DeviceSection()
            }

            PreviewSectionContainer(title = "Theme") {
                ThemeSection(
                    settings = settings,
                    onCopyColor = onCopyColor,
                )
            }

            GallerySection()

            PreviewSectionContainer(title = "Palettes", initialExpanded = false) {
                PaletteSection()
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}
