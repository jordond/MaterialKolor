package com.materialkolor.builder.engine

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.MaterialKolors
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.AccentColors
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.model.TonalRamp
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.fluent.toFluentShades
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.from
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.material3.DynamicMaterialExpressiveTheme
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.unstyled.toThemeValues

/*
 * What each library adapter builds for a document, reached through its own public API. The
 * document types are mapped here by hand rather than through the engine's mapping, and every
 * composable `remember` call is replaced by the plain chain it wraps. Pins land on top the way the
 * exports write them, as a literal per mode.
 */

/** The document's contrast as the double the library takes. */
internal val ThemeDocument.contrastLevel: Double
    get() = contrast.hundredths / 100.0

/** The pin this document holds for [role] in the mode [isDark] picks, if any. */
internal fun ThemeDocument.pin(
    role: Role,
    isDark: Boolean,
): Argb? = pins[role]?.let { pin -> if (isDark) pin.dark else pin.light }

internal fun Argb.asColor(): Color = Color(value)

internal fun Color.asArgb(): Argb = Argb(toArgb())

internal fun referenceStyle(document: ThemeDocument): PaletteStyle =
    if (document.style == Style.Cmf) {
        PaletteStyle.Cmf(document.cmfTertiarySeed?.asColor())
    } else {
        PaletteStyle.fromName(document.style.name)
    }

internal fun referenceSpec(spec: SpecVersion): ColorSpec.SpecVersion =
    ColorSpec.SpecVersion.valueOf("SPEC_" + spec.name.removePrefix("Spec"))

internal fun referencePlatform(platform: SchemePlatform): DynamicScheme.Platform =
    DynamicScheme.Platform.valueOf(platform.name.uppercase())

/** The core scheme for [document], built by core's public factory with every key color. */
internal fun referenceScheme(
    document: ThemeDocument,
    isDark: Boolean,
    contrastLevel: Double = document.contrastLevel,
): DynamicScheme =
    DynamicScheme(
        seedColor = document.seed.asColor(),
        isDark = isDark,
        primary = document.keyColors.primary?.asColor(),
        secondary = document.keyColors.secondary?.asColor(),
        tertiary = document.keyColors.tertiary?.asColor(),
        neutral = document.keyColors.neutral?.asColor(),
        neutralVariant = document.keyColors.neutralVariant?.asColor(),
        error = document.keyColors.error?.asColor(),
        style = referenceStyle(document),
        contrastLevel = contrastLevel,
        specVersion = referenceSpec(document.spec),
        platform = referencePlatform(document.platform),
    )

/** Every role as core's [MaterialKolors] reads it off [scheme], with no AMOLED and no pins. */
internal fun coreRoles(scheme: DynamicScheme): Map<Role, Argb> {
    val kolors = MaterialKolors(scheme)
    return Role.entries.associateWith { role -> kolors.roleColor(role).asArgb() }
}

/** Every role the Material 3 module's `dynamicColorScheme` gives [document], pins on top. */
internal fun material3Roles(
    document: ThemeDocument,
    isDark: Boolean,
    contrastLevel: Double = document.contrastLevel,
): Map<Role, Argb> {
    val scheme = dynamicColorScheme(
        seedColor = document.seed.asColor(),
        isDark = isDark,
        isAmoled = document.amoled,
        primary = document.keyColors.primary?.asColor(),
        secondary = document.keyColors.secondary?.asColor(),
        tertiary = document.keyColors.tertiary?.asColor(),
        neutral = document.keyColors.neutral?.asColor(),
        neutralVariant = document.keyColors.neutralVariant?.asColor(),
        error = document.keyColors.error?.asColor(),
        style = referenceStyle(document),
        contrastLevel = contrastLevel,
        specVersion = referenceSpec(document.spec),
        platform = referencePlatform(document.platform),
    )
    return Role.entries.associateWith { role -> document.pin(role, isDark) ?: scheme.roleColor(role).asArgb() }
}

/**
 * Every role the Unstyled module's `toThemeValues` gives [document], pins on top.
 *
 * Unstyled has no AMOLED switch, so this reads the scheme as generated.
 */
