package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.AnnotationSpec
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.ClassKind
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.KotlinFileScope
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.lambdaType
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.AccentColors
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.export.RoleTable
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RoleGroup
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Material 3 export that writes every color out as a literal, plain or expressive.
 *
 * It writes `Color.kt` with every role in both modes, `Theme.kt` with a light and a dark scheme for
 * each contrast variant and a theme function that uses the standard pair, and `ExtendedColors.kt`
 * when the theme has accents. Nothing it writes needs MaterialKolor, and nothing is worked out here,
 * every color comes from the resolved export as it is.
 */
public object Material3Frozen {
    /** Every file the export of [input] writes, in the order a reader would open them. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        val target = input.target
        require(target == ExportTarget.Material3 || target == ExportTarget.Material3Expressive) {
            "The Material 3 frozen export cannot write a $target theme"
        }

        return listOfNotNull(colorFile(input), themeFile(input), extendedColorsFile(input))
    }
}

/** The two modes a frozen export writes each set of colors in, light first. */
internal enum class FrozenMode {
    Light,
    Dark,
}

/** The theme function's first parameter, which every frozen theme picks its colors by. */
internal const val IS_DARK_PARAMETER: String = "isDark"

/** The theme function's content parameter. */
internal const val CONTENT_PARAMETER: String = "content"

private const val EXTENDED_COLORS = "extendedColors"
private const val EXTENDED_LIGHT = "extendedLight"
private const val EXTENDED_DARK = "extendedDark"

/** Every role in the order `lightColorScheme` and `darkColorScheme` take them, which `Color.kt` follows too. */
private val SchemeOrder: List<Role> =
    listOf(
        Role.Primary,
        Role.OnPrimary,
        Role.PrimaryContainer,
        Role.OnPrimaryContainer,
        Role.InversePrimary,
        Role.Secondary,
        Role.OnSecondary,
        Role.SecondaryContainer,
        Role.OnSecondaryContainer,
        Role.Tertiary,
        Role.OnTertiary,
        Role.TertiaryContainer,
        Role.OnTertiaryContainer,
        Role.Background,
        Role.OnBackground,
        Role.Surface,
        Role.OnSurface,
        Role.SurfaceVariant,
        Role.OnSurfaceVariant,
        Role.SurfaceTint,
        Role.InverseSurface,
        Role.InverseOnSurface,
        Role.Error,
        Role.OnError,
        Role.ErrorContainer,
        Role.OnErrorContainer,
        Role.Outline,
        Role.OutlineVariant,
        Role.Scrim,
        Role.SurfaceBright,
        Role.SurfaceContainer,
        Role.SurfaceContainerHigh,
        Role.SurfaceContainerHighest,
        Role.SurfaceContainerLow,
        Role.SurfaceContainerLowest,
        Role.SurfaceDim,
    ) + Role.entries.filter { role -> role.group == RoleGroup.Fixed }

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
internal val AccentFamilyValues.propertyName: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }

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
 * the one theme function every frozen export writes, with [statements] as its body.
 */
internal fun KotlinFileScope.frozenThemeFunction(
    input: ExportInput,
    statements: BodyScope.() -> Unit,
) {
    function(name = input.document.themeName, annotations = listOf(Symbols.Composable)) {
        parameter(IS_DARK_PARAMETER, Symbols.Boolean, default = call(Symbols.IsSystemInDarkTheme))
        parameter(CONTENT_PARAMETER, lambdaType(annotations = listOf(Symbols.Composable)))
        body(statements)
    }
}

/** The `ColorFamily` data class, the same one the dynamic export declares for its accents. */
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

private fun colorFile(input: ExportInput): GeneratedFile {
    val resolved = input.resolved

    return kotlinFile(path = input.sourcePath("Color.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input))
        resolved.contrastVariants.forEach { variant ->
            val table = resolved.roles.getValue(variant)
            FrozenMode.entries.forEach { mode ->
                val colors = table.colorsIn(mode)
                SchemeOrder.forEach { role ->
                    property(roleValueName(role, mode, variant), Literals.colorLiteral(colors.getValue(role).value))
                }
            }
        }
    }
}

private fun themeFile(input: ExportInput): GeneratedFile {
    val variants = input.resolved.contrastVariants
    val standardPair = "${schemeName(ContrastVariant.Standard, FrozenMode.Light)} and " +
        schemeName(ContrastVariant.Standard, FrozenMode.Dark)

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input))
        variants.forEach { variant ->
            if (variant == variants.firstOrNull { other -> other != ContrastVariant.Standard }) {
                comment(
                    "The schemes below are the other contrast variants, to swap in for $standardPair " +
                        "in ${input.document.themeName}.",
                )
            }
            FrozenMode.entries.forEach { mode -> property(schemeName(variant, mode), schemeCall(variant, mode)) }
        }
        frozenThemeFunction(input) { themeBody(input) }
    }
}

/**
 * The theme function's body. Accents are provided around the theme so that
 * `LocalExtendedColors.current` works anywhere inside it.
 */
