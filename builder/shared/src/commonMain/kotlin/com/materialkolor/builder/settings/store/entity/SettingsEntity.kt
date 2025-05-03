package com.materialkolor.builder.settings.store.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.settings.model.ColorSettings
import com.materialkolor.builder.settings.model.ImagePresets
import com.materialkolor.builder.settings.model.KeyColor
import com.materialkolor.builder.settings.model.SeedImage
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.settings.model.SettingsDefaults
import com.materialkolor.dynamiccolor.ColorSpec

data class SettingsEntity(
    val colors: Map<KeyColor, Int?>,
    val isDarkMode: Boolean? = null,
    val contrast: Double = SettingsDefaults.contrast.value,
    val selectedPresetId: String? = null,
    val style: PaletteStyle = SettingsDefaults.style,
    val isAmoled: Boolean = SettingsDefaults.isAmoled,
    val specVersion: ColorSpec.SpecVersion = SettingsDefaults.specVersion,
    val packageName: String? = null,
)

fun Settings.toEntity(): SettingsEntity {
    val presetId = (selectedImage as? SeedImage.Resource)?.id
    return SettingsEntity(
        colors = colors.toEntity(),
        contrast = contrast.value,
        isDarkMode = isDarkMode,
        selectedPresetId = presetId,
        style = style,
        isAmoled = isAmoled,
        specVersion = specVersion,
        packageName = packageName,
    )
}

fun SettingsEntity.toModel(isDarkModeFallback: Boolean): Settings {
    val preset = selectedPresetId?.let { id ->
        ImagePresets.all.find { it.id == id }
    }

    return Settings(
        colors = colors.toModel(),
        isDarkMode = isDarkMode ?: isDarkModeFallback,
        contrast = contrast.parseContrast(),
        selectedImage = preset,
        style = style,
        isAmoled = isAmoled,
        specVersion = specVersion,
        packageName = packageName ?: SettingsDefaults.packageName,
    )
}

private fun ColorSettings.toEntity(): Map<KeyColor, Int?> = listOfNotNull(
    KeyColor.Seed to seed.toArgb(),
    primary?.let { KeyColor.Primary to it.toArgb() },
    secondary?.let { KeyColor.Secondary to it.toArgb() },
    tertiary?.let { KeyColor.Tertiary to it.toArgb() },
    error?.let { KeyColor.Error to it.toArgb() },
    neutral?.let { KeyColor.Neutral to it.toArgb() },
    neutralVariant?.let { KeyColor.NeutralVariant to it.toArgb() },
).toMap()

private fun Map<KeyColor, Int?>.toModel(): ColorSettings {
    val seed = get(KeyColor.Seed)?.let { Color(it) } ?: ColorSettings.colors.first()
    return ColorSettings(
        seed = seed,
        primary = get(KeyColor.Primary)?.let { Color(it) },
        secondary = get(KeyColor.Secondary)?.let { Color(it) },
        tertiary = get(KeyColor.Tertiary)?.let { Color(it) },
        error = get(KeyColor.Error)?.let { Color(it) },
        neutral = get(KeyColor.Neutral)?.let { Color(it) },
        neutralVariant = get(KeyColor.NeutralVariant)?.let { Color(it) },
    )
}

private fun Double.parseContrast(): Contrast = when (this) {
    Contrast.Default.value -> Contrast.Default
    Contrast.Medium.value -> Contrast.Medium
    Contrast.High.value -> Contrast.High
    Contrast.Reduced.value -> Contrast.Reduced
    else -> Contrast.Default
}
