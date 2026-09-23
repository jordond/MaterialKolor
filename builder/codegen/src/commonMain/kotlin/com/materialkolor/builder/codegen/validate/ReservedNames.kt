package com.materialkolor.builder.codegen.validate

import com.materialkolor.builder.codegen.symbol.Symbol
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.custom.LOCAL_THEME_COLORS
import com.materialkolor.builder.codegen.target.custom.THEME_COLORS
import com.materialkolor.builder.codegen.target.custom.propertyName
import com.materialkolor.builder.codegen.target.fluent.THEME_SHADES
import com.materialkolor.builder.codegen.target.material3.COLOR_FAMILY
import com.materialkolor.builder.codegen.target.material3.EXTENDED_COLORS_TYPE
import com.materialkolor.builder.codegen.target.material3.LOCAL_EXTENDED_COLORS
import com.materialkolor.builder.codegen.target.material3.REMEMBER_EXTENDED_COLORS
import com.materialkolor.builder.domain.model.CustomSlot
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
    /** Every name [target] refers to by its simple name, whichever mode it is exported in. */
    public fun of(target: ExportTarget): Set<String> =
        when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive -> Material3Symbols.names() + Material3Declared
            ExportTarget.Unstyled -> UnstyledSymbols.names()
            ExportTarget.Fluent -> FluentSymbols.names() + FluentDeclared // b-111
            ExportTarget.Custom -> CommonSymbols.names() + CustomDeclared // b-111
        }

    /** The theme and accent names of [document] that its export target cannot use, theme name first. */
    public fun clashes(document: ThemeDocument): List<ReservedNameClash> {
        val reserved = of(ExportTarget.of(document.library, document.expressive)).mapTo(mutableSetOf()) { it.folded() }

        return buildList {
            if (document.themeName.folded() in reserved) add(ReservedNameClash.ThemeName(document.themeName))
            document.accents.forEachIndexed { index, accent ->
                if (accent.name.folded() in reserved) add(ReservedNameClash.AccentName(index, accent.name))
            }
        }
    }

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

/**
 * What the Custom export declares. An accent becomes a property of `ThemeColors` beside the slots,
 * so it cannot share a slot's name either.
 */
private val CustomDeclared: Set<String> =
    CustomSlot.entries.mapTo(mutableSetOf()) { slot -> slot.propertyName } +
        setOf(THEME_COLORS, LOCAL_THEME_COLORS, COLOR_FAMILY)

/** What the Fluent export declares. */
private val FluentDeclared: Set<String> = setOf(THEME_SHADES)