private fun BodyScope.themeBody(input: ExportInput) {
    val hasAccents = input.resolved.accents.isNotEmpty()
    val theme = themeCall(input)

    if (hasAccents) {
        assign(EXTENDED_COLORS, byMode(EXTENDED_LIGHT, EXTENDED_DARK))
        blankLine()
        call(Symbols.CompositionLocalProvider) {
            argument(infix(ref(LOCAL_EXTENDED_COLORS), "provides", ref(EXTENDED_COLORS)))
            trailingLambda { statement(theme) }
        }
    } else {
        statement(theme)
    }
}

/** `MaterialTheme(...)`, or `MaterialExpressiveTheme(...)` with the document's motion scheme. */
private fun themeCall(input: ExportInput): Expression {
    val colorScheme = byMode(
        light = schemeName(ContrastVariant.Standard, FrozenMode.Light),
        dark = schemeName(ContrastVariant.Standard, FrozenMode.Dark),
    )
    val content = ref(CONTENT_PARAMETER)

    return if (input.target == ExportTarget.Material3Expressive) {
        call(Symbols.MaterialExpressiveTheme, multiline = true) {
            argument("colorScheme", colorScheme)
            argument("motionScheme", motionSchemeExpression(input.document.motionScheme))
            argument(CONTENT_PARAMETER, content)
        }
    } else {
        call(Symbols.MaterialTheme, multiline = true) {
            argument("colorScheme", colorScheme)
            argument(CONTENT_PARAMETER, content)
        }
    }
}

private fun motionSchemeExpression(choice: MotionSchemeChoice): Expression {
    val motionScheme = ref(Symbols.MotionScheme)

    return when (choice) {
        MotionSchemeChoice.Standard -> motionScheme.call("standard")
        MotionSchemeChoice.Expressive -> motionScheme.call("expressive")
    }
}

/** `lightColorScheme(primary = primaryLight, ...)` with every role, in the order the function takes them. */
private fun schemeCall(
    variant: ContrastVariant,
    mode: FrozenMode,
): Expression {
    val function = when (mode) {
        FrozenMode.Light -> Symbols.LightColorScheme
        FrozenMode.Dark -> Symbols.DarkColorScheme
    }

    return call(function, multiline = true) {
        SchemeOrder.forEach { role -> argument(role.schemeParameter, ref(roleValueName(role, mode, variant))) }
    }
}

/** `ExtendedColors.kt` with a literal family per accent in each mode, or nothing when the theme has none. */
private fun extendedColorsFile(input: ExportInput): GeneratedFile? {
    val accents = input.resolved.accents
    if (accents.isEmpty()) return null

    return kotlinFile(path = input.sourcePath("ExtendedColors.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input))
        colorFamilyClass()
        classDeclaration(
            name = EXTENDED_COLORS_TYPE,
            kind = ClassKind.DataClass,
            annotations = listOf(AnnotationSpec(Symbols.Immutable)),
        ) {
            accents.forEach { accent -> property(accent.propertyName, type(COLOR_FAMILY)) }
        }
        property(
            name = LOCAL_EXTENDED_COLORS,
            value = call(Symbols.StaticCompositionLocalOf) {
                typeArgument(type(EXTENDED_COLORS_TYPE))
                trailingLambda {
                    val message = "ExtendedColors are only provided inside ${input.document.themeName}"
                    call("error") { argument(Literals.string(message)) }
                }
            },
        )
        FrozenMode.entries.forEach { mode ->
            val name = when (mode) {
                FrozenMode.Light -> EXTENDED_LIGHT
                FrozenMode.Dark -> EXTENDED_DARK
            }
            property(
                name = name,
                value = call(EXTENDED_COLORS_TYPE, multiline = true) {
                    accents.forEach { accent -> argument(accent.propertyName, colorFamilyValue(accent.colorsIn(mode))) }
                },
            )
        }
    }
}

private fun RoleTable.colorsIn(mode: FrozenMode): Map<Role, Argb> =
    when (mode) {
        FrozenMode.Light -> light
        FrozenMode.Dark -> dark
    }

/** `primaryLight`, or `primaryDarkHighContrast` for a role at another contrast. */
private fun roleValueName(
    role: Role,
    mode: FrozenMode,
    variant: ContrastVariant,
): String = role.schemeParameter + mode.name + variant.namePrefix.replaceFirstChar { char -> char.uppercaseChar() }

/** `lightScheme` and `darkScheme` at the standard contrast, `mediumContrastLightColorScheme` and the like otherwise. */
private fun schemeName(
    variant: ContrastVariant,
    mode: FrozenMode,
): String =
    when (variant) {
        ContrastVariant.Standard -> "${mode.name.lowercase()}Scheme"
        ContrastVariant.Medium, ContrastVariant.High -> "${variant.namePrefix}${mode.name}ColorScheme"
    }

/** The scheme parameter for this role, as in `surfaceContainerHigh`. */
private val Role.schemeParameter: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }
