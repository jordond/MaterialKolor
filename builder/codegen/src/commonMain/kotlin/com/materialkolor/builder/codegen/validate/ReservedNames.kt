package com.materialkolor.builder.codegen.validate

import com.materialkolor.builder.codegen.symbol.Symbol
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.COLOR_FAMILY
import com.materialkolor.builder.codegen.target.FrozenMode
import com.materialkolor.builder.codegen.target.custom.LOCAL_THEME_COLORS
import com.materialkolor.builder.codegen.target.custom.REMEMBER_THEME_COLORS
import com.materialkolor.builder.codegen.target.custom.THEME_COLORS
import com.materialkolor.builder.codegen.target.custom.propertyName
import com.materialkolor.builder.codegen.target.fluent.THEME_SHADES
import com.materialkolor.builder.codegen.target.material3.EXTENDED_COLORS_TYPE
import com.materialkolor.builder.codegen.target.material3.LOCAL_EXTENDED_COLORS
import com.materialkolor.builder.codegen.target.material3.REMEMBER_EXTENDED_COLORS
import com.materialkolor.builder.codegen.target.unstyled.COLORS_PROPERTY
import com.materialkolor.builder.codegen.target.unstyled.THEME_TOKENS
import com.materialkolor.builder.codegen.target.unstyled.accentTokenNames
import com.materialkolor.builder.codegen.target.unstyled.colorsName
import com.materialkolor.builder.codegen.target.unstyled.tokenName
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * A theme or accent name that an export cannot use, because a file it writes refers to something
 * else by that name.
 */
public sealed interface ReservedNameClash {
    /** The name as it was typed. */
    public val name: String

    /**
     * The theme name is taken.
     *
     * @property[name] The theme name.
     */
    public data class ThemeName(
        override val name: String,
    ) : ReservedNameClash

    /**
     * An accent name is taken.
     *
     * @property[index] Where the accent sits in the document's list.
     * @property[name] The accent name.
     */
    public data class AccentName(
        public val index: Int,
        override val name: String,
    ) : ReservedNameClash
}

/**
 * The names a theme or accent cannot take in an export, because the generated files import or
 * declare something by that name.
 *
 * Generated code refers to everything by its simple name, so a theme called `MaterialTheme` would
 * collide with the import it sits next to and the generator would refuse to write it. The domain
 * checks cannot see the symbol table, so the clash is caught here instead, early enough for the
 * export screen to point at the name rather than fail.
 *
 * A name is compared with its first letter in either case, since the export writes an accent both
 * ways, as `brand` for its family and `BrandSeed` for its seed.
 */
public object ReservedNames {
    /**
     * Every name an accent cannot take in [target], whichever mode it is exported in. That is every
     * name the export refers to by its simple name, and the members an accent's family sits beside.
     * [clashes] also checks each Unstyled accent's four flattened token names, which this set alone
     * cannot catch.
     */
    public fun of(target: ExportTarget): Set<String> = topLevel(target) + members(target)

    /** The theme and accent names of [document] that its export target cannot use, theme name first. */
    public fun clashes(document: ThemeDocument): List<ReservedNameClash> {
        val target = ExportTarget.of(document.library, document.expressive)
        val forTheme = topLevel(target).mapTo(mutableSetOf()) { it.folded() }
        val forAccents = of(target).mapTo(mutableSetOf()) { it.folded() }

        return buildList {
            if (document.themeName.folded() in forTheme) add(ReservedNameClash.ThemeName(document.themeName))
            document.accents.forEachIndexed { index, accent ->
                val taken = accent.name.folded() in forAccents ||
                    (target == ExportTarget.Unstyled && flattenedClash(document.accents, index)) // b-111b
                if (taken) add(ReservedNameClash.AccentName(index, accent.name))
            }
        }
    }

    /** What [target] imports or declares at the top of a file, which the theme function sits beside too. */
    private fun topLevel(target: ExportTarget): Set<String> =
        when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive -> Material3Symbols.names() + Material3Declared
            ExportTarget.Unstyled -> UnstyledSymbols.names() + UnstyledDeclared // b-111b
            ExportTarget.Fluent -> FluentSymbols.names() + FluentDeclared // b-111
            ExportTarget.Custom -> CommonSymbols.names() + CustomDeclared + CustomDynamicNames // b-111, b-112b
        }

    /**
     * The members of the class an accent's family is a property of, other than the accents. A theme
     * function cannot clash with a member property, so only accents are held to these.
     */
    private fun members(target: ExportTarget): Set<String> =
        when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive, ExportTarget.Fluent -> {
                emptySet()
            }
            ExportTarget.Unstyled -> {
                UnstyledTokens // b-111b
            }
            ExportTarget.Custom -> {
                CustomMembers
            }
        }

    // b-111b

    /**
     * Whether the accent at [index] shares one of its four flattened token names with a library token
     * or with an earlier accent. Tokens are equal by name, so either one would quietly overwrite the other.
     */
    private fun flattenedClash(
        accents: List<Accent>,
        index: Int,
    ): Boolean {
        val tokens = UnstyledTokens.mapTo(mutableSetOf()) { it.folded() }
        val earlier = accents.take(index).flatMapTo(mutableSetOf()) { accent -> accent.flattened() }

        return accents[index].flattened().any { name -> name in tokens || name in earlier }
    }

    private fun Accent.flattened(): List<String> = accentTokenNames(name).map { it.folded() }

    private fun String.folded(): String = replaceFirstChar { char -> char.uppercaseChar() }

    private fun List<Symbol>.names(): Set<String> = mapTo(mutableSetOf()) { symbol -> symbol.simpleName }
}

