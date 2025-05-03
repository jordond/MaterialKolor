package com.materialkolor.builder.ui.home.customize

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.settings.model.ImagePresets
import com.materialkolor.builder.settings.model.KeyColor
import com.materialkolor.builder.settings.model.SeedImage
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.LocalWindowSizeClass
import com.materialkolor.builder.ui.home.customize.colors.CoreColorsSection
import com.materialkolor.builder.ui.home.customize.contrast.ContrastSection
import com.materialkolor.builder.ui.home.customize.seed.SeedColorSection
import com.materialkolor.builder.ui.home.customize.spec.ColorSpecSection
import com.materialkolor.builder.ui.home.customize.style.PaletteStyleSection
import com.materialkolor.builder.ui.ktx.widthIsExpanded
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.collections.immutable.PersistentList

@Composable
fun CustomizeSection(
    settings: Settings,
    modifier: Modifier = Modifier,
    onSelectImage: (SeedImage.Resource?) -> Unit,
    openColorPicker: (KeyColor, Color) -> Unit,
    onRandomColor: () -> Unit,
    onUpdatePaletteStyle: (PaletteStyle) -> Unit,
    onUpdateContrast: (Contrast) -> Unit,
    updateSpecVersion: (ColorSpec.SpecVersion) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    imagePresets: PersistentList<SeedImage.Resource> = ImagePresets.all,
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
    processingImage: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Generate your own Material 3 color scheme, to use with Jetpack Compose, or Compose Multiplatform.",
        )

        SeedColorSection(
            settings = settings,
            onSelectImage = onSelectImage,
            openColorPicker = { openColorPicker(KeyColor.Seed, settings.colors.seed) },
            onRandomColor = onRandomColor,
            imagePresets = imagePresets,
            processingImage = processingImage,
        )

        ColorSpecSection(
            selected = settings.specVersion,
            onSelected = updateSpecVersion,
        )

        PaletteStyleSection(
            selected = settings.style,
            onUpdate = onUpdatePaletteStyle,
        )

        if (!windowSizeClass.widthIsExpanded()) {
            ContrastSection(
                selected = settings.contrast,
                onUpdate = onUpdateContrast,
            )
        }

        CoreColorsSection(
            onClickColor = openColorPicker,
        )

        Spacer(modifier = Modifier.height(200.dp))
    }
}
