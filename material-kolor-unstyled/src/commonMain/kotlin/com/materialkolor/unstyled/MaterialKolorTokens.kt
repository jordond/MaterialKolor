package com.materialkolor.unstyled

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.MaterialKolors

/**
 * The property and tokens an Unstyled theme uses to carry a MaterialKolor scheme.
 *
 * Every token is named after the [MaterialKolors] role it carries, so reading
 * `Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]` gives you the same color
 * [MaterialKolors.primary] returns for the scheme the theme was built from.
 *
 * Nothing here is required. An application that prefers its own vocabulary declares its own
 * property and tokens and maps the roles onto them.
 *
 * ```kotlin
 * properties[appColors] = remember(scheme) {
 *     with(MaterialKolors(scheme)) {
 *         mapOf(accent to primary(), onAccent to onPrimary())
 *     }
 * }
 * ```
 */
public object MaterialKolorTokens {
    /**
     * The property that holds the role colors, filled with [rememberDynamicColors] or [toThemeValues].
     */
    public val colors: ThemeProperty<Color> = ThemeProperty("materialkolor.colors")

    /**
     * The token for the [MaterialKolors.primaryPaletteKeyColor] role.
     */
    public val primaryPaletteKeyColor: ThemeToken<Color> = ThemeToken("primaryPaletteKeyColor")

    /**
     * The token for the [MaterialKolors.secondaryPaletteKeyColor] role.
     */
    public val secondaryPaletteKeyColor: ThemeToken<Color> = ThemeToken("secondaryPaletteKeyColor")

    /**
     * The token for the [MaterialKolors.tertiaryPaletteKeyColor] role.
     */
    public val tertiaryPaletteKeyColor: ThemeToken<Color> = ThemeToken("tertiaryPaletteKeyColor")

    /**
     * The token for the [MaterialKolors.errorPaletteKeyColor] role.
     */
    public val errorPaletteKeyColor: ThemeToken<Color> = ThemeToken("errorPaletteKeyColor")

    /**
     * The token for the [MaterialKolors.neutralPaletteKeyColor] role.
     */
    public val neutralPaletteKeyColor: ThemeToken<Color> = ThemeToken("neutralPaletteKeyColor")

    /**
     * The token for the [MaterialKolors.neutralVariantPaletteKeyColor] role.
     */
    public val neutralVariantPaletteKeyColor: ThemeToken<Color> = ThemeToken("neutralVariantPaletteKeyColor")

    /**
     * The token for the [MaterialKolors.background] role.
     */
    public val background: ThemeToken<Color> = ThemeToken("background")

    /**
     * The token for the [MaterialKolors.onBackground] role.
     */
    public val onBackground: ThemeToken<Color> = ThemeToken("onBackground")

    /**
     * The token for the [MaterialKolors.surface] role.
     */
    public val surface: ThemeToken<Color> = ThemeToken("surface")

    /**
     * The token for the [MaterialKolors.surfaceDim] role.
     */
    public val surfaceDim: ThemeToken<Color> = ThemeToken("surfaceDim")

    /**
     * The token for the [MaterialKolors.surfaceBright] role.
     */
    public val surfaceBright: ThemeToken<Color> = ThemeToken("surfaceBright")

    /**
     * The token for the [MaterialKolors.surfaceContainerLowest] role.
     */
    public val surfaceContainerLowest: ThemeToken<Color> = ThemeToken("surfaceContainerLowest")

    /**
     * The token for the [MaterialKolors.surfaceContainerLow] role.
     */
    public val surfaceContainerLow: ThemeToken<Color> = ThemeToken("surfaceContainerLow")

    /**
     * The token for the [MaterialKolors.surfaceContainer] role.
     */
    public val surfaceContainer: ThemeToken<Color> = ThemeToken("surfaceContainer")

    /**
     * The token for the [MaterialKolors.surfaceContainerHigh] role.
     */
    public val surfaceContainerHigh: ThemeToken<Color> = ThemeToken("surfaceContainerHigh")

    /**
     * The token for the [MaterialKolors.surfaceContainerHighest] role.
     */
    public val surfaceContainerHighest: ThemeToken<Color> = ThemeToken("surfaceContainerHighest")

    /**
     * The token for the [MaterialKolors.onSurface] role.
     */
    public val onSurface: ThemeToken<Color> = ThemeToken("onSurface")

    /**
     * The token for the [MaterialKolors.surfaceVariant] role.
     */
    public val surfaceVariant: ThemeToken<Color> = ThemeToken("surfaceVariant")

    /**
     * The token for the [MaterialKolors.onSurfaceVariant] role.
     */
    public val onSurfaceVariant: ThemeToken<Color> = ThemeToken("onSurfaceVariant")

    /**
     * The token for the [MaterialKolors.inverseSurface] role.
     */
    public val inverseSurface: ThemeToken<Color> = ThemeToken("inverseSurface")

