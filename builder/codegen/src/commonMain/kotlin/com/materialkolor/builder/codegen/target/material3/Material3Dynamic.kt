package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.AnnotationSpec
import com.materialkolor.builder.codegen.dsl.ArgumentsScope
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.classLiteral
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.lambda
import com.materialkolor.builder.codegen.dsl.lambdaType
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.symbol.DefaultArguments
import com.materialkolor.builder.codegen.symbol.SchemeDefaults
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.symbol.optionalArgument
import com.materialkolor.builder.codegen.text.Header
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * The Material 3 export that builds its scheme from the seed at runtime, plain or expressive.
 *
 * It writes `Color.kt` with the seed and every color the document sets by hand, `Theme.kt` with a
 * single call of `DynamicMaterialTheme` or `DynamicMaterialExpressiveTheme`, and `ExtendedColors.kt`
 * when the theme has accents. An argument only appears when it changes what the called function
 * would do on its own, so a default theme exports as little more than its seed.
 */
public object Material3Dynamic {
    /** Every file the export of [input] writes, in the order a reader would open them. */
    public fun files(input: ExportInput): List<GeneratedFile> {
        val target = input.target
        require(target == ExportTarget.Material3 || target == ExportTarget.Material3Expressive) {
            "The Material 3 dynamic export cannot write a $target theme"
        }

        return listOfNotNull(colorFile(input), themeFile(input), Material3Extended.file(input))
    }
}

/** The name of the seed color in `Color.kt`, which every other file reads. */
internal const val SEED_COLOR: String = "SeedColor"

/** The second seed a CMF theme gives its tertiary palette. */
private const val TERTIARY_SEED_COLOR = "TertiarySeedColor"

private const val IS_DARK = "isDark"
private const val CONTENT = "content"
private const val STATE = "state"
private const val SCHEME = "scheme"
private const val EXTENDED_COLORS = "extendedColors"

// b-112

/** The key colors in the order the theme functions take them, which is also the order `Color.kt` lists them. */
internal val KeyColorOrder: List<KeyColor> =
    listOf(
        KeyColor.Primary,
        KeyColor.Secondary,
        KeyColor.Tertiary,
        KeyColor.Neutral,
        KeyColor.NeutralVariant,
        KeyColor.Error,
    )

private fun colorFile(input: ExportInput): GeneratedFile {
    val document = input.document

    return kotlinFile(path = input.sourcePath("Color.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Dynamic))
        property(SEED_COLOR, Literals.colorLiteral(document.seed.value))
        KeyColorOrder.forEach { keyColor ->
            document.keyColors[keyColor]?.let { color -> property(keyColor.name, Literals.colorLiteral(color.value)) }
        }
        document.tertiarySeedForCmf()?.let { color ->
            property(TERTIARY_SEED_COLOR, Literals.colorLiteral(color.value))
        }
        document.accents.forEach { accent -> property(accent.seedName, Literals.colorLiteral(accent.seed.value)) }
    }
}

// b-112

/**
 * `Color.kt` of every dynamic export. Unstyled writes it as it is, and Fluent writes it less its
 * accent seeds and the key colors R1 hides.
 */
internal fun dynamicColorFile(input: ExportInput): GeneratedFile = colorFile(input)

private fun themeFile(input: ExportInput): GeneratedFile {
    val expressive = input.target == ExportTarget.Material3Expressive
    val annotations = buildList {
        // DynamicMaterialExpressiveTheme still carries the experimental marker, so its callers opt in.
        if (expressive) {
            add(AnnotationSpec(Symbols.OptIn, listOf(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi))))
        }
        add(AnnotationSpec(Symbols.Composable))
    }

    return kotlinFile(path = input.sourcePath("Theme.kt"), packageName = input.prefs.packageName) {
        header(Header.lines(input, ExportMode.Dynamic))
        function(name = input.document.themeName, annotations = annotations) {
            parameter(IS_DARK, Symbols.Boolean, default = call(Symbols.IsSystemInDarkTheme))
            parameter(CONTENT, lambdaType(annotations = listOf(Symbols.Composable)))
            body { themeBody(input, expressive) }
        }
    }
}

