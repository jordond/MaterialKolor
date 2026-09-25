package com.materialkolor.builder.codegen.symbol

import com.materialkolor.builder.codegen.dsl.ArgumentsScope
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style

/**
 * One parameter default of a library function, as the document would say it and as the library
 * writes it.
 *
 * @property[parameter] The parameter's name, which is also the name the argument is written with.
 * @property[value] The document value that gives the same result as leaving the argument out.
 * @property[source] The default exactly as the library's signature spells it. `LibrarySymbolsTest`
 * holds the table to the library source through this.
 */
public class DefaultArgument<T>(
    public val parameter: String,
    public val value: T,
    public val source: String,
) {
    /**
     * Whether passing [candidate] would only repeat what the function does anyway.
     */
    public fun isDefault(candidate: T): Boolean = candidate == value

    override fun toString(): String = "$parameter = $source"
}

/**
 * The argument for [default], left out when [value] is what the called function would pick anyway.
 *
 * Omission is always relative to the function actually being called, so the same document writes
 * `style` for one wrapper and leaves it out for another.
 */
public fun <T> ArgumentsScope.optionalArgument(
    default: DefaultArgument<T>,
    value: T,
    expression: (T) -> Expression,
) {
    optionalArgument(default.parameter, if (default.isDefault(value)) null else expression(value))
}

/**
 * The defaults of a function that builds a scheme from a seed, which is what every dynamic export
 * calls in the end.
 *
 * A parameter the function does not take is null, so a target cannot write an argument the
 * function would not compile with.
 *
 * @property[function] The function these defaults belong to.
 * @property[style] The palette style.
 * @property[contrastLevel] The contrast level.
 * @property[specVersion] The Material spec.
 * @property[platform] The device the scheme is tuned for.
 * @property[isAmoled] Whether dark surfaces drop to black.
 * @property[animate] Whether color changes animate.
 * @property[motionScheme] The motion scheme. Its default is null, which no document choice
 * matches, so the choice is always written.
 */
public class SchemeDefaults(
    public val function: Symbol,
    public val style: DefaultArgument<Style>,
    public val contrastLevel: DefaultArgument<ContrastLevel>,
    public val specVersion: DefaultArgument<SpecVersion>,
    public val platform: DefaultArgument<SchemePlatform>,
    public val isAmoled: DefaultArgument<Boolean>? = null,
    public val animate: DefaultArgument<Boolean>? = null,
    public val motionScheme: DefaultArgument<MotionSchemeChoice?>? = null,
) {
    /**
     * Every default above that the function has.
     */
    public val arguments: List<DefaultArgument<*>>
        get() = listOfNotNull(style, contrastLevel, specVersion, platform, isAmoled, animate, motionScheme)
}

/**
 * The defaults of every library function an export calls, so an argument can be left out exactly
 * when the called function would pick the same value on its own.
 *
 * `DynamicMaterialExpressiveTheme` defaults to the expressive style and the 2025 spec, while
 * `DynamicMaterialTheme` defaults to tonal spot and 2021. An expressive export of a tonal spot 2021
 * document therefore writes both arguments, and a plain one writes neither.
 */
public object DefaultArguments {
    public val DynamicMaterialTheme: SchemeDefaults = SchemeDefaults(
        function = Symbols.DynamicMaterialTheme,
        style = tonalSpot(),
        contrastLevel = standardContrast(),
        specVersion = defaultSpec(),
        platform = defaultPlatform(),
        isAmoled = notAmoled(),
        animate = notAnimated(),
    )

    public val DynamicMaterialExpressiveTheme: SchemeDefaults = SchemeDefaults(
        function = Symbols.DynamicMaterialExpressiveTheme,
        style = DefaultArgument("style", Style.Expressive, "PaletteStyle.Expressive"),
        contrastLevel = standardContrast(),
        specVersion = DefaultArgument("specVersion", SpecVersion.Spec2025, "ColorSpec.SpecVersion.SPEC_2025"),
        platform = defaultPlatform(),
        isAmoled = notAmoled(),
        animate = notAnimated(),
        motionScheme = DefaultArgument("motionScheme", null, "null"),
    )

