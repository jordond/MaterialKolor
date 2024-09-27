package com.materialkolor.builder.settings.model

import androidx.compose.runtime.Immutable
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle

@Immutable
data class Settings(
    val colors: ColorSettings,
    val isDarkMode: Boolean,
    val selectedImage: SeedImage?,
    val contrast: Contrast = Contrast.Default,
    val style: PaletteStyle = PaletteStyle.TonalSpot,
    val isExtendedFidelity: Boolean = false,
    val isModified: Boolean = false,
)
