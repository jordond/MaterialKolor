package com.materialkolor.builder.codegen.target.unstyled

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.index
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.DefaultArguments
import com.materialkolor.builder.codegen.symbol.SchemeDefaults
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.symbol.optionalArgument
import com.materialkolor.builder.codegen.target.material3.KeyColorOrder
import com.materialkolor.builder.codegen.target.material3.SEED_COLOR
import com.materialkolor.builder.codegen.target.material3.dynamicColorFile
import com.materialkolor.builder.codegen.target.material3.harmonizes
import com.materialkolor.builder.codegen.target.material3.paletteName
import com.materialkolor.builder.codegen.target.material3.seedName
import com.materialkolor.builder.codegen.target.material3.thresholdExpression
import com.materialkolor.builder.codegen.target.propertyName
import com.materialkolor.builder.codegen.target.schemeArguments
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Unstyled export that builds its schemes from the seed at runtime.
 *
 * It writes the same `Color.kt` as the Material 3 dynamic export, `Theme.kt` with a `buildThemeV2`
 * theme, and `Tokens.kt` with four tokens per accent when the theme has accents.
 *
 * A theme with neither pins nor accents hands everything to `dynamicColorSchemes`. A theme with
 * either writes both schemes out and lays its own tokens over each, because `colorScheme` replaces
 * the dark override it is handed rather than adding to it, so a second dark block would undo the
 * one `dynamicColorSchemes` sets. Unstyled has no AMOLED switch yet, so AMOLED is never written.
 */
public object UnstyledDynamic {
    /**
     * Every file the export of [input] writes, in the order a reader would open them.
     */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Unstyled) {
            "The Unstyled dynamic export cannot write a ${input.target} theme"
        }

        return listOfNotNull(dynamicColorFile(input), themeFile(input), tokensFile(input))
    }
}

private const val TRANSITION_SPEC = "colorSchemeTransitionSpec"
private const val PROPERTIES = "properties"
private const val LIGHT_SCHEME = "lightScheme"
private const val DARK_SCHEME = "darkScheme"

private fun themeFile(input: ExportInput): GeneratedFile {
    val document = input.document
    val prefs = input.prefs

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = prefs.packageName) {
        header(Header.lines(input, ExportMode.Dynamic))
        property(
            name = document.themeName,
            value = call(Symbols.BuildThemeV2) {
                trailingLambda {
                    if (prefs.animate) {
                        val duration = Literals.int(prefs.animationDurationMs)
                        reassign(ref(TRANSITION_SPEC), callOf(Symbols.Tween) { argument("durationMillis", duration) })
                        blankLine()
                    }
                    if (document.pins.isEmpty() && document.accents.isEmpty()) {
                        val defaults = DefaultArguments.DynamicColorSchemes
                        call(Symbols.DynamicColorSchemes, multiline = document.overridesScheme(defaults)) {
                            schemeArguments(document, defaults, seed = ref(SEED_COLOR), isDark = null)
                        }
                    } else {
                        explicitSchemes(document)
                    }
                }
            },
        )
    }
}

/**
 * Both schemes, the accent ramps, and the light values as the base with the dark values as the
 * dark override, each with the pins and accents of that mode laid over it and remembered the way
 * `dynamicColorSchemes` remembers its own.
 */
private fun BodyScope.explicitSchemes(document: ThemeDocument) {
    val colors = ref(PROPERTIES).index(ref(Symbols.MaterialKolorTokens).member(COLORS_PROPERTY))

    listOf(false, true).forEach { isDark ->
        assign(
            name = schemeName(isDark),
            value = callOf(Symbols.RememberDynamicScheme, multiline = true) {
                schemeArguments(
                    document = document,
                    defaults = DefaultArguments.RememberDynamicScheme,
                    seed = ref(SEED_COLOR),
                    isDark = Literals.boolean(isDark),
                )
            },
        )
    }
    document.accents.forEach { accent -> assign(accent.paletteName, paletteCall(accent)) }
    blankLine()
    reassign(colors, rememberedValues(document, isDark = false))
    blankLine()
    call("colorScheme") {
        argument(ref(Symbols.UnstyledColorScheme).member("Dark"))
        trailingLambda { reassign(colors, rememberedValues(document, isDark = true)) }
    }
}

/**
 * `remember(lightScheme, brandPalette) { ... }` around the values of one mode. It is keyed on that
 * mode's scheme and every accent ramp, so the map and the accents' `onTone` searches only run again
 * when one of them changes.
 */
