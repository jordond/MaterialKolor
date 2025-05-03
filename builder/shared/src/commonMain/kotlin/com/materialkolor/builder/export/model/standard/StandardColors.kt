package com.materialkolor.builder.export.model.standard

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.decapitalize
import androidx.compose.ui.text.intl.Locale
import com.materialkolor.Contrast
import com.materialkolor.builder.export.variable
import com.materialkolor.builder.ktx.snakeToCamelCase
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.getColor
import com.materialkolor.scheme.DynamicScheme

fun standardColorsKt(
    packageName: String,
    settings: Settings,
): String {
    val map = MaterialDynamicColors().colorList()
    val light = createScheme(isDark = false, settings = settings)
    val dark = createScheme(isDark = true, settings = settings)

    return """
package $packageName

import androidx.compose.ui.graphics.Color

${settings.colors.seed.variable("Seed")}

${map.toColorVariables(scheme = light)}
${map.toColorVariables(scheme = dark)}
""".trimIndent().dropLastWhile { it == '\n' }
}

/**
 * Return a list of color names:
 *
 * Example:
 * ```
 * "primary" to "PrimaryLight"
 * ```
 */
fun lightVariableNamePairs(settings: Settings): Map<String, String> {
    return variableNamePairs(settings, isDark = false)
}

fun darkVariableNamePairs(settings: Settings): Map<String, String> {
    return variableNamePairs(settings, isDark = true)
}

private fun variableNamePairs(settings: Settings, isDark: Boolean): Map<String, String> {
    val list = MaterialDynamicColors().colorList()
    val scheme = createScheme(isDark = isDark, settings = settings)
    return list.variableNamePair(scheme).map { entry ->
        entry.value.name.snakeToCamelCase().decapitalize(Locale("EN")) to entry.key
    }.toMap()
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun createScheme(
    isDark: Boolean,
    settings: Settings,
) = DynamicScheme(
    isDark = isDark,
    seedColor = settings.colors.seed,
    primary = settings.colors.primary ?: settings.colors.seed,
    secondary = settings.colors.secondary,
    tertiary = settings.colors.tertiary,
    error = settings.colors.error,
    neutral = settings.colors.neutral,
    neutralVariant = settings.colors.neutralVariant,
    style = settings.style,
    contrastLevel = settings.contrast.value,
)

private fun MaterialDynamicColors.colorList(): List<DynamicColor> = listOf(
    primary(),
    onPrimary(),
    primaryContainer(),
    onPrimaryContainer(),
    secondary(),
    onSecondary(),
    secondaryContainer(),
    onSecondaryContainer(),
    tertiary(),
    onTertiary(),
    tertiaryContainer(),
    onTertiaryContainer(),
    error(),
    onError(),
    errorContainer(),
    onErrorContainer(),
    background(),
    onBackground(),
    surface(),
    onSurface(),
    surfaceVariant(),
    onSurfaceVariant(),
    outline(),
    outlineVariant(),
    scrim(),
    inverseSurface(),
    inverseOnSurface(),
    inversePrimary(),
    surfaceDim(),
    surfaceBright(),
    surfaceContainerLowest(),
    surfaceContainerLow(),
    surfaceContainer(),
    surfaceContainerHigh(),
    surfaceContainerHighest(),
)

private fun List<DynamicColor>.variableNamePair(
    scheme: DynamicScheme,
): Map<String, DynamicColor> {
    val contrast = scheme.contrastSuffix()
    val mode = if (scheme.isDark) "Dark" else "Light"
    val suffix = "$mode$contrast"
    return associateBy { color -> "${color.name.snakeToCamelCase()}$suffix" }
}

private fun List<DynamicColor>.toColorVariables(
    scheme: DynamicScheme,
): String {
    val values = variableNamePair(scheme)
    return buildString {
        values.forEach { (name, color) ->
            appendLine(color.getColor(scheme).variable(name))
        }
    }
}

private fun DynamicScheme.contrastSuffix(): String = when (contrastLevel) {
    Contrast.Reduced.value -> "ReducedContrast"
    Contrast.Default.value -> ""
    Contrast.Medium.value -> "MediumContrast"
    Contrast.High.value -> "HighContrast"
    else -> ""
}
