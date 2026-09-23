package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.AnnotationSpec
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.ClassKind
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.KotlinFileScope
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.lambdaType
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.export.AccentColors
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.model.Accent

// The pieces more than one target writes. The theme function serves every frozen export and the
// Fluent dynamic one, the modes serve every frozen export, and the accent family serves the frozen
// exports and the Material 3 dynamic one.

/** The two modes a frozen export writes each set of colors in, light first. */
internal enum class FrozenMode {
    Light,
    Dark,
}

/** The theme function's first parameter, which every frozen theme picks its colors by. */
internal const val IS_DARK_PARAMETER: String = "isDark"

/** The theme function's content parameter. */
internal const val CONTENT_PARAMETER: String = "content"

/** The data class an export declares for the four colors of one accent. */
internal const val COLOR_FAMILY: String = "ColorFamily"

/** The four colors of a family, in the order `ColorFamily` declares them. */
internal val FamilyParts: List<String> = listOf("color", "onColor", "colorContainer", "onColorContainer")

/** The contrast variants the export writes, standard first and then medium and high. */
internal val ResolvedExport.contrastVariants: List<ContrastVariant>
    get() = ContrastVariant.entries.filter { variant -> variant in roles }

/** What a value at this contrast is prefixed with, as in `mediumContrast`, and nothing at the standard one. */
internal val ContrastVariant.namePrefix: String
    get() = when (this) {
        ContrastVariant.Standard -> ""
        ContrastVariant.Medium -> "mediumContrast"
        ContrastVariant.High -> "highContrast"
    }

/** The property an accent's family is read from, as in `brand`. */
internal val Accent.propertyName: String
    get() = accentPropertyName(name)

/** The property a resolved accent's family is read from, the same name its [Accent] gets. */
internal val AccentFamilyValues.propertyName: String
    get() = accentPropertyName(name)

/** The accent's four colors in [mode]. */
internal fun AccentFamilyValues.colorsIn(mode: FrozenMode): AccentColors =
    when (mode) {
        FrozenMode.Light -> light
        FrozenMode.Dark -> dark
    }

/** `if (isDark) dark else light`, which is how a frozen theme picks between its two standard values. */
internal fun byMode(
    light: String,
    dark: String,
): Expression = ifElse(condition = ref(IS_DARK_PARAMETER), whenTrue = ref(dark), whenFalse = ref(light))

/**
 * `@Composable fun AppTheme(isDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`,
 * the theme function every frozen export and the Fluent dynamic export write, with [statements] as
 * its body.
 */
internal fun KotlinFileScope.themeFunction(
    input: ExportInput,
    statements: BodyScope.() -> Unit,
) {
    function(name = input.document.themeName, annotations = listOf(Symbols.Composable)) {
        parameter(IS_DARK_PARAMETER, Symbols.Boolean, default = call(Symbols.IsSystemInDarkTheme))
        parameter(CONTENT_PARAMETER, lambdaType(annotations = listOf(Symbols.Composable)))
        body(statements)
    }
}

/** The `ColorFamily` data class, the one every export with accents declares. */
internal fun KotlinFileScope.colorFamilyClass() {
    classDeclaration(
        name = COLOR_FAMILY,
        kind = ClassKind.DataClass,
        annotations = listOf(AnnotationSpec(Symbols.Immutable)),
    ) {
        FamilyParts.forEach { part -> property(part, Symbols.Color) }
    }
}

/**
 * `ColorFamily(color = ..., onColor = ..., colorContainer = ..., onColorContainer = ...)` with the
 * accent's literal colors. The domain's container and on container land in the family's
 * `colorContainer` and `onColorContainer`.
 */
internal fun colorFamilyValue(colors: AccentColors): Expression {
    val values = listOf(colors.color, colors.onColor, colors.container, colors.onContainer)

    return call(COLOR_FAMILY, multiline = true) {
        FamilyParts.zip(values).forEach { (part, color) -> argument(part, Literals.colorLiteral(color.value)) }
    }
}

private fun accentPropertyName(name: String): String = name.replaceFirstChar { char -> char.lowercaseChar() }
