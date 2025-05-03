package com.materialkolor.builder.settings.model

import androidx.compose.runtime.Immutable
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

@Immutable
data class Settings(
    val colors: ColorSettings,
    val isDarkMode: Boolean,
    val selectedImage: SeedImage?,
    val contrast: Contrast = SettingsDefaults.contrast,
    val style: PaletteStyle = SettingsDefaults.style,
    val isAmoled: Boolean = SettingsDefaults.isAmoled,
    val isModified: Boolean = false,
    val specVersion: ColorSpec.SpecVersion = SettingsDefaults.specVersion,
    val packageName: String = SettingsDefaults.packageName,
) {

    companion object {

        val Default = Settings(
            colors = ColorSettings(ColorSettings.colors.last()),
            isDarkMode = false,
            selectedImage = null,
        )
    }
}

object SettingsDefaults {
    val contrast = Contrast.Default
    val style = PaletteStyle.TonalSpot
    const val isAmoled = false
    val specVersion = ColorSpec.SpecVersion.Default
    const val packageName = "com.example.app"
}