internal fun unstyledRoles(
    document: ThemeDocument,
    isDark: Boolean,
): Map<Role, Argb> {
    val values = referenceScheme(document, isDark).toThemeValues()
    return Role.entries.associateWith { role ->
        document.pin(role, isDark) ?: values.getValue(role.unstyledToken()).asArgb()
    }
}

/** Every role [scheme] holds as the expressive theme put it in `MaterialTheme`, pins on top. */
internal fun expressiveRoles(
    document: ThemeDocument,
    scheme: ColorScheme,
    isDark: Boolean,
): Map<Role, Argb> =
    Role.entries.associateWith { role -> document.pin(role, isDark) ?: scheme.roleColor(role).asArgb() }

/**
 * Every Custom slot the Custom dynamic export's `rememberThemeColors` gives [document].
 *
 * The export builds a scheme, reads roles through `MaterialKolors` with the document's AMOLED
 * setting, writes a pin as a literal in its own mode and cuts a ramp slot with `toneColor`.
 */
internal fun customDynamicSlots(
    document: ThemeDocument,
    isDark: Boolean,
    contrastLevel: Double = document.contrastLevel,
): Map<CustomSlot, Argb> {
    val scheme = referenceScheme(document, isDark, contrastLevel)
    val kolors = MaterialKolors(scheme, isAmoled = document.amoled)
    return CustomSlot.entries.associateWith { slot ->
        val moved = document.customTones[slot]?.let { tone -> if (isDark) tone.dark else tone.light }
        when (val resolution = slot.resolution) {
            is SlotResolution.FromRole -> {
                document.pin(resolution.role, isDark) ?: kolors.roleColor(resolution.role).asArgb()
            }
            is SlotResolution.FromRamp -> {
                val tone = moved ?: if (isDark) resolution.dark else resolution.light
                scheme.ramp(resolution.ramp).toneColor(tone).asArgb()
            }
            is SlotResolution.OnRamp -> {
                val palette = scheme.ramp(resolution.ramp)
                val background = if (isDark) resolution.dark else resolution.light
                (moved?.let(palette::toneColor) ?: palette.onTone(background)).asArgb()
            }
        }
    }
}

/**
 * Every accent family of [document] the way the exported `ExtendedColors.kt` builds it.
 *
 * The ramp is the body of core's `rememberTonalPalette(seed, harmonizeWith)`, and each color is
 * `toneColor` or `onTone` at the accent's threshold.
 */
internal fun accentFamilies(document: ThemeDocument): List<AccentFamilyValues> =
    document.accents.map { accent ->
        val seed = accent.seed.asColor()
        val harmonizeWith = document.seed.asColor().takeIf { accent.harmonize }
        val palette = TonalPalette.from(harmonizeWith?.let { themeSeed -> seed.harmonize(themeSeed) } ?: seed)
        val threshold = accent.threshold.referenceThreshold()
        AccentFamilyValues(
            name = accent.name,
            light = palette.accentColors(accent.light, threshold),
            dark = palette.accentColors(accent.dark, threshold),
        )
    }

/**
 * The shades `toFluentColors` in the Fluent module gives [document] in the mode [isDark] picks,
 * which `toFluentShades` cuts off that mode's own primary palette.
 *
 * The Fluent export only passes a primary override, since Fluent reads nothing but that ramp.
 */
internal fun fluentShades(
    document: ThemeDocument,
    isDark: Boolean,
): FluentShadeValues {
    val scheme = DynamicScheme(
        seedColor = document.seed.asColor(),
        isDark = isDark,
        primary = document.keyColors.primary?.asColor(),
        style = referenceStyle(document),
        contrastLevel = document.contrastLevel,
        specVersion = referenceSpec(document.spec),
        platform = referencePlatform(document.platform),
    )
    val shades = scheme.primaryPalette.toFluentShades()
    return FluentShadeValues(
        dark3 = shades.dark3.asArgb(),
        dark2 = shades.dark2.asArgb(),
        dark1 = shades.dark1.asArgb(),
        base = shades.base.asArgb(),
        light1 = shades.light1.asArgb(),
        light2 = shades.light2.asArgb(),
        light3 = shades.light3.asArgb(),
    )
}

