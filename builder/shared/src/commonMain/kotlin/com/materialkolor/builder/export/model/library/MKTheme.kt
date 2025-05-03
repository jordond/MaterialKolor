package com.materialkolor.builder.export.model.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.decapitalize
import androidx.compose.ui.text.intl.Locale
import com.materialkolor.Contrast
import com.materialkolor.builder.export.model.header
import com.materialkolor.builder.settings.model.Settings

fun mkThemeKt(
    packageName: String,
    themeName: String,
    settings: Settings,
    animate: Boolean,
): String {
    val contrast = if (settings.contrast == Contrast.Default) null
    else "contrastLevel = ${settings.contrast.value}"

    val params = listOfNotNull(
        contrast,
        settings.isAmoled.parameter("isAmoled"),
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
package $packageName

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun $themeName(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = darkTheme,
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

private fun Color?.parameter(name: String): String? {
    if (this == null) return null
    return "${name.decapitalize(Locale("EN"))} = ${name.replaceFirstChar { it.uppercase() }}"
}
