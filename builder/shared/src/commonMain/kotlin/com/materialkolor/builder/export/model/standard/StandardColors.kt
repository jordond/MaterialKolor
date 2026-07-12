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
    settings: Settings,
): String {
    val materialDynamicColors = MaterialDynamicColors()
    val map = materialDynamicColors.colorList(settings.includeMiscColors)
    val light = createScheme(isDark = false, settings = settings)
    val dark = createScheme(isDark = true, settings = settings)

    val schemeIndependent =
        materialDynamicColors
            .schemeIndependentColors(settings.includeMiscColors)
            .toColorVariables(settings.contrast, light, includeScheme = false) // scheme doesn't matter here

    return """
package ${settings.packageName}

import androidx.compose.ui.graphics.Color

${settings.colors.seed.variable("Seed")}

${map.toColorVariables(settings.contrast, scheme = light)}
${map.toColorVariables(settings.contrast, scheme = dark)}
$schemeIndependent
""".trimIndent().dropLastWhile { it == '\n' }
}

fun variableNamePairs(
    settings: Settings,
    isDark: Boolean,
): Map<String, String> {
    val colors = MaterialDynamicColors()
    val independentColors = colors.schemeIndependentColors(false)
    val list = colors.colorList(false) + independentColors
    val scheme = createScheme(isDark = isDark, settings = settings)
    return list.variableNamePair(settings.contrast, scheme, independentColors).map { entry ->
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
    specVersion = settings.specVersion,
)

private fun MaterialDynamicColors.colorList(includeMisc: Boolean): List<DynamicColor> {
    val main = listOf(
        primary(),
        onPrimary(),
        primaryContainer(),
        onPrimaryContainer(),
        inversePrimary(),
        secondary(),
        onSecondary(),
        secondaryContainer(),
        onSecondaryContainer(),
        tertiary(),
        onTertiary(),
        tertiaryContainer(),
        onTertiaryContainer(),
        background(),
        onBackground(),
        surface(),
        onSurface(),
        surfaceVariant(),
        onSurfaceVariant(),
        surfaceTint(),
        inverseSurface(),
        inverseOnSurface(),
        error(),
        onError(),
        errorContainer(),
        onErrorContainer(),
        outline(),
        outlineVariant(),
        scrim(),
        surfaceBright(),
        surfaceContainer(),
        surfaceContainerHigh(),
        surfaceContainerHighest(),
        surfaceContainerLow(),
        surfaceContainerLowest(),
        surfaceDim(),
    )

    return if (includeMisc) main + miscColors() else main
}

private fun MaterialDynamicColors.schemeIndependentColors(includeMisc: Boolean) = listOfNotNull(
    primaryFixed(),
    primaryFixedDim(),
    onPrimaryFixed(),
    onPrimaryFixedVariant(),
    secondaryFixed(),
    secondaryFixedDim(),
    onSecondaryFixed(),
    onSecondaryFixedVariant(),
    tertiaryFixed(),
    tertiaryFixedDim(),
    onTertiaryFixed(),
    onTertiaryFixedVariant(),
    if (includeMisc) primaryPaletteKeyColor() else null,
    if (includeMisc) secondaryPaletteKeyColor() else null,
    if (includeMisc) tertiaryPaletteKeyColor() else null,
    if (includeMisc) neutralPaletteKeyColor() else null,
    if (includeMisc) neutralVariantPaletteKeyColor() else null,
    if (includeMisc) errorPaletteKeyColor() else null,
)

private fun MaterialDynamicColors.miscColors(): List<DynamicColor> = listOf(
    shadow(),
    controlActivated(),
    controlNormal(),
    controlHighlight(),
    textPrimaryInverse(),
    textSecondaryAndTertiaryInverse(),
    textPrimaryInverseDisableOnly(),
    textSecondaryAndTertiaryInverseDisabled(),
    textHintInverse(),
)

private fun List<DynamicColor>.variableNamePair(
    contrast: Contrast,
    scheme: DynamicScheme,
    independentColors: List<DynamicColor>,
    includeScheme: Boolean = true
): Map<String, DynamicColor> {
    val contrast = contrast.suffix()
    val mode = if (!includeScheme) "" else {
        if (scheme.isDark) "Dark" else "Light"
    }
    val suffix = "$mode$contrast"
    return associateBy { color ->
        if (independentColors.contains(color)) {
            "${color.name.snakeToCamelCase()}$contrast"
        } else {
            "${color.name.snakeToCamelCase()}$suffix"
        }
    }
}

private fun List<DynamicColor>.toColorVariables(
    contrast: Contrast,
    scheme: DynamicScheme,
    includeScheme: Boolean = true,
    schemeIndependentColors: List<DynamicColor> = MaterialDynamicColors().schemeIndependentColors(false)
): String {
    val values = variableNamePair(contrast, scheme, schemeIndependentColors, includeScheme)
    return buildString {
        values.forEach { (name, color) ->
            appendLine(color.getColor(scheme).variable(name))
        }
    }
}

private fun Contrast.suffix(): String = when (this) {
    Contrast.Reduced -> "ReducedContrast"
    Contrast.Default -> ""
    Contrast.Medium -> "MediumContrast"
    Contrast.High -> "HighContrast"
}
