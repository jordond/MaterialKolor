package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.AnnotationSpec
import com.materialkolor.builder.codegen.dsl.ClassKind
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.KotlinFileScope
import com.materialkolor.builder.codegen.dsl.Visibility
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.DefaultArguments
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.symbol.optionalArgument
import com.materialkolor.builder.codegen.target.COLOR_FAMILY
import com.materialkolor.builder.codegen.target.FamilyParts
import com.materialkolor.builder.codegen.target.colorFamilyClass
import com.materialkolor.builder.codegen.target.propertyName
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.persist.ExportMode

/** The composable in `ExtendedColors.kt` that builds every accent family, which `Theme.kt` calls. */
internal const val REMEMBER_EXTENDED_COLORS: String = "rememberExtendedColors"

/** The composition local `Theme.kt` provides the accent families through. */
internal const val LOCAL_EXTENDED_COLORS: String = "LocalExtendedColors"

/** The data class `ExtendedColors.kt` declares to hold every accent family. */
internal const val EXTENDED_COLORS_TYPE: String = "ExtendedColors"

private const val TO_COLOR_FAMILY = "colorFamily"
private const val SEED_COLOR_PARAMETER = "seedColor"
private const val IS_DARK = "isDark"
private const val TONE = "tone"
private const val CONTAINER_TONE = "containerTone"
private const val THRESHOLD = "threshold"

/**
 * `ExtendedColors.kt`, the color families the theme's accents turn into.
 *
 * The families match the builder's own preview in the engine's `AccentFamily`, though not by making
 * the same calls. The engine calls `harmonize` and then `TonalPalette.from`, while the export calls
 * `rememberTonalPalette`, whose body in core is that same chain. Parity rests on that body, and
 * B-117's parity gate checks it. Each mode then cuts its color and container out of the ramp, with
 * `onTone` finding the content color on top of each. Keep the two in step or the export stops
 * matching the preview.
 *
 * The families snap from light to dark rather than animating, even when the theme animates.
 */
internal object Material3Extended {
    /** The file for the accents of [input], or nothing when the theme has none. */
    fun file(input: ExportInput): GeneratedFile? {
        val accents = input.document.accents
        if (accents.isEmpty()) return null

        return kotlinFile(path = input.sourcePath("ExtendedColors.kt"), packageName = input.prefs.packageName) {
            header(Header.lines(input, ExportMode.Dynamic))
            extendedColorsDeclarations(input, accents.map { accent -> accent.propertyName })
            rememberExtendedColors(accents)
            colorFamily()
        }
    }
}

/**
 * `ColorFamily`, `ExtendedColors` with a family per name in [accentNames], and `LocalExtendedColors`,
 * which open `ExtendedColors.kt` in both modes. The two differ only in how they fill the families.
 */
