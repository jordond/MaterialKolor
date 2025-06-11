package com.materialkolor.builder.settings.store.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.ktx.parseHexToColor
import com.materialkolor.builder.settings.model.KeyColor
import com.materialkolor.builder.settings.model.SettingsDefaults
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.toHex
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLQueryComponent

private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_IS_AMOLED = "is_amoled"
private const val KEY_CONTRAST = "contrast"
private const val KEY_SELECTED_PRESET_ID = "selected_preset_id"
private const val KEY_COLOR_SPEC = "color_spec"
private const val KEY_STYLE = "style"
private const val KEY_PACKAGE_NAME = "package_name"
private const val KEY_INCLUDE_MISC_COLORS = "misc"
private const val KEY_USE_MATERIAL_EXPRESSIVE = "expressive"
private val KeyColor.KEY
    get() = "color_${name.lowercase()}"

private const val SEPARATOR = "&"

fun SettingsEntity.toQueryParams(): String {
    val colors = colors
        .mapNotNull { (key, value) ->
            value
                ?.let { Color(it) }
                ?.toHex(
                    includePrefix = false,
                    alwaysIncludeAlpha = true,
                )
                .param(key.KEY)
        }

    val colorParams = if (colors.isEmpty()) null else colors.joinToString(SEPARATOR)
    val params = listOfNotNull(
        colorParams,
        "${KEY_DARK_MODE}=${isDarkMode ?: false}",
        style.param(KEY_STYLE, SettingsDefaults.style),
        selectedPresetId.param(KEY_SELECTED_PRESET_ID),
        contrast.param(KEY_CONTRAST, SettingsDefaults.contrast.value),
        isAmoled.param(KEY_IS_AMOLED, SettingsDefaults.isAmoled),
        specVersion.param(KEY_COLOR_SPEC, SettingsDefaults.specVersion),
        packageName.param(KEY_PACKAGE_NAME),
        includeMiscColors.param(KEY_INCLUDE_MISC_COLORS, SettingsDefaults.includeMiscColors),
        useMaterialExpressive.param(KEY_USE_MATERIAL_EXPRESSIVE, SettingsDefaults.useMaterialExpressive),
    ).joinToString(SEPARATOR)

    return "?$params"
}

fun String.toSettingsEntity(): SettingsEntity {
    val params = splitQueryParams()
    val colors = KeyColor.entries
        .associateWith { keyColor -> params[keyColor.KEY]?.parseHexToColor()?.toArgb() }
        .filterValues { it != null }

    return SettingsEntity(
        colors = colors,
        isDarkMode = params[KEY_DARK_MODE]?.toBooleanStrictOrNull(),
        contrast = params[KEY_CONTRAST]?.toDoubleOrNull() ?: Contrast.Default.value,
        selectedPresetId = params[KEY_SELECTED_PRESET_ID]?.takeIf { it.isNotBlank() },
        style = params[KEY_STYLE]?.safeToPaletteStyle() ?: PaletteStyle.TonalSpot,
        specVersion = parseSpecVersion(params[KEY_COLOR_SPEC]),
        packageName = params[KEY_PACKAGE_NAME],
        includeMiscColors = params[KEY_INCLUDE_MISC_COLORS]?.toBooleanStrictOrNull() ?: SettingsDefaults.includeMiscColors,
        useMaterialExpressive = params[KEY_USE_MATERIAL_EXPRESSIVE]?.toBooleanStrictOrNull() ?: SettingsDefaults.useMaterialExpressive,
    )
}

fun String.splitQueryParams(): Map<String, String> {
    return replaceFirst("?", "").split(SEPARATOR)
        .associate { param ->
            val (key, value) = param.split("=")
            key to value.decodeURLQueryComponent()
        }
}

private fun parseSpecVersion(string: String?): ColorSpec.SpecVersion {
    if (string == null) return ColorSpec.SpecVersion.Default
    return runCatching { ColorSpec.SpecVersion.valueOf(string) }.getOrDefault(ColorSpec.SpecVersion.Default)
}

private inline fun <reified T> T?.param(key: String, default: T? = null): String? {
    if (this == null || this == default) return null
    return "$key=${this.toString().encodeURLQueryComponent()}"
}

private fun String.safeToPaletteStyle(): PaletteStyle? {
    return try {
        enumValueOf<PaletteStyle>(this)
    } catch (_: Throwable) {
        null
    }
}