/**
 * The theme function's body.
 *
 * Pins need the scheme after it is generated, which only the state form hands over, so a pinned
 * theme remembers a state first and passes that on. Accents are provided around the theme so that
 * `LocalExtendedColors.current` works anywhere inside it.
 */
private fun BodyScope.themeBody(
    input: ExportInput,
    expressive: Boolean,
) {
    val document = input.document
    val hasAccents = document.accents.isNotEmpty()
    val hasPins = document.pins.isNotEmpty()

    if (hasAccents) {
        assign(
            name = EXTENDED_COLORS,
            value = callOf(REMEMBER_EXTENDED_COLORS, multiline = true) {
                argument("seedColor", ref(SEED_COLOR))
                argument(IS_DARK, ref(IS_DARK))
            },
        )
    }
    if (hasPins) {
        assign(
            name = STATE,
            value = callOf(Symbols.RememberDynamicMaterialThemeState, multiline = true) {
                schemeArguments(document, DefaultArguments.RememberDynamicMaterialThemeState)
                argument("modifyColorScheme", pinnedScheme(document))
            },
        )
    }
    if (hasAccents || hasPins) blankLine()

    val theme = themeCall(input, expressive, withState = hasPins)
    if (hasAccents) {
        call(Symbols.CompositionLocalProvider) {
            argument(infix(ref(LOCAL_EXTENDED_COLORS), "provides", ref(EXTENDED_COLORS)))
            trailingLambda { statement(theme) }
        }
    } else {
        statement(theme)
    }
}

private fun themeCall(
    input: ExportInput,
    expressive: Boolean,
    withState: Boolean,
): Expression {
    val function = if (expressive) Symbols.DynamicMaterialExpressiveTheme else Symbols.DynamicMaterialTheme
    val defaults = if (expressive) {
        DefaultArguments.DynamicMaterialExpressiveTheme
    } else {
        DefaultArguments.DynamicMaterialTheme
    }
    val document = input.document
    val prefs = input.prefs

    return call(function, multiline = true) {
        if (withState) {
            argument(STATE, ref(STATE))
            defaults.motionScheme?.let { default -> motionSchemeArgument(default.parameter, document.motionScheme) }
        } else {
            schemeArguments(document, defaults)
        }
        // The library's own spring is internal, so the duration someone picked is always written out.
        optionalArgument(checkNotNull(defaults.animate), prefs.animate) { animate -> Literals.boolean(animate) }
        if (prefs.animate) {
            val duration = Literals.int(prefs.animationDurationMs)
            argument("animationSpec", call(Symbols.Tween) { argument("durationMillis", duration) })
        }
        argument(CONTENT, ref(CONTENT))
    }
}

/**
 * The arguments that decide the scheme, in the order the called function declares them.
 *
 * The seed is always written, and each overridden palette goes in beside it rather than replacing
 * it, so the seed still drives every palette the document leaves alone.
 */
private fun ArgumentsScope.schemeArguments(
    document: ThemeDocument,
    defaults: SchemeDefaults,
) {
    argument("seedColor", ref(SEED_COLOR))
    defaults.motionScheme?.let { default -> motionSchemeArgument(default.parameter, document.motionScheme) }
    argument(IS_DARK, ref(IS_DARK))
    defaults.isAmoled?.let { default ->
        optionalArgument(default, document.amoled) { amoled -> Literals.boolean(amoled) }
    }
    KeyColorOrder.forEach { keyColor ->
        optionalArgument(keyColor.parameterName, document.keyColors[keyColor]?.let { ref(keyColor.name) })
    }
    optionalArgument(defaults.style, document.style) { style -> styleExpression(style, document) }
    optionalArgument(defaults.contrastLevel, document.contrast) { contrast -> Literals.decimal(contrast.hundredths) }
    optionalArgument(defaults.specVersion, document.spec) { spec -> specExpression(spec) }
    optionalArgument(defaults.platform, document.platform) { platform -> platformExpression(platform) }
}