internal fun KotlinFileScope.extendedColorsDeclarations(
    input: ExportInput,
    accentNames: List<String>,
) {
    colorFamilyClass()
    classDeclaration(
        name = EXTENDED_COLORS_TYPE,
        kind = ClassKind.DataClass,
        annotations = listOf(AnnotationSpec(Symbols.Immutable)),
    ) {
        accentNames.forEach { name -> property(name, type(COLOR_FAMILY)) }
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
}

/** The value in `Color.kt` that holds an accent's seed, as in `BrandSeed`. */
internal val Accent.seedName: String
    get() = name.replaceFirstChar { char -> char.uppercaseChar() } + "Seed"

// b-112

/**
 * The local holding an accent's ramp. The suffix keeps an accent called `isDark` or `seedColor`
 * from shadowing the parameter of the same name.
 */
internal val Accent.paletteName: String
    get() = "${propertyName}Palette"

/** Whether the ramp call of the accent passes `harmonizeWith`, which gives it a second argument. */
internal val Accent.harmonizes: Boolean
    get() = !DefaultArguments.RememberTonalPaletteHarmonizeWith.isDefault(harmonize)

private fun KotlinFileScope.rememberExtendedColors(accents: List<Accent>) {
    function(
        name = REMEMBER_EXTENDED_COLORS,
        annotations = listOf(AnnotationSpec(Symbols.Composable)),
        returns = type(EXTENDED_COLORS_TYPE),
    ) {
        parameter(SEED_COLOR_PARAMETER, Symbols.Color)
        parameter(IS_DARK, Symbols.Boolean)
        body {
            accents.forEach { accent ->
                assign(
                    name = accent.paletteName,
                    value = callOf(Symbols.RememberTonalPalette, multiline = accent.harmonizes) {
                        argument("seed", ref(accent.seedName))
                        optionalArgument(DefaultArguments.RememberTonalPaletteHarmonizeWith, accent.harmonize) {
                            ref(SEED_COLOR_PARAMETER)
                        }
                    },
                )
            }
            blankLine()
            returns(
                callOf(Symbols.Remember) {
                    accents.forEach { accent -> argument(ref(accent.paletteName)) }
                    argument(ref(IS_DARK))
                    trailingLambda {
                        call(EXTENDED_COLORS_TYPE, multiline = true) {
                            accents.forEach { accent -> argument(accent.propertyName, familyOf(accent)) }
                        }
                    }
                },
            )
        }
    }
}

/** `brandPalette.colorFamily(tone = ..., containerTone = ...)`, at the accent's tones for each mode. */
private fun familyOf(accent: Accent): Expression =
    ref(accent.paletteName).call(TO_COLOR_FAMILY, multiline = true) {
        argument(TONE, toneExpression(light = accent.light.color, dark = accent.dark.color))
        argument(CONTAINER_TONE, toneExpression(light = accent.light.container, dark = accent.dark.container))
        optionalArgument(
            DefaultArguments.OnToneThreshold,
            accent.threshold,
        ) { threshold -> thresholdExpression(threshold) }
    }

/** `if (isDark) 80 else 40`, or just the tone when both modes use the same one. */
private fun toneExpression(
    light: Int,
    dark: Int,
): Expression =
    if (light == dark) {
        Literals.int(light)
    } else {
        ifElse(condition = ref(IS_DARK), whenTrue = Literals.int(dark), whenFalse = Literals.int(light))
    }

/**
 * The private helper that cuts one family out of a ramp.
 *
 * Its threshold defaults to the one `onTone` defaults to, so an accent on the usual threshold does
 * not have to say so.
 */
private fun KotlinFileScope.colorFamily() {
    function(
        name = TO_COLOR_FAMILY,
        returns = type(COLOR_FAMILY),
        visibility = Visibility.Private,
        receiver = type(Symbols.TonalPalette),
    ) {
        parameter(TONE, Symbols.Int)
        parameter(CONTAINER_TONE, Symbols.Int)
        parameter(
            name = THRESHOLD,
            symbol = Symbols.ContrastThreshold,
            default = thresholdExpression(DefaultArguments.OnToneThreshold.value),
        )
        body {
            // ktlint wants a body that is only a return written as an expression, which the DSL cannot
            // write yet, so each color gets a name first, in the order the engine works them out.
            assign(FamilyParts[0], toneColorCall(TONE))
            assign(FamilyParts[1], onToneCall(TONE))
            assign(FamilyParts[2], toneColorCall(CONTAINER_TONE))
            assign(FamilyParts[3], onToneCall(CONTAINER_TONE))
            blankLine()
            returns(callOf(COLOR_FAMILY) { FamilyParts.forEach { part -> argument(ref(part)) } })
        }
    }
}

private fun toneColorCall(tone: String): Expression = call(Symbols.ToneColor) { argument(ref(tone)) }

private fun onToneCall(tone: String): Expression =
    call(Symbols.OnTone) {
        argument(ref(tone))
        argument(ref(THRESHOLD))
    }

// b-112

/**
 * The core threshold for a document threshold, the same mapping the engine uses.
 *
 * AAA maps to the normal text rule, since an on color carries body text.
 */
internal fun thresholdExpression(threshold: OnColorThreshold): Expression {
    val contrastThreshold = ref(Symbols.ContrastThreshold)

    return when (threshold) {
        OnColorThreshold.AaNormal -> contrastThreshold.member("WCAG_AA_NORMAL_TEXT")
        OnColorThreshold.AaLarge -> contrastThreshold.member("WCAG_AA_LARGE_TEXT")
        OnColorThreshold.Aaa -> contrastThreshold.member("WCAG_AAA_NORMAL_TEXT")
    }
}