/** What every export can name, the Kotlin types, the Compose runtime and graphics, and core. */
private val CommonSymbols: List<Symbol> =
    listOf(
        Symbols.Boolean,
        Symbols.Double,
        Symbols.Int,
        Symbols.String,
        Symbols.Unit,
        Symbols.OptIn,
        Symbols.Composable,
        Symbols.Immutable,
        Symbols.Remember,
        Symbols.StaticCompositionLocalOf,
        Symbols.CompositionLocalProvider,
        Symbols.Color,
        Symbols.IsSystemInDarkTheme,
        Symbols.Tween,
        Symbols.PaletteStyle,
        Symbols.ColorSpec,
        Symbols.DynamicScheme,
        Symbols.TonalPalette,
        Symbols.Harmonize,
        Symbols.RememberTonalPalette,
        Symbols.OnTone,
        Symbols.ToneColor,
        Symbols.ContrastThreshold,
        Symbols.RememberDynamicScheme,
    )

private val Material3Symbols: List<Symbol> =
    CommonSymbols +
        listOf(
            Symbols.MaterialTheme,
            Symbols.MaterialExpressiveTheme,
            Symbols.ColorScheme,
            Symbols.LightColorScheme,
            Symbols.DarkColorScheme,
            Symbols.MotionScheme,
            Symbols.ExperimentalMaterial3ExpressiveApi,
            Symbols.DynamicMaterialTheme,
            Symbols.DynamicMaterialExpressiveTheme,
            Symbols.DynamicMaterialThemeState,
            Symbols.RememberDynamicMaterialThemeState,
            // b-111c
            Symbols.Build,
            Symbols.LocalContext,
            Symbols.DynamicDarkColorScheme,
            Symbols.DynamicLightColorScheme,
        )

/** What the Material 3 export declares in `ExtendedColors.kt`, kept apart since none of it is imported. */
private val Material3Declared: Set<String> =
    setOf(COLOR_FAMILY, EXTENDED_COLORS_TYPE, LOCAL_EXTENDED_COLORS, REMEMBER_EXTENDED_COLORS)

private val UnstyledSymbols: List<Symbol> =
    CommonSymbols +
        listOf(
            Symbols.DynamicColorSchemes,
            Symbols.DynamicColors,
            Symbols.ThemeValues,
            Symbols.ToThemeValues,
            Symbols.MaterialKolorTokens,
            Symbols.BuildThemeV2,
            Symbols.ThemeToken,
            Symbols.ThemeProperty, // b-111b
            Symbols.UnstyledColorScheme,
            Symbols.Map,
            Symbols.MapOf,
        )

private val FluentSymbols: List<Symbol> =
    CommonSymbols +
        listOf(
            Symbols.RememberFluentColors,
            Symbols.AnimateFluentColors,
            Symbols.FluentTheme,
            Symbols.FluentColors,
            Symbols.FluentShades,
        )

// b-111

/** What the Custom export declares. */
private val CustomDeclared: Set<String> = setOf(THEME_COLORS, LOCAL_THEME_COLORS, COLOR_FAMILY)

/** The slots of `ThemeColors`, which an accent becomes a property beside and so cannot share a name with. */
private val CustomMembers: Set<String> = CustomSlot.entries.mapTo(mutableSetOf()) { slot -> slot.propertyName }

/** What the Fluent export declares. */
private val FluentDeclared: Set<String> = setOf(THEME_SHADES)

// b-111b

/** What the Unstyled frozen export declares, the tokens object and a light and dark map per contrast. */
private val UnstyledDeclared: Set<String> =
    ContrastVariant.entries.flatMapTo(mutableSetOf(THEME_TOKENS)) { variant ->
        FrozenMode.entries.map { mode -> colorsName(variant, mode) }
    }

// b-112

/**
 * The token names an Unstyled accent cannot take, all 63 `MaterialKolorTokens` and `colors`.
 *
 * The frozen `ThemeTokens` declares `colors` and a token for each of the 48 roles, named as
 * `MaterialKolorTokens` names them. The 15 library tokens with no [Role] are reserved too, since the
 * dynamic export writes accents into `MaterialKolorTokens.colors` beside them.
 */
internal val UnstyledTokens: Set<String> =
    Role.entries.mapTo(mutableSetOf(COLORS_PROPERTY)) { role -> role.tokenName } +
        listOf(
            "primaryPaletteKeyColor",
            "secondaryPaletteKeyColor",
            "tertiaryPaletteKeyColor",
            "errorPaletteKeyColor",
            "neutralPaletteKeyColor",
            "neutralVariantPaletteKeyColor",
            "shadow",
            "controlActivated",
            "controlNormal",
            "controlHighlight",
            "textPrimaryInverse",
            "textSecondaryAndTertiaryInverse",
            "textPrimaryInverseDisableOnly",
            "textSecondaryAndTertiaryInverseDisabled",
            "textHintInverse",
        )

// b-112b

/** The Custom dynamic export's names beyond the frozen set that can clash. */
private val CustomDynamicNames: Set<String> = setOf(Symbols.MaterialKolors.simpleName, REMEMBER_THEME_COLORS)
