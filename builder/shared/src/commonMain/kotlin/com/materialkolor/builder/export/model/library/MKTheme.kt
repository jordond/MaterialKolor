package com.materialkolor.builder.export.model.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.decapitalize
import androidx.compose.ui.text.intl.Locale
import com.materialkolor.Contrast
import com.materialkolor.builder.export.model.header
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.dynamiccolor.ColorSpec

fun mkThemeKt(
    themeName: String,
    settings: Settings,
    animate: Boolean,
): String {
    val contrast = if (settings.contrast == Contrast.Default) null
    else "contrastLevel = ${settings.contrast.value}"

    val imports = listOfNotNull(
        "import androidx.compose.foundation.isSystemInDarkTheme",
        "import androidx.compose.runtime.Composable",
        "import com.materialkolor.DynamicMaterialTheme",
        "import com.materialkolor.PaletteStyle",
        if (settings.specVersion.include) "import com.materialkolor.dynamiccolor.ColorSpec" else null,
        "import com.materialkolor.rememberDynamicMaterialThemeState",
    ).joinToString("\n")

    val params = listOfNotNull(
        contrast,
        settings.isAmoled.parameter("isAmoled"),
        settings.specVersion.parameter(),
        if (settings.colors.primary == null) settings.colors.seed.parameter("SeedColor")
        else settings.colors.primary.parameter("Primary"),
        settings.colors.secondary.parameter("Secondary"),
        settings.colors.tertiary.parameter("Tertiary"),
        settings.colors.error.parameter("Error"),
        settings.colors.neutral.parameter("Neutral"),
        settings.colors.neutralVariant.parameter("NeutralVariant"),
    ).joinToString(",\n        ")

    return """
${header(settings)}
package ${settings.packageName}

$imports

@Composable
fun $themeName(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = PaletteStyle.${settings.style},
        $params,
    )
    
    DynamicMaterialTheme(
        state = dynamicThemeState,
        animate = $animate,
        content = content,
    )
}
""".trimIndent()
}

private fun Boolean.parameter(name: String) = if (this) "$name = true" else null

private val ColorSpec.SpecVersion.include
    get() = this != ColorSpec.SpecVersion.SPEC_2021

private fun ColorSpec.SpecVersion.parameter(): String? {
    return if (!include) null
    else "specVersion = ColorSpec.SpecVersion.${this.name}"
}

private fun Color?.parameter(name: String): String? {
    if (this == null) return null
    return "${name.decapitalize(Locale("EN"))} = ${name.replaceFirstChar { it.uppercase() }}"
}