private fun rememberedValues(
    document: ThemeDocument,
    isDark: Boolean,
): Expression =
    call(Symbols.Remember) {
        argument(ref(schemeName(isDark)))
        document.accents.forEach { accent -> argument(ref(accent.paletteName)) }
        trailingLambda { statement(schemeValues(document, isDark)) }
    }

/**
 * Whether a scheme call for this document has anything to say beyond its seed.
 */
private fun ThemeDocument.overridesScheme(defaults: SchemeDefaults): Boolean =
    KeyColorOrder.any { keyColor -> keyColors[keyColor] != null } ||
        !defaults.style.isDefault(style) ||
        !defaults.contrastLevel.isDefault(contrast) ||
        !defaults.specVersion.isDefault(spec) ||
        !defaults.platform.isDefault(platform)

/**
 * `rememberTonalPalette(seed = BrandSeed, harmonizeWith = SeedColor)`, the same ramp the Material 3 export builds.
 */
private fun paletteCall(accent: Accent): Expression =
    call(Symbols.RememberTonalPalette, multiline = accent.harmonizes) {
        argument("seed", ref(accent.seedName))
        optionalArgument(DefaultArguments.RememberTonalPaletteHarmonizeWith, accent.harmonize) { ref(SEED_COLOR) }
    }

/**
 * `lightScheme.toThemeValues()`, with `+ mapOf(...)` for the pinned roles and accent tokens of the
 * mode when there are any. A role pinned in one mode only keeps the generated color in the other.
 */
private fun schemeValues(
    document: ThemeDocument,
    isDark: Boolean,
): Expression {
    val generated = ref(schemeName(isDark)).call(Symbols.ToThemeValues)
    val tokens = ref(Symbols.MaterialKolorTokens)
    val pins = Role.entries.mapNotNull { role ->
        val pin = document.pins[role]
        val color = if (isDark) pin?.dark else pin?.light
        color?.let { argb -> infix(tokens.member(role.tokenName), "to", Literals.colorLiteral(argb.value)) }
    }
    val accents = document.accents.flatMap { accent -> accentEntries(accent, isDark) }
    val entries = pins + accents
    if (entries.isEmpty()) return generated

    return infix(generated, "+", call(Symbols.MapOf, multiline = true) { entries.forEach { entry -> argument(entry) } })
}

/**
 * The four tokens of an accent in one mode, cut from its ramp the way the Material 3 export's
 * `colorFamily` cuts them, so the two exports agree to the ARGB.
 */
private fun accentEntries(
    accent: Accent,
    isDark: Boolean,
): List<Expression> {
    val tones = if (isDark) accent.dark else accent.light
    val palette = ref(accent.paletteName)
    val values = listOf(
        palette.call(Symbols.ToneColor) { argument(Literals.int(tones.color)) },
        onToneCall(accent, tones.color),
        palette.call(Symbols.ToneColor) { argument(Literals.int(tones.container)) },
        onToneCall(accent, tones.container),
    )

    return accentTokenNames(accent.propertyName).zip(values).map { (name, value) ->
        infix(ref(THEME_TOKENS).member(name), "to", value)
    }
}

/**
 * `brandPalette.onTone(40)`, with the threshold when the accent asks for another one.
 */
private fun onToneCall(
    accent: Accent,
    tone: Int,
): Expression =
    ref(accent.paletteName).call(Symbols.OnTone) {
        argument(Literals.int(tone))
        if (!DefaultArguments.OnToneThreshold.isDefault(accent.threshold)) {
            argument(thresholdExpression(accent.threshold))
        }
    }

/**
 * `Tokens.kt`, the four tokens of every accent, or nothing when the theme has none.
 */
private fun tokensFile(input: ExportInput): GeneratedFile? {
    val accents = input.document.accents
    if (accents.isEmpty()) return null

    return kotlinFile(path = input.sourcePath("Tokens.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Dynamic))
        objectDeclaration(name = THEME_TOKENS) {
            accents.flatMap { accent -> accentTokenNames(accent.propertyName) }.forEach { name ->
                property(
                    name = name,
                    value = call(Symbols.ThemeToken) {
                        typeArgument(type(Symbols.Color))
                        argument(Literals.string(name))
                    },
                )
            }
        }
    }
}

private fun schemeName(isDark: Boolean): String = if (isDark) DARK_SCHEME else LIGHT_SCHEME
