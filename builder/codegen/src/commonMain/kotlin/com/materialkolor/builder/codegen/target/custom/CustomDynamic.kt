package com.materialkolor.builder.codegen.target.custom

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.AnnotationSpec
import com.materialkolor.builder.codegen.dsl.ClassKind
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.KotlinFileScope
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.type
import com.materialkolor.builder.codegen.symbol.DefaultArguments
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.symbol.optionalArgument
import com.materialkolor.builder.codegen.target.COLOR_FAMILY
import com.materialkolor.builder.codegen.target.CONTENT_PARAMETER
import com.materialkolor.builder.codegen.target.IS_DARK_PARAMETER
import com.materialkolor.builder.codegen.target.colorFamilyClass
import com.materialkolor.builder.codegen.target.material3.SEED_COLOR
import com.materialkolor.builder.codegen.target.material3.colorFamily
import com.materialkolor.builder.codegen.target.material3.dynamicColorFile
import com.materialkolor.builder.codegen.target.material3.familyOf
import com.materialkolor.builder.codegen.target.material3.harmonizes
import com.materialkolor.builder.codegen.target.material3.paletteName
import com.materialkolor.builder.codegen.target.material3.propertyName
import com.materialkolor.builder.codegen.target.material3.seedName
import com.materialkolor.builder.codegen.target.material3.toneExpression
import com.materialkolor.builder.codegen.target.propertyName
import com.materialkolor.builder.codegen.target.schemeArguments
import com.materialkolor.builder.codegen.target.themeFunction
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.model.TonalRamp
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Custom export that works every slot out from the seed at runtime, with nothing but core.
 *
 * It writes `ThemeSeeds.kt` with the seed and every color the document sets by hand, named as the
 * Material 3 dynamic export names them, `ThemeColors.kt` with the `ThemeColors` the frozen export
 * declares and `rememberThemeColors` to fill it, and `Theme.kt` with the theme function that provides
 * it. The shape follows the custom theme sample's `appColors`, ported rather than depended on.
 *
 * Each slot resolves the way the engine's `CustomSlotColors` resolves it for the preview. A slot that
 * names a role reads it off `MaterialKolors`, which applies AMOLED to dark mode only, and a pin then
 * replaces it in its own mode. A slot cut off a ramp takes the scheme's palette at its tone, or at
 * the document's custom tone for that mode. Accents follow the Material 3 dynamic export's chain.
 * Keep the two in step or the export stops matching the preview, which B-117's parity gate checks.
 */
public object CustomDynamic {
    /** Every file the export of [input] writes, in the order a reader would open them. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        require(input.target == ExportTarget.Custom) {
            "The Custom dynamic export cannot write a ${input.target} theme"
        }

        return listOf(themeSeedsFile(input), themeColorsFile(input), themeFile(input))
    }
}

/** The composable in `ThemeColors.kt` that works out every slot, which `Theme.kt` calls. */
internal const val REMEMBER_THEME_COLORS: String = "rememberThemeColors"

private const val SEED_COLOR_PARAMETER = "seedColor"
private const val SCHEME = "scheme"
private const val KOLORS = "kolors"
private const val COLORS = "colors"

/**
 * Where one slot's color comes from in one mode, once pins and custom tones are applied. Two modes
 * with the same source are written once, without an `if`.
 */
private sealed interface SlotSource {
    /** A pinned role, written as the pin's color. */
    data class Pinned(
        val color: Argb,
    ) : SlotSource

    /** A role the scheme works out, read off `MaterialKolors` so AMOLED reaches it. */
    data class SchemeRole(
        val role: Role,
    ) : SlotSource

    /** A tone cut off one of the scheme's ramps. */
    data class RampTone(
        val ramp: TonalRamp,
        val tone: Int,
    ) : SlotSource