    public val RememberDynamicMaterialThemeState: SchemeDefaults = SchemeDefaults(
        function = Symbols.RememberDynamicMaterialThemeState,
        style = tonalSpot(),
        contrastLevel = standardContrast(),
        specVersion = defaultSpec(),
        platform = defaultPlatform(),
        isAmoled = notAmoled(),
    )

    public val RememberFluentColors: SchemeDefaults = SchemeDefaults(
        function = Symbols.RememberFluentColors,
        style = tonalSpot(),
        contrastLevel = standardContrast(),
        specVersion = defaultSpec(),
        platform = defaultPlatform(),
    )

    public val DynamicColorSchemes: SchemeDefaults = SchemeDefaults(
        function = Symbols.DynamicColorSchemes,
        style = tonalSpot(),
        contrastLevel = standardContrast(),
        specVersion = defaultSpec(),
        platform = defaultPlatform(),
    )

    /**
     * Core's own `rememberDynamicScheme`, which the Unstyled export calls when it writes its schemes
     * out and the Fluent export calls when it maps the shades itself. Core spells its style default
     * without the class name.
     */
    public val RememberDynamicScheme: SchemeDefaults = SchemeDefaults(
        function = Symbols.RememberDynamicScheme,
        style = DefaultArgument("style", Style.TonalSpot, "TonalSpot"),
        contrastLevel = standardContrast(),
        specVersion = defaultSpec(),
        platform = defaultPlatform(),
    )

    /**
     * The contrast `TonalPalette.onTone` aims for when an accent does not say.
     */
    public val OnToneThreshold: DefaultArgument<OnColorThreshold> =
        DefaultArgument("threshold", OnColorThreshold.AaNormal, "ContrastThreshold.WCAG_AA_NORMAL_TEXT")

    /**
     * Whether `rememberTonalPalette` harmonizes. The library leaves the seed alone unless it is
     * handed a color to harmonize with, which an accent that does not harmonize never passes.
     */
    public val RememberTonalPaletteHarmonizeWith: DefaultArgument<Boolean> =
        DefaultArgument("harmonizeWith", false, "null")

    /**
     * Whether `Color.harmonize` also matches saturation.
     */
    public val HarmonizeMatchSaturation: DefaultArgument<Boolean> =
        DefaultArgument("matchSaturation", false, "false")

    /**
     * Whether the `MaterialKolors` constructor drops dark surfaces to black.
     */
    public val MaterialKolorsIsAmoled: DefaultArgument<Boolean> = notAmoled()

    /**
     * Every default in this table, by the function it belongs to.
     */
    public val all: Map<Symbol, List<DefaultArgument<*>>>
        get() = listOf(
            DynamicMaterialTheme,
            DynamicMaterialExpressiveTheme,
            RememberDynamicMaterialThemeState,
            RememberFluentColors,
            DynamicColorSchemes,
            RememberDynamicScheme,
        ).associate { defaults -> defaults.function to defaults.arguments } +
            mapOf(
                Symbols.OnTone to listOf(OnToneThreshold),
                Symbols.RememberTonalPalette to listOf(RememberTonalPaletteHarmonizeWith),
                Symbols.Harmonize to listOf(HarmonizeMatchSaturation),
                Symbols.MaterialKolors to listOf(MaterialKolorsIsAmoled),
            )

    private fun tonalSpot(): DefaultArgument<Style> =
        DefaultArgument("style", Style.TonalSpot, "PaletteStyle.TonalSpot")

    private fun standardContrast(): DefaultArgument<ContrastLevel> =
        DefaultArgument("contrastLevel", ContrastLevel.Standard, "Contrast.Default.value")

    private fun defaultSpec(): DefaultArgument<SpecVersion> =
        DefaultArgument("specVersion", SpecVersion.Spec2021, "ColorSpec.SpecVersion.Default")

    private fun defaultPlatform(): DefaultArgument<SchemePlatform> =
        DefaultArgument("platform", SchemePlatform.Phone, "DynamicScheme.Platform.Default")

    private fun notAmoled(): DefaultArgument<Boolean> = DefaultArgument("isAmoled", false, "false")

    private fun notAnimated(): DefaultArgument<Boolean> = DefaultArgument("animate", false, "false")
}