/**
 * The color schemes `DynamicMaterialExpressiveTheme` provides for [documents] in both modes.
 *
 * Every theme is composed in one pass and reads `MaterialTheme.colorScheme` from inside itself.
 * With [defaults] set, style, contrast, spec and platform are left out of the call, the way an
 * export leaves them out when the document matches the theme's own defaults.
 */
@OptIn(ExperimentalTestApi::class)
internal fun renderExpressive(
    documents: List<ThemeDocument>,
    defaults: Boolean,
): List<ModeSchemes> {
    val light = arrayOfNulls<ColorScheme>(documents.size)
    val dark = arrayOfNulls<ColorScheme>(documents.size)
    runComposeUiTest {
        setContent {
            documents.forEachIndexed { index, document ->
                key(index) {
                    ExpressiveTheme(document, isDark = false, defaults = defaults) {
                        light[index] = MaterialTheme.colorScheme
                    }
                    ExpressiveTheme(document, isDark = true, defaults = defaults) {
                        dark[index] = MaterialTheme.colorScheme
                    }
                }
            }
        }
        waitForIdle()
    }
    return documents.indices.map { index ->
        ModeSchemes(light = checkNotNull(light[index]), dark = checkNotNull(dark[index]))
    }
}

/** A color scheme per mode. */
internal class ModeSchemes(
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    fun mode(isDark: Boolean): ColorScheme = if (isDark) dark else light
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveTheme(
    document: ThemeDocument,
    isDark: Boolean,
    defaults: Boolean,
    content: @Composable () -> Unit,
) {
    if (defaults) {
        DynamicMaterialExpressiveTheme(
            seedColor = document.seed.asColor(),
            isDark = isDark,
            isAmoled = document.amoled,
            primary = document.keyColors.primary?.asColor(),
            secondary = document.keyColors.secondary?.asColor(),
            tertiary = document.keyColors.tertiary?.asColor(),
            neutral = document.keyColors.neutral?.asColor(),
            neutralVariant = document.keyColors.neutralVariant?.asColor(),
            error = document.keyColors.error?.asColor(),
            content = content,
        )
    } else {
        DynamicMaterialExpressiveTheme(
            seedColor = document.seed.asColor(),
            isDark = isDark,
            isAmoled = document.amoled,
            primary = document.keyColors.primary?.asColor(),
            secondary = document.keyColors.secondary?.asColor(),
            tertiary = document.keyColors.tertiary?.asColor(),
            neutral = document.keyColors.neutral?.asColor(),
            neutralVariant = document.keyColors.neutralVariant?.asColor(),
            error = document.keyColors.error?.asColor(),
            style = referenceStyle(document),
            contrastLevel = document.contrastLevel,
            specVersion = referenceSpec(document.spec),
            platform = referencePlatform(document.platform),
            content = content,
        )
    }
}

private fun TonalPalette.accentColors(
    tones: FamilyTones,
    threshold: ContrastThreshold,
): AccentColors =
    AccentColors(
        color = toneColor(tones.color).asArgb(),
        onColor = onTone(tones.color, threshold).asArgb(),
        container = toneColor(tones.container).asArgb(),
        onContainer = onTone(tones.container, threshold).asArgb(),
    )

/** The threshold the exported `ExtendedColors.kt` names for this document threshold. */
private fun OnColorThreshold.referenceThreshold(): ContrastThreshold =
    when (this) {
        OnColorThreshold.AaNormal -> ContrastThreshold.WCAG_AA_NORMAL_TEXT
        OnColorThreshold.AaLarge -> ContrastThreshold.WCAG_AA_LARGE_TEXT
        OnColorThreshold.Aaa -> ContrastThreshold.WCAG_AAA_NORMAL_TEXT
    }

private fun DynamicScheme.ramp(ramp: TonalRamp): TonalPalette =
    when (ramp) {
        TonalRamp.Primary -> primaryPalette
        TonalRamp.Secondary -> secondaryPalette
        TonalRamp.Tertiary -> tertiaryPalette
        TonalRamp.Error -> errorPalette
        TonalRamp.Neutral -> neutralPalette
        TonalRamp.NeutralVariant -> neutralVariantPalette
    }
