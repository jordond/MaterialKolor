package com.materialkolor.builder.codegen.target.fluent

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.CONTENT_PARAMETER
import com.materialkolor.builder.codegen.target.IS_DARK_PARAMETER
import com.materialkolor.builder.codegen.target.themeFunction
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Fluent export that writes its seven shades out as literals.
 *
 * It writes `Theme.kt` only, with the shades and a theme function that hands them to `FluentTheme`.
 * Fluent has no contrast variants, pins or accents, so the export writes the same one set whatever
 * the document or the export options say about those.
 */
public object FluentFrozen {
    /** Every file the export of [input] writes. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Fluent) { "The Fluent frozen export cannot write a ${input.target} theme" }

        return listOf(themeFile(input))
    }
}

/** The shades `Theme.kt` declares. */
internal const val THEME_SHADES: String = "ThemeShades"

private const val COLORS = "colors"

private fun themeFile(input: ExportInput): GeneratedFile {
    val shades = requireNotNull(input.resolved.fluentShades) { "A Fluent export needs its shades resolved" }
    val named = listOf(
        "base" to shades.base,
        "light1" to shades.light1,
        "light2" to shades.light2,
        "light3" to shades.light3,
        "dark1" to shades.dark1,
        "dark2" to shades.dark2,
        "dark3" to shades.dark3,
    )

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Frozen))
        property(
            name = THEME_SHADES,
            value = call(Symbols.FluentShades, multiline = true) {
                named.forEach { (name, color) -> argument(name, Literals.colorLiteral(color.value)) }
            },
        )
        themeFunction(input) {
            // Colors holds state of its own, so it is remembered the way rememberFluentColors does.
            assign(
                name = COLORS,
                value = callOf(Symbols.Remember) {
                    argument(ref(IS_DARK_PARAMETER))
                    trailingLambda {
                        call(Symbols.FluentColors, multiline = true) {
                            argument("shades", ref(THEME_SHADES))
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
