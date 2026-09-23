package com.materialkolor.builder.codegen.target.fluent

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.KotlinFileScope
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.CONTENT_PARAMETER
import com.materialkolor.builder.codegen.target.IS_DARK_PARAMETER
import com.materialkolor.builder.codegen.target.themeFunction
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Fluent export that writes its seven shades out as literals.
 *
 * It writes `Theme.kt` only, with the light and the dark shades and a theme function that hands
 * the ones for its mode to `FluentTheme`. Fluent builds each mode's shades from that mode's own
 * primary palette, so both sets are always written, even when they come out the same. Fluent has
 * no contrast variants, pins or accents, so the export writes the same two sets whatever the
 * document or the export options say about those.
 */
public object FluentFrozen {
    /** Every file the export of [input] writes. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Fluent) { "The Fluent frozen export cannot write a ${input.target} theme" }

        return listOf(themeFile(input))
    }
}

/** The light shades `Theme.kt` declares. */
internal const val LIGHT_THEME_SHADES: String = "LightThemeShades"

/** The dark shades `Theme.kt` declares. */
internal const val DARK_THEME_SHADES: String = "DarkThemeShades"

private const val COLORS = "colors"

private fun themeFile(input: ExportInput): GeneratedFile {
    val shades = requireNotNull(input.resolved.fluentShades) { "A Fluent export needs its shades resolved" }

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        shadesProperty(LIGHT_THEME_SHADES, shades.light)
        shadesProperty(DARK_THEME_SHADES, shades.dark)
        themeFunction(input) {
            // Colors holds state of its own, so it is remembered the way rememberFluentColors does.
            assign(
                name = COLORS,
                value = callOf(Symbols.Remember) {
                    argument(ref(IS_DARK_PARAMETER))
                    trailingLambda {
                        call(Symbols.FluentColors, multiline = true) {
                            argument(
                                name = "shades",
                                value = ifElse(
                                    condition = ref(IS_DARK_PARAMETER),
                                    whenTrue = ref(DARK_THEME_SHADES),
                                    whenFalse = ref(LIGHT_THEME_SHADES),
                                ),
                            )
                            argument("darkMode", ref(IS_DARK_PARAMETER))
                        }
                    }
                },
            )
            blankLine()
            call(Symbols.FluentTheme, multiline = true) {
                argument(COLORS, ref(COLORS))
                argument(CONTENT_PARAMETER, ref(CONTENT_PARAMETER))
            }
        }
    }
}

/** `val LightThemeShades = Shades(...)`, the seven shades of one mode in the order `Shades` declares them. */
private fun KotlinFileScope.shadesProperty(
    name: String,
    shades: FluentShadeValues,
) {
    val named = listOf(
        "base" to shades.base,
        "light1" to shades.light1,
        "light2" to shades.light2,
        "light3" to shades.light3,
        "dark1" to shades.dark1,
        "dark2" to shades.dark2,
        "dark3" to shades.dark3,
    )
    property(
        name = name,
        value = call(Symbols.FluentShades, multiline = true) {
            named.forEach { (shade, color) -> argument(shade, Literals.colorLiteral(color.value)) }
        },
    )
}
