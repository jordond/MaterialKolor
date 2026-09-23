package com.materialkolor.builder.codegen.target.custom

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.AnnotationSpec
import com.materialkolor.builder.codegen.dsl.ClassKind
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.material3.COLOR_FAMILY
import com.materialkolor.builder.codegen.target.material3.CONTENT_PARAMETER
import com.materialkolor.builder.codegen.target.material3.FrozenMode
import com.materialkolor.builder.codegen.target.material3.byMode
import com.materialkolor.builder.codegen.target.material3.colorFamilyClass
import com.materialkolor.builder.codegen.target.material3.colorFamilyValue
import com.materialkolor.builder.codegen.target.material3.colorsIn
import com.materialkolor.builder.codegen.target.material3.contrastVariants
import com.materialkolor.builder.codegen.target.material3.frozenThemeFunction
import com.materialkolor.builder.codegen.target.material3.namePrefix
import com.materialkolor.builder.codegen.target.material3.propertyName
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.CustomSlotValues
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Custom export that writes every slot out as a literal.
 *
 * It writes `ThemeColors.kt` with a `ThemeColors` class holding every slot and one color family per
 * accent, and its values in both modes at each contrast variant, then `Theme.kt` with the theme
 * function that provides the standard pair. The shape follows the custom theme sample's `AppColors`
 * and `AppTheme`, without anything from MaterialKolor.
 */
public object CustomFrozen {
    /** Every file the export of [input] writes, in the order a reader would open them. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Custom) { "The Custom frozen export cannot write a ${input.target} theme" }
        require(input.resolved.customSlots.keys == input.resolved.roles.keys) {
            "A Custom export needs its slots resolved at every contrast variant it writes"
        }

        return listOf(themeColorsFile(input), themeFile(input))
    }
}

/** The data class `ThemeColors.kt` declares for every slot and accent. */
internal const val THEME_COLORS: String = "ThemeColors"

/** The composition local `Theme.kt` provides the colors through. */
internal const val LOCAL_THEME_COLORS: String = "LocalThemeColors"

private const val COLORS = "colors"

/** The property a slot is written as, as in `textStrong`, the same name the slot serializes under. */
internal val CustomSlot.propertyName: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }

private fun themeColorsFile(input: ExportInput): GeneratedFile {
    val resolved = input.resolved
    val accents = resolved.accents
    val variants = resolved.contrastVariants
    val standardPair = "${themeColorsName(ContrastVariant.Standard, FrozenMode.Light)} and " +
        themeColorsName(ContrastVariant.Standard, FrozenMode.Dark)

    return kotlinFile(path = input.sourcePath("ThemeColors.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input))
        if (accents.isNotEmpty()) colorFamilyClass()
        classDeclaration(
            name = THEME_COLORS,
            kind = ClassKind.DataClass,
            annotations = listOf(AnnotationSpec(Symbols.Immutable)),
        ) {
            CustomSlot.entries.forEach { slot -> property(slot.propertyName, Symbols.Color) }
            accents.forEach { accent -> property(accent.propertyName, type(COLOR_FAMILY)) }
        }
        variants.forEach { variant ->
            if (variant == variants.firstOrNull { other -> other != ContrastVariant.Standard }) {
                comment(
                    "The colors below are the other contrast variants, to swap in for $standardPair " +
                        "in ${input.document.themeName}.",
                )
            }
            val slots = resolved.customSlots.getValue(variant)
            FrozenMode.entries.forEach { mode ->
                property(themeColorsName(variant, mode), themeColorsValue(slots.colorsIn(mode), accents, mode))
            }
        }
    }
}

/** `ThemeColors(primary = ..., ...)` with every slot and then every accent's family, all literal. */
private fun themeColorsValue(
    slots: Map<CustomSlot, Argb>,
    accents: List<AccentFamilyValues>,
    mode: FrozenMode,
): Expression =
    call(THEME_COLORS, multiline = true) {
        CustomSlot.entries.forEach { slot ->
            argument(slot.propertyName, Literals.colorLiteral(slots.getValue(slot).value))
        }
        accents.forEach { accent -> argument(accent.propertyName, colorFamilyValue(accent.colorsIn(mode))) }
    }

private fun themeFile(input: ExportInput): GeneratedFile =
    kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input))
        property(
            name = LOCAL_THEME_COLORS,
            value = call(Symbols.StaticCompositionLocalOf) {
                typeArgument(type(THEME_COLORS))
                trailingLambda {
                    val message = "ThemeColors are only provided inside ${input.document.themeName}"
                    call("error") { argument(Literals.string(message)) }
                }
            },
        )
        frozenThemeFunction(input) {
            val colors = byMode(
                light = themeColorsName(ContrastVariant.Standard, FrozenMode.Light),
                dark = themeColorsName(ContrastVariant.Standard, FrozenMode.Dark),
            )
            assign(COLORS, colors)
            blankLine()
            call(Symbols.CompositionLocalProvider) {
                argument(infix(ref(LOCAL_THEME_COLORS), "provides", ref(COLORS)))
                argument(CONTENT_PARAMETER, ref(CONTENT_PARAMETER))
            }
        }
    }

private fun CustomSlotValues.colorsIn(mode: FrozenMode): Map<CustomSlot, Argb> =
    when (mode) {
        FrozenMode.Light -> light
        FrozenMode.Dark -> dark
    }

/** `lightThemeColors`, or `highContrastDarkThemeColors` at another contrast. */
private fun themeColorsName(
    variant: ContrastVariant,
    mode: FrozenMode,
): String {
    val prefix = variant.namePrefix

    return if (prefix.isEmpty()) "${mode.name.lowercase()}$THEME_COLORS" else "$prefix${mode.name}$THEME_COLORS"
}
