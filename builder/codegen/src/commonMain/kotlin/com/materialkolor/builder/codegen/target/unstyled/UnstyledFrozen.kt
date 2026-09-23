package com.materialkolor.builder.codegen.target.unstyled

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.index
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.Symbol
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.FrozenMode
import com.materialkolor.builder.codegen.target.colorsIn
import com.materialkolor.builder.codegen.target.contrastVariants
import com.materialkolor.builder.codegen.target.namePrefix
import com.materialkolor.builder.codegen.target.propertyName
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.AccentColors
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Unstyled export that writes every color out as a literal token map.
 *
 * It writes `Tokens.kt` with a token per role and four per accent, `Color.kt` with a light and a dark
 * map for each contrast variant, and `Theme.kt` with a theme that uses the standard pair. Nothing it
 * writes needs MaterialKolor, and every color comes from the resolved export as it is.
 * The 15 `MaterialKolorTokens` with no [Role] behind them have no resolved value and are left out.
 */
public object UnstyledFrozen {
    /** Every file the export of [input] writes, in the order a reader would open them. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Unstyled) {
            "The Unstyled frozen export cannot write a ${input.target} theme"
        }

        return listOf(tokensFile(input), colorFile(input), themeFile(input))
    }
}

/** The object `Tokens.kt` declares. */
internal const val THEME_TOKENS: String = "ThemeTokens"

/** The property on [THEME_TOKENS] every map is written into. */
internal const val COLORS_PROPERTY: String = "colors"

/** `lightColors` and `darkColors` at the standard contrast, `mediumContrastLightColors` and the like otherwise. */
internal fun colorsName(
    variant: ContrastVariant,
    mode: FrozenMode,
): String =
    when (variant) {
        ContrastVariant.Standard -> "${mode.name.lowercase()}Colors"
        ContrastVariant.Medium, ContrastVariant.High -> "${variant.namePrefix}${mode.name}Colors"
    }

/**
 * The four token names an accent is flattened into, as in `brand`, `onBrand`, `brandContainer` and
 * `onBrandContainer`, in the order the domain's [AccentColors] holds them.
 */
internal fun accentTokenNames(accentName: String): List<String> {
    val lower = accentName.replaceFirstChar { char -> char.lowercaseChar() }
    val upper = accentName.replaceFirstChar { char -> char.uppercaseChar() }

    return listOf(lower, "on$upper", "${lower}Container", "on${upper}Container")
}

private fun tokensFile(input: ExportInput): GeneratedFile =
    kotlinFile(path = input.sourcePath("Tokens.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        objectDeclaration(name = THEME_TOKENS) {
            property(COLORS_PROPERTY, tokenCall(Symbols.ThemeProperty, COLORS_PROPERTY))
            tokenNames(input).forEach { name -> property(name, tokenCall(Symbols.ThemeToken, name)) }
        }
    }

private fun colorFile(input: ExportInput): GeneratedFile {
    val resolved = input.resolved
    val mapType = type(Symbols.Map, type(Symbols.ThemeToken, type(Symbols.Color)), type(Symbols.Color))

    return kotlinFile(path = input.sourcePath("Color.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        resolved.contrastVariants.forEach { variant ->
            val table = resolved.roles.getValue(variant)
            FrozenMode.entries.forEach { mode ->
                val roles = table.colorsIn(mode)
                val values = Role.entries.map { role -> roles.getValue(role) } +
                    resolved.accents.flatMap { accent -> accent.colorsIn(mode).asList() }
                property(
                    name = colorsName(variant, mode),
                    value = call(Symbols.MapOf, multiline = true) {
                        tokenNames(input).zip(values).forEach { (name, color) ->
                            argument(infix(token(name), "to", Literals.colorLiteral(color.value)))
                        }
                    },
                    type = mapType,
                )
            }
        }
    }
}

private fun themeFile(input: ExportInput): GeneratedFile {
    val others = input.resolved.contrastVariants.filter { variant -> variant != ContrastVariant.Standard }
    val colors = ref("properties").index(token(COLORS_PROPERTY))

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        if (others.isNotEmpty()) {
            comment(
                "The ${others.joinToString(" and ") { variant -> variant.namePrefix }} maps in Color.kt " +
                    "can be swapped in for ${colorsName(ContrastVariant.Standard, FrozenMode.Light)} and " +
                    "${colorsName(ContrastVariant.Standard, FrozenMode.Dark)}.",
            )
        }
        property(
            name = input.document.themeName,
            value = call(Symbols.BuildThemeV2) {
                trailingLambda {
                    reassign(colors, ref(colorsName(ContrastVariant.Standard, FrozenMode.Light)))
                    blankLine()
                    call("colorScheme") {
                        argument(ref(Symbols.UnstyledColorScheme).member("Dark"))
                        trailingLambda {
                            reassign(colors, ref(colorsName(ContrastVariant.Standard, FrozenMode.Dark)))
                        }
                    }
                }
            },
        )
    }
}

/** Every token `ThemeTokens` declares after `colors`, the roles in the domain's order and then the accents. */
private fun tokenNames(input: ExportInput): List<String> =
    Role.entries.map { role -> role.tokenName } +
        input.resolved.accents.flatMap { accent -> accentTokenNames(accent.propertyName) }

/** `ThemeToken<Color>("primary")`, or the same for the property. */
private fun tokenCall(
    symbol: Symbol,
    name: String,
): Expression =
    call(symbol) {
        typeArgument(type(Symbols.Color))
        argument(Literals.string(name))
    }

/** `ThemeTokens.primary`. */
private fun token(name: String): Expression = ref(THEME_TOKENS).member(name)

private fun AccentColors.asList(): List<Argb> = listOf(color, onColor, container, onContainer)

/** The token for this role, named as `MaterialKolorTokens` names it, as in `surfaceContainerHigh`. */
internal val Role.tokenName: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }
