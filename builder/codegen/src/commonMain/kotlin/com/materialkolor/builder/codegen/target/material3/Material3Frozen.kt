package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.FrozenMode
import com.materialkolor.builder.codegen.target.byMode
import com.materialkolor.builder.codegen.target.colorFamilyValue
import com.materialkolor.builder.codegen.target.colorsIn
import com.materialkolor.builder.codegen.target.contrastVariants
import com.materialkolor.builder.codegen.target.namePrefix
import com.materialkolor.builder.codegen.target.propertyName
import com.materialkolor.builder.codegen.target.themeFunction
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RoleGroup
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Material 3 export that writes every color out as a literal, plain or expressive.
 *
 * It writes `Color.kt` with every role in both modes, `Theme.kt` with a light and a dark scheme for
 * each contrast variant and a theme function that uses the standard pair, and `ExtendedColors.kt`
 * when the theme has accents. Nothing it writes needs MaterialKolor, and nothing is worked out here,
 * every color comes from the resolved export as it is. An Android export can also ask for the
 * wallpaper colors, which then win over the standard pair from Android 12 on.
 */
public object Material3Frozen {
    /**
     * Every file the export of [input] writes, in the order a reader would open them.
     */
    public fun files(input: ExportInput): List<GeneratedFile> {
        val target = input.target
        require(target == ExportTarget.Material3 || target == ExportTarget.Material3Expressive) {
            "The Material 3 frozen export cannot write a $target theme"
        }

        return listOfNotNull(colorFile(input), themeFile(input), extendedColorsFile(input))
    }
}

private const val EXTENDED_COLORS = "extendedColors"
private const val EXTENDED_LIGHT = "extendedLight"
private const val EXTENDED_DARK = "extendedDark"

/**
 * Every role in the order `lightColorScheme` and `darkColorScheme` take them, which `Color.kt` follows too.
 */
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

private fun colorFile(input: ExportInput): GeneratedFile {
    val resolved = input.resolved

    return kotlinFile(path = input.sourcePath("Color.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
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

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        variants.forEach { variant ->
            FrozenMode.entries.forEach { mode -> property(schemeName(variant, mode), schemeCall(variant, mode)) }
        }
        themeFunction(input, dynamicColor = input.writesAndroidDynamicColor) { themeBody(input) }
    }
}

/**
 * The theme function's body. Accents are provided around the theme so that
 * `LocalExtendedColors.current` works anywhere inside it, the wallpaper branch included.
 */
private fun BodyScope.themeBody(input: ExportInput) {
    val hasAccents = input.resolved.accents.isNotEmpty()
    val dynamicColor = input.writesAndroidDynamicColor
    val literal = themeCall(input)
    val theme = if (dynamicColor) androidDynamicColorBranch(input, literal) else literal

    if (hasAccents) assign(EXTENDED_COLORS, byMode(EXTENDED_LIGHT, EXTENDED_DARK))
    if (dynamicColor) assignContext()
    if (hasAccents || dynamicColor) blankLine()

    if (hasAccents) {
        call(Symbols.CompositionLocalProvider) {
            argument(infix(ref(LOCAL_EXTENDED_COLORS), "provides", ref(EXTENDED_COLORS)))
            trailingLambda { statement(theme) }
        }
    } else {
        statement(theme)
    }
}

/**
 * The theme call on the standard pair.
 */
private fun themeCall(input: ExportInput): Expression =
    materialThemeCall(
        input = input,
        colorScheme = byMode(
            light = schemeName(ContrastVariant.Standard, FrozenMode.Light),
            dark = schemeName(ContrastVariant.Standard, FrozenMode.Dark),
        ),
    )

/**
 * `lightColorScheme(primary = primaryLight, ...)` with every role, in the order the function takes them.
 */
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

/**
 * `ExtendedColors.kt` with a literal family per accent in each mode, or nothing when the theme has none.
 */
private fun extendedColorsFile(input: ExportInput): GeneratedFile? {
    val accents = input.resolved.accents
    if (accents.isEmpty()) return null

    return kotlinFile(path = input.sourcePath("ExtendedColors.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        extendedColorsDeclarations(input, accents.map { accent -> accent.propertyName })
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

/**
 * `primaryLight`, or `primaryDarkHighContrast` for a role at another contrast.
 */
private fun roleValueName(
    role: Role,
    mode: FrozenMode,
    variant: ContrastVariant,
): String = role.schemeParameter + mode.name + variant.namePrefix.replaceFirstChar { char -> char.uppercaseChar() }

/**
 * `lightScheme` and `darkScheme` at the standard contrast, `mediumContrastLightColorScheme` and the like otherwise.
 */
private fun schemeName(
    variant: ContrastVariant,
    mode: FrozenMode,
): String =
    when (variant) {
        ContrastVariant.Standard -> "${mode.name.lowercase()}Scheme"
        ContrastVariant.Medium, ContrastVariant.High -> "${variant.namePrefix}${mode.name}ColorScheme"
    }

/**
 * The scheme parameter for this role, as in `surfaceContainerHigh`.
 */
private val Role.schemeParameter: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }
