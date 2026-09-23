package com.materialkolor.builder.codegen.target.fluent

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.FluentBinding
import com.materialkolor.builder.codegen.dsl.ArgumentsScope
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.KotlinFileScope
import com.materialkolor.builder.codegen.dsl.Visibility
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.lambda
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.DefaultArguments
import com.materialkolor.builder.codegen.symbol.SchemeDefaults
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.symbol.optionalArgument
import com.materialkolor.builder.codegen.target.CONTENT_PARAMETER
import com.materialkolor.builder.codegen.target.IS_DARK_PARAMETER
import com.materialkolor.builder.codegen.target.frozenThemeFunction
import com.materialkolor.builder.codegen.target.material3.KeyColorOrder
import com.materialkolor.builder.codegen.target.material3.SEED_COLOR
import com.materialkolor.builder.codegen.target.material3.dynamicColorFile
import com.materialkolor.builder.codegen.target.material3.parameterName
import com.materialkolor.builder.codegen.target.material3.platformExpression
import com.materialkolor.builder.codegen.target.material3.specExpression
import com.materialkolor.builder.codegen.target.material3.styleExpression
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Fluent export that builds its shades from the seed at runtime.
 *
 * It writes the same `Color.kt` as the Material 3 dynamic export, less the accent seeds, and
 * `Theme.kt` with a theme function that hands the colors to `FluentTheme`. With
 * `material-kolor-fluent` published it calls `rememberFluentColors`, and `animateFluentColors` when
 * the theme animates. Without it the export maps the primary palette onto the seven shades itself,
 * the same way the module does, and does not animate.
 *
 * Contrast is never written, since the shades come from the primary palette and contrast does not
 * move it. Fluent has no pins, accents or AMOLED, so none of those are written either.
 */
public object FluentDynamic {
    /** Every file the export of [input] writes, in the order a reader would open them. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Fluent) {
            "The Fluent dynamic export cannot write a ${input.target} theme"
        }
        val withoutAccents = input.copy(document = input.document.copy(accents = emptyList()))

        return listOf(dynamicColorFile(withoutAccents), themeFile(input))
    }
}

/** The line the inline form adds to its header. */
internal const val SWAP_TO_MODULE_NOTE: String = "Swap to material-kolor-fluent when it is available."

private const val COLORS = "colors"
private const val TARGET_COLORS = "targetColors"
private const val SCHEME = "scheme"
private const val TO_SHADES = "toShades"

/** The seven shades in the order `Shades` declares them, each with the tone the module reads it from. */
private val ShadeTones: List<Pair<String, Int>> =
    listOf(
        "base" to 50,
        "light1" to 60,
        "light2" to 80,
        "light3" to 90,
        "dark1" to 40,
        "dark2" to 30,
        "dark3" to 15,
    )

private fun themeFile(input: ExportInput): GeneratedFile {
    val binding = input.versions.fluentBinding
    val header = when (binding) {
        FluentBinding.Module -> Header.lines(input, ExportMode.Dynamic)
        FluentBinding.Inline -> Header.lines(input, ExportMode.Dynamic) + SWAP_TO_MODULE_NOTE
    }

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(header)
        frozenThemeFunction(input) {
            when (binding) {
                FluentBinding.Module -> moduleColors(input)
                FluentBinding.Inline -> inlineColors(input.document)
            }
            blankLine()
            call(Symbols.FluentTheme, multiline = true) {
                argument(COLORS, ref(COLORS))
                argument(CONTENT_PARAMETER, ref(CONTENT_PARAMETER))
            }
        }
        if (binding == FluentBinding.Inline) toShades()
    }
}

/** `rememberFluentColors(...)`, passed through `animateFluentColors` when the theme animates. */
private fun BodyScope.moduleColors(input: ExportInput) {
    val colors = callOf(Symbols.RememberFluentColors, multiline = true) {
        schemeArguments(input.document, DefaultArguments.RememberFluentColors)
    }
    if (!input.prefs.animate) {
        assign(COLORS, colors)
        return
    }

    // The module's own spring is internal, so the duration someone picked is always written out.
    val duration = Literals.int(input.prefs.animationDurationMs)
    assign(TARGET_COLORS, colors)
    assign(
        name = COLORS,
        value = callOf(Symbols.AnimateFluentColors) {
            argument(ref(TARGET_COLORS))
            argument("animationSpec", lambda { call(Symbols.Tween) { argument("durationMillis", duration) } })
        },
    )
}

/** The scheme from core and `Colors` built from its primary palette, remembered as the module does. */
private fun BodyScope.inlineColors(document: ThemeDocument) {
    assign(
        name = SCHEME,
        value = callOf(Symbols.RememberDynamicScheme, multiline = true) {
            schemeArguments(document, DefaultArguments.RememberDynamicScheme)
        },
    )
    assign(
        name = COLORS,
        value = callOf(Symbols.Remember) {
            argument(ref(SCHEME))
            trailingLambda {
                call(Symbols.FluentColors, multiline = true) {
                    argument("shades", ref(SCHEME).member("primaryPalette").call(TO_SHADES))
                    argument("darkMode", ref(IS_DARK_PARAMETER))
                }
            }
        },
    )
}

/**
 * The arguments that decide the scheme, in the order both functions declare them. The seed is
 * always written and each overridden palette goes in beside it, as in the Material 3 dynamic export.
 */
private fun ArgumentsScope.schemeArguments(
    document: ThemeDocument,
    defaults: SchemeDefaults,
) {
    argument("seedColor", ref(SEED_COLOR))
    argument(IS_DARK_PARAMETER, ref(IS_DARK_PARAMETER))
    KeyColorOrder.forEach { keyColor ->
        optionalArgument(keyColor.parameterName, document.keyColors[keyColor]?.let { ref(keyColor.name) })
    }
    optionalArgument(defaults.style, document.style) { style -> styleExpression(style, document) }
    optionalArgument(defaults.specVersion, document.spec) { spec -> specExpression(spec) }
    optionalArgument(defaults.platform, document.platform) { platform -> platformExpression(platform) }
}

/** The private `TonalPalette.toShades()` of the inline form, the module's `toFluentShades` written out. */
private fun KotlinFileScope.toShades() {
    function(
        name = TO_SHADES,
        returns = type(Symbols.FluentShades),
        visibility = Visibility.Private,
        receiver = type(Symbols.TonalPalette),
    ) {
        body {
            // ktlint wants a body that is only a return written as an expression, which the DSL cannot
            // write yet, so each shade gets a name first.
            ShadeTones.forEach { (name, tone) ->
                assign(name, callOf(Symbols.ToneColor) { argument(Literals.int(tone)) })
            }
            blankLine()
            returns(
                callOf(Symbols.FluentShades, multiline = true) {
                    ShadeTones.forEach { (name, _) -> argument(name, ref(name)) }
                },
            )
        }
    }
}
