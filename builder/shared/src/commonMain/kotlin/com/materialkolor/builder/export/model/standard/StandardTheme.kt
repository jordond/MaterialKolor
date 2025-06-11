package com.materialkolor.builder.export.model.standard

import com.materialkolor.Contrast
import com.materialkolor.builder.export.model.header
import com.materialkolor.builder.settings.model.Settings

private val androidImports = """
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
""".trimIndent()

private val multiplatformImports = """
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
""".trimIndent()

fun standardThemeKt(
    themeName: String,
    multiplatform: Boolean,
    settings: Settings,
): String {
    val lightColors = variableNamePairs(settings, isDark = false, includeMisc = false)
    val lightSchemeName = settings.contrast.schemeName(isDark = false)
    val darkColors = variableNamePairs(settings, isDark = true, includeMisc = false)
    val darkSchemeName = settings.contrast.schemeName(isDark = true)
    val themeComposable =
        if (multiplatform) multiplatformTheme(themeName, lightSchemeName, darkSchemeName)
        else androidTheme(themeName, lightSchemeName, darkSchemeName)

    return """
${header(settings)}
package ${settings.packageName}

${if (multiplatform) multiplatformImports else androidImports}

private val $lightSchemeName = lightColorScheme(
${lightColors.toParamList()},
)

private val $darkSchemeName = darkColorScheme(
${darkColors.toParamList()},
)

$themeComposable
""".trimIndent()
}

private fun multiplatformTheme(themeName: String, lightSchemeName: String, darkSchemeName: String) = """
    @Composable
    fun $themeName(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable() () -> Unit,
    ) {
        val colorScheme = when {
            darkTheme -> $darkSchemeName
            else -> $lightSchemeName
        }

        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
""".trimIndent()

private fun androidTheme(themeName: String, lightSchemeName: String, darkSchemeName: String) = """
    @Composable
    fun $themeName(
        isDarkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = true,
        content: @Composable() () -> Unit,
    ) {
        val colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            isDarkTheme -> $darkSchemeName
            else -> $lightSchemeName
        }
    
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
""".trimIndent()

private fun Contrast.schemeName(isDark: Boolean): String {
    val mode = if (isDark) "Dark" else "Light"
    return when (this) {
        Contrast.Reduced -> "reducedContrast${mode}ColorScheme"
        Contrast.Default -> "${mode.lowercase()}ColorScheme"
        Contrast.Medium -> "mediumContrast${mode}ColorScheme"
        Contrast.High -> "highContrast${mode}ColorScheme"
    }
}

private fun Map<String, String>.toParamList(): String {
    return toList().joinToString(",\n") { (name, variable) ->
        "    $name = $variable"
    }
}