    /** The color `onTone` finds on a ramp for a background tone, at the default threshold. */
    data class RampOnTone(
        val ramp: TonalRamp,
        val background: Int,
    ) : SlotSource
}

/**
 * `ThemeSeeds.kt`, the Material 3 dynamic export's `Color.kt` under the name the custom theme sample
 * gives its seeds file.
 */
private fun themeSeedsFile(input: ExportInput): GeneratedFile {
    val colors = dynamicColorFile(input)

    return GeneratedFile(path = input.sourcePath("ThemeSeeds.kt"), language = colors.language, lines = colors.lines)
}

private fun themeColorsFile(input: ExportInput): GeneratedFile {
    val accents = input.document.accents

    return kotlinFile(path = input.sourcePath("ThemeColors.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Dynamic))
        if (accents.isNotEmpty()) colorFamilyClass()
        classDeclaration(
            name = THEME_COLORS,
            kind = ClassKind.DataClass,
            annotations = listOf(AnnotationSpec(Symbols.Immutable)),
        ) {
            CustomSlot.entries.forEach { slot -> property(slot.propertyName, Symbols.Color) }
            accents.forEach { accent -> property(accent.propertyName, type(COLOR_FAMILY)) }
        }
        rememberThemeColors(input.document)
        if (accents.isNotEmpty()) colorFamily()
    }
}

/**
 * `rememberThemeColors`, which builds the scheme and every accent ramp and then works out each slot.
 *
 * The slots are remembered on the scheme and the ramps. The scheme is itself remembered on
 * `isDark`, so switching modes works everything out again.
 */
private fun KotlinFileScope.rememberThemeColors(document: ThemeDocument) {
    val readsRoles = CustomSlot.entries.any { slot ->
        listOf(false, true).any { isDark ->
            sourceOf(slot.resolution, document.customTones[slot], document.pins, isDark) is SlotSource.SchemeRole
        }
    }

    function(
        name = REMEMBER_THEME_COLORS,
        annotations = listOf(AnnotationSpec(Symbols.Composable)),
        returns = type(THEME_COLORS),
    ) {
        parameter(SEED_COLOR_PARAMETER, Symbols.Color)
        parameter(IS_DARK_PARAMETER, Symbols.Boolean)
        body {
            val scheme = callOf(Symbols.RememberDynamicScheme, multiline = true) {
                schemeArguments(
                    document = document,
                    defaults = DefaultArguments.RememberDynamicScheme,
                    seed = ref(SEED_COLOR_PARAMETER),
                    isDark = ref(IS_DARK_PARAMETER),
                )
            }
            assign(SCHEME, scheme)
            document.accents.forEach { accent -> assign(accent.paletteName, paletteCall(accent)) }
            blankLine()
            returns(
                callOf(Symbols.Remember) {
                    argument(ref(SCHEME))
                    document.accents.forEach { accent -> argument(ref(accent.paletteName)) }
                    trailingLambda {
                        if (readsRoles) {
                            assign(KOLORS, kolorsCall(document))
                            blankLine()
                        }
                        call(THEME_COLORS, multiline = true) {
                            CustomSlot.entries.forEach { slot ->
                                val value = slotValue(slot.resolution, document.customTones[slot], document.pins)
                                argument(slot.propertyName, value)
                            }
                            document.accents.forEach { accent -> argument(accent.propertyName, familyOf(accent)) }
                        }
                    }
                },
            )
        }
    }
}

/** `rememberTonalPalette(seed = BrandSeed, harmonizeWith = seedColor)`, the ramp the Material 3 export builds. */
private fun paletteCall(accent: Accent): Expression =
    call(Symbols.RememberTonalPalette, multiline = accent.harmonizes) {
        argument("seed", ref(accent.seedName))
        optionalArgument(DefaultArguments.RememberTonalPaletteHarmonizeWith, accent.harmonize) {
            ref(SEED_COLOR_PARAMETER)
        }
    }

/** `MaterialKolors(scheme)`, with `isAmoled = true` when the document asks for AMOLED. */
private fun kolorsCall(document: ThemeDocument): Expression =
    call(Symbols.MaterialKolors) {
        argument(ref(SCHEME))
        optionalArgument(DefaultArguments.MaterialKolorsIsAmoled, document.amoled) { amoled ->
            Literals.boolean(amoled)
        }
    }

/**
 * The value one slot is written with in `ThemeColors(...)`, for the slot's [resolution], the
 * document's custom [tone] for it and the document's [pins].
 *
 * A source both modes share is written once. Two tones on the same ramp become one call with an
 * `if` around the tone, as the accent families write theirs, and anything else becomes an `if`
 * around the two values.
 */
internal fun slotValue(
    resolution: SlotResolution,
    tone: CustomTone?,
    pins: Map<Role, RolePin>,
): Expression {
    val light = sourceOf(resolution, tone, pins, isDark = false)
    val dark = sourceOf(resolution, tone, pins, isDark = true)

    return when {
        light == dark -> {
            light.expression()
        }
        light is SlotSource.RampTone && dark is SlotSource.RampTone && light.ramp == dark.ramp -> {
            palette(light.ramp).call(Symbols.ToneColor) { argument(toneExpression(light.tone, dark.tone)) }
        }
        light is SlotSource.RampOnTone && dark is SlotSource.RampOnTone && light.ramp == dark.ramp -> {
            palette(light.ramp).call(Symbols.OnTone) { argument(toneExpression(light.background, dark.background)) }
        }
        else -> {
            ifElse(condition = ref(IS_DARK_PARAMETER), whenTrue = dark.expression(), whenFalse = light.expression())
        }
    }
}

/**
 * Where a slot's color comes from in the mode [isDark] picks, the same way the engine's
 * `CustomSlotColors.resolve` works it out.
 *
 * A custom tone moves a ramp slot, or sets an on color's own tone with no contrast search. It never
 * moves a role slot (D27), since a role is moved with a pin.
 */
private fun sourceOf(
    resolution: SlotResolution,
    tone: CustomTone?,
    pins: Map<Role, RolePin>,
    isDark: Boolean,
): SlotSource {
    val moved = tone?.let { custom -> if (isDark) custom.dark else custom.light }

    return when (resolution) {
        is SlotResolution.FromRole -> {
            val pin = pins[resolution.role]?.let { pinned -> if (isDark) pinned.dark else pinned.light }
            if (pin != null) SlotSource.Pinned(pin) else SlotSource.SchemeRole(resolution.role)
        }
        is SlotResolution.FromRamp -> {
            SlotSource.RampTone(resolution.ramp, moved ?: if (isDark) resolution.dark else resolution.light)
        }
        is SlotResolution.OnRamp -> {
            if (moved != null) {
                SlotSource.RampTone(resolution.ramp, moved)
            } else {
                SlotSource.RampOnTone(resolution.ramp, if (isDark) resolution.dark else resolution.light)
            }
        }
    }
}

private fun SlotSource.expression(): Expression =
    when (this) {
        is SlotSource.Pinned -> Literals.colorLiteral(color.value)
        is SlotSource.SchemeRole -> ref(KOLORS).call(role.propertyName)
        is SlotSource.RampTone -> palette(ramp).call(Symbols.ToneColor) { argument(Literals.int(tone)) }
        is SlotSource.RampOnTone -> palette(ramp).call(Symbols.OnTone) { argument(Literals.int(background)) }
    }

/** `scheme.primaryPalette` and the like. */
private fun palette(ramp: TonalRamp): Expression {
    val name = when (ramp) {
        TonalRamp.Primary -> "primaryPalette"
        TonalRamp.Secondary -> "secondaryPalette"
        TonalRamp.Tertiary -> "tertiaryPalette"
        TonalRamp.Error -> "errorPalette"
        TonalRamp.Neutral -> "neutralPalette"
        TonalRamp.NeutralVariant -> "neutralVariantPalette"
    }

    return ref(SCHEME).member(name)
}

private fun themeFile(input: ExportInput): GeneratedFile =
    kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Dynamic))
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
        themeFunction(input) {
            val colors = callOf(REMEMBER_THEME_COLORS, multiline = true) {
                argument(SEED_COLOR_PARAMETER, ref(SEED_COLOR))
                argument(IS_DARK_PARAMETER, ref(IS_DARK_PARAMETER))
            }
            assign(COLORS, colors)
            blankLine()
            call(Symbols.CompositionLocalProvider) {
                argument(infix(ref(LOCAL_THEME_COLORS), "provides", ref(COLORS)))
                argument(CONTENT_PARAMETER, ref(CONTENT_PARAMETER))
            }
        }
    }