    /**
     * The token for the [MaterialKolors.inverseOnSurface] role.
     */
    public val inverseOnSurface: ThemeToken<Color> = ThemeToken("inverseOnSurface")

    /**
     * The token for the [MaterialKolors.outline] role.
     */
    public val outline: ThemeToken<Color> = ThemeToken("outline")

    /**
     * The token for the [MaterialKolors.outlineVariant] role.
     */
    public val outlineVariant: ThemeToken<Color> = ThemeToken("outlineVariant")

    /**
     * The token for the [MaterialKolors.shadow] role.
     */
    public val shadow: ThemeToken<Color> = ThemeToken("shadow")

    /**
     * The token for the [MaterialKolors.scrim] role.
     */
    public val scrim: ThemeToken<Color> = ThemeToken("scrim")

    /**
     * The token for the [MaterialKolors.surfaceTint] role.
     */
    public val surfaceTint: ThemeToken<Color> = ThemeToken("surfaceTint")

    /**
     * The token for the [MaterialKolors.primary] role.
     */
    public val primary: ThemeToken<Color> = ThemeToken("primary")

    /**
     * The token for the [MaterialKolors.onPrimary] role.
     */
    public val onPrimary: ThemeToken<Color> = ThemeToken("onPrimary")

    /**
     * The token for the [MaterialKolors.primaryContainer] role.
     */
    public val primaryContainer: ThemeToken<Color> = ThemeToken("primaryContainer")

    /**
     * The token for the [MaterialKolors.onPrimaryContainer] role.
     */
    public val onPrimaryContainer: ThemeToken<Color> = ThemeToken("onPrimaryContainer")

    /**
     * The token for the [MaterialKolors.inversePrimary] role.
     */
    public val inversePrimary: ThemeToken<Color> = ThemeToken("inversePrimary")

    /**
     * The token for the [MaterialKolors.secondary] role.
     */
    public val secondary: ThemeToken<Color> = ThemeToken("secondary")

    /**
     * The token for the [MaterialKolors.onSecondary] role.
     */
    public val onSecondary: ThemeToken<Color> = ThemeToken("onSecondary")

    /**
     * The token for the [MaterialKolors.secondaryContainer] role.
     */
    public val secondaryContainer: ThemeToken<Color> = ThemeToken("secondaryContainer")

    /**
     * The token for the [MaterialKolors.onSecondaryContainer] role.
     */
    public val onSecondaryContainer: ThemeToken<Color> = ThemeToken("onSecondaryContainer")

    /**
     * The token for the [MaterialKolors.tertiary] role.
     */
    public val tertiary: ThemeToken<Color> = ThemeToken("tertiary")

    /**
     * The token for the [MaterialKolors.onTertiary] role.
     */
    public val onTertiary: ThemeToken<Color> = ThemeToken("onTertiary")

    /**
     * The token for the [MaterialKolors.tertiaryContainer] role.
     */
    public val tertiaryContainer: ThemeToken<Color> = ThemeToken("tertiaryContainer")

    /**
     * The token for the [MaterialKolors.onTertiaryContainer] role.
     */
    public val onTertiaryContainer: ThemeToken<Color> = ThemeToken("onTertiaryContainer")

    /**
     * The token for the [MaterialKolors.error] role.
     */
    public val error: ThemeToken<Color> = ThemeToken("error")

    /**
     * The token for the [MaterialKolors.onError] role.
     */
    public val onError: ThemeToken<Color> = ThemeToken("onError")

    /**
     * The token for the [MaterialKolors.errorContainer] role.
     */
    public val errorContainer: ThemeToken<Color> = ThemeToken("errorContainer")

    /**
     * The token for the [MaterialKolors.onErrorContainer] role.
     */
    public val onErrorContainer: ThemeToken<Color> = ThemeToken("onErrorContainer")

    /**
     * The token for the [MaterialKolors.primaryFixed] role.
     */
    public val primaryFixed: ThemeToken<Color> = ThemeToken("primaryFixed")

    /**
     * The token for the [MaterialKolors.primaryFixedDim] role.
     */
    public val primaryFixedDim: ThemeToken<Color> = ThemeToken("primaryFixedDim")

    /**
     * The token for the [MaterialKolors.onPrimaryFixed] role.
     */
    public val onPrimaryFixed: ThemeToken<Color> = ThemeToken("onPrimaryFixed")

    /**
     * The token for the [MaterialKolors.onPrimaryFixedVariant] role.
     */
    public val onPrimaryFixedVariant: ThemeToken<Color> = ThemeToken("onPrimaryFixedVariant")

    /**
     * The token for the [MaterialKolors.secondaryFixed] role.
     */
    public val secondaryFixed: ThemeToken<Color> = ThemeToken("secondaryFixed")