/** The motion scheme has no default a document could match, so it is always written. */
private fun ArgumentsScope.motionSchemeArgument(
    parameter: String,
    choice: MotionSchemeChoice,
) {
    val motionScheme = ref(Symbols.MotionScheme)
    val value = when (choice) {
        MotionSchemeChoice.Standard -> motionScheme.call("standard")
        MotionSchemeChoice.Expressive -> motionScheme.call("expressive")
    }
    argument(parameter, value)
}

/**
 * `{ scheme -> scheme.copy(...) }` with every pinned role replaced, in the order the roles are
 * declared. A role pinned in one mode only keeps the generated color in the other.
 */
private fun pinnedScheme(document: ThemeDocument): Expression =
    lambda(SCHEME) {
        statement(
            ref(SCHEME).call("copy", multiline = true) {
                Role.entries.forEach { role ->
                    document.pins[role]?.let { pin -> argument(role.propertyName, pinExpression(role, pin)) }
                }
            },
        )
    }

private fun pinExpression(
    role: Role,
    pin: RolePin,
): Expression {
    val generated = ref(SCHEME).member(role.propertyName)

    return ifElse(
        condition = ref(IS_DARK),
        whenTrue = pin.dark?.let { color -> Literals.colorLiteral(color.value) } ?: generated,
        whenFalse = pin.light?.let { color -> Literals.colorLiteral(color.value) } ?: generated,
    )
}

/** `PaletteStyle.TonalSpot` and the like. A CMF style carries its tertiary seed when it has one. */
internal fun styleExpression(
    style: Style,
    document: ThemeDocument,
): Expression {
    val paletteStyle = ref(Symbols.PaletteStyle)

    return when (style) {
        Style.TonalSpot -> paletteStyle.member("TonalSpot")
        Style.Neutral -> paletteStyle.member("Neutral")
        Style.Vibrant -> paletteStyle.member("Vibrant")
        Style.Expressive -> paletteStyle.member("Expressive")
        Style.Rainbow -> paletteStyle.member("Rainbow")
        Style.FruitSalad -> paletteStyle.member("FruitSalad")
        Style.Monochrome -> paletteStyle.member("Monochrome")
        Style.Fidelity -> paletteStyle.member("Fidelity")
        Style.Content -> paletteStyle.member("Content")
        Style.Cmf -> paletteStyle.call("Cmf") {
            optionalArgument("tertiarySeedColor", document.tertiarySeedForCmf()?.let { ref(TERTIARY_SEED_COLOR) })
        }
    }
}

/** `ColorSpec.SpecVersion.SPEC_2025` and the like. */
internal fun specExpression(spec: SpecVersion): Expression {
    val specVersion = ref(Symbols.ColorSpec).member("SpecVersion")

    return when (spec) {
        SpecVersion.Spec2021 -> specVersion.member("SPEC_2021")
        SpecVersion.Spec2025 -> specVersion.member("SPEC_2025")
        SpecVersion.Spec2026 -> specVersion.member("SPEC_2026")
    }
}

/** `DynamicScheme.Platform.PHONE` or `WATCH`. */
internal fun platformExpression(platform: SchemePlatform): Expression {
    val schemePlatform = ref(Symbols.DynamicScheme).member("Platform")

    return when (platform) {
        SchemePlatform.Phone -> schemePlatform.member("PHONE")
        SchemePlatform.Watch -> schemePlatform.member("WATCH")
    }
}

/** The tertiary seed a CMF theme is built with, and nothing for any other style. */
private fun ThemeDocument.tertiarySeedForCmf(): Argb? = cmfTertiarySeed.takeIf { style == Style.Cmf }

// b-112

/** The theme function's parameter for this key color, as in `neutralVariant`. */
internal val KeyColor.parameterName: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }

/** The `ColorScheme` property for this role, as in `surfaceContainerHigh`. */
private val Role.propertyName: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() }
