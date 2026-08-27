package com.materialkolor.builder.export.model.standard

import com.materialkolor.Contrast
import com.materialkolor.builder.export.model.header
import com.materialkolor.builder.settings.model.Settings

private fun androidImports(expressive: Boolean) = listOfNotNull(
    "import android.os.Build",
    "import androidx.compose.foundation.isSystemInDarkTheme",
    if (expressive) "import androidx.compose.material3.MaterialExpressiveTheme"
    else "import androidx.compose.material3.MaterialTheme",
    if (expressive) "import androidx.compose.material3.MotionScheme" else null,
    "import androidx.compose.material3.darkColorScheme",
    "import androidx.compose.material3.lightColorScheme",
    "import androidx.compose.material3.dynamicDarkColorScheme",
    "import androidx.compose.material3.dynamicLightColorScheme",
    "import androidx.compose.runtime.Composable",
    "import androidx.compose.ui.platform.LocalContext",
).joinToString("\n")

private fun multiplatformImports(expressive: Boolean) = listOfNotNull(
    "import androidx.compose.foundation.isSystemInDarkTheme",
    if (expressive) "import androidx.compose.material3.MaterialExpressiveTheme"
    else "import androidx.compose.material3.MaterialTheme",
    if (expressive) "import androidx.compose.material3.MotionScheme" else null,
    "import androidx.compose.material3.darkColorScheme",
    "import androidx.compose.material3.lightColorScheme",
    "import androidx.compose.runtime.Composable",
).joinToString("\n")

fun standardThemeKt(
    themeName: String,
    multiplatform: Boolean,
    settings: Settings,
): String {
    val lightColors = variableNamePairs(settings, isDark = false)
    val lightSchemeName = settings.contrast.schemeName(isDark = false)
    val darkColors = variableNamePairs(settings, isDark = true)
    val darkSchemeName = settings.contrast.schemeName(isDark = true)
    val baseThemeComposable = if (settings.useMaterialExpressive) {
        """
        |    MaterialExpressiveTheme(
        |        colorScheme = colorScheme,
        |        motionScheme = MotionScheme.expressive(),
        |        content = content,
        |    )
    """.trimMargin()
    } else {
        """
        |    MaterialTheme(
        |        colorScheme = colorScheme,
        |        content = content,
        |    )
    """.trimMargin()
    }

    val themeComposable =
        if (multiplatform) multiplatformTheme(themeName, lightSchemeName, darkSchemeName, baseThemeComposable)
        else androidTheme(themeName, lightSchemeName, darkSchemeName, baseThemeComposable)

    return """
${header(settings)}
package ${settings.packageName}

${if (multiplatform) multiplatformImports(settings.useMaterialExpressive) else androidImports(settings.useMaterialExpressive)}

private val $lightSchemeName = lightColorScheme(
${lightColors.toParamList()},
)

private val $darkSchemeName = darkColorScheme(
${darkColors.toParamList()},
)

$themeComposable
""".trimIndent()
}

private fun multiplatformTheme(
    themeName: String,
    lightSchemeName: String,
    darkSchemeName: String,
    baseThemeComposable: String
) = """
@Composable
fun $themeName(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> $darkSchemeName
        else -> $lightSchemeName
    }

$baseThemeComposable
}
""".trimIndent()

private fun androidTheme(
    themeName: String,
    lightSchemeName: String,
    darkSchemeName: String,
    baseThemeComposable: String
) = """
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

$baseThemeComposable
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