    /**
     * The token for the [MaterialKolors.secondaryFixedDim] role.
     */
    public val secondaryFixedDim: ThemeToken<Color> = ThemeToken("secondaryFixedDim")

    /**
     * The token for the [MaterialKolors.onSecondaryFixed] role.
     */
    public val onSecondaryFixed: ThemeToken<Color> = ThemeToken("onSecondaryFixed")

    /**
     * The token for the [MaterialKolors.onSecondaryFixedVariant] role.
     */
    public val onSecondaryFixedVariant: ThemeToken<Color> = ThemeToken("onSecondaryFixedVariant")

    /**
     * The token for the [MaterialKolors.tertiaryFixed] role.
     */
    public val tertiaryFixed: ThemeToken<Color> = ThemeToken("tertiaryFixed")

    /**
     * The token for the [MaterialKolors.tertiaryFixedDim] role.
     */
    public val tertiaryFixedDim: ThemeToken<Color> = ThemeToken("tertiaryFixedDim")

    /**
     * The token for the [MaterialKolors.onTertiaryFixed] role.
     */
    public val onTertiaryFixed: ThemeToken<Color> = ThemeToken("onTertiaryFixed")

    /**
     * The token for the [MaterialKolors.onTertiaryFixedVariant] role.
     */
    public val onTertiaryFixedVariant: ThemeToken<Color> = ThemeToken("onTertiaryFixedVariant")

    /**
     * The token for the [MaterialKolors.controlActivated] role.
     */
    public val controlActivated: ThemeToken<Color> = ThemeToken("controlActivated")

    /**
     * The token for the [MaterialKolors.controlNormal] role.
     */
    public val controlNormal: ThemeToken<Color> = ThemeToken("controlNormal")

    /**
     * The token for the [MaterialKolors.controlHighlight] role.
     */
    public val controlHighlight: ThemeToken<Color> = ThemeToken("controlHighlight")

    /**
     * The token for the [MaterialKolors.textPrimaryInverse] role.
     */
    public val textPrimaryInverse: ThemeToken<Color> = ThemeToken("textPrimaryInverse")

    /**
     * The token for the [MaterialKolors.textSecondaryAndTertiaryInverse] role.
     */
    public val textSecondaryAndTertiaryInverse: ThemeToken<Color> = ThemeToken("textSecondaryAndTertiaryInverse")

    /**
     * The token for the [MaterialKolors.textPrimaryInverseDisableOnly] role.
     */
    public val textPrimaryInverseDisableOnly: ThemeToken<Color> = ThemeToken("textPrimaryInverseDisableOnly")

    /**
     * The token for the [MaterialKolors.textSecondaryAndTertiaryInverseDisabled] role.
     */
    public val textSecondaryAndTertiaryInverseDisabled: ThemeToken<Color> =
        ThemeToken("textSecondaryAndTertiaryInverseDisabled")

    /**
     * The token for the [MaterialKolors.textHintInverse] role.
     */
    public val textHintInverse: ThemeToken<Color> = ThemeToken("textHintInverse")

    public val all: List<ThemeToken<Color>> =
        listOf(
            primaryPaletteKeyColor,
            secondaryPaletteKeyColor,
            tertiaryPaletteKeyColor,
            errorPaletteKeyColor,
            neutralPaletteKeyColor,
            neutralVariantPaletteKeyColor,
            background,
            onBackground,
            surface,
            surfaceDim,
            surfaceBright,
            surfaceContainerLowest,
            surfaceContainerLow,
            surfaceContainer,
            surfaceContainerHigh,
            surfaceContainerHighest,
            onSurface,
            surfaceVariant,
            onSurfaceVariant,
            inverseSurface,
            inverseOnSurface,
            outline,
            outlineVariant,
            shadow,
            scrim,
            surfaceTint,
            primary,
            onPrimary,
            primaryContainer,
            onPrimaryContainer,
            inversePrimary,
            secondary,
            onSecondary,
            secondaryContainer,
            onSecondaryContainer,
            tertiary,
            onTertiary,
            tertiaryContainer,
            onTertiaryContainer,
            error,
            onError,
            errorContainer,
            onErrorContainer,
            primaryFixed,
            primaryFixedDim,
            onPrimaryFixed,
            onPrimaryFixedVariant,
            secondaryFixed,
            secondaryFixedDim,
            onSecondaryFixed,
            onSecondaryFixedVariant,
            tertiaryFixed,
            tertiaryFixedDim,
            onTertiaryFixed,
            onTertiaryFixedVariant,
            controlActivated,
            controlNormal,
            controlHighlight,
            textPrimaryInverse,
            textSecondaryAndTertiaryInverse,
            textPrimaryInverseDisableOnly,
            textSecondaryAndTertiaryInverseDisabled,
            textHintInverse,
        )
}
