package com.materialkolor.unstyled

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.MaterialKolors
import com.materialkolor.dynamiccolor.DynamicScheme

/**
 * Every [MaterialKolorTokens] token paired with the color this scheme gives it.
 *
 * Use this when the theme already has a scheme, for example one pinned in application state. For a
 * scheme generated from a seed inside the theme, [rememberDynamicColors] also remembers the result.
 *
 * ```kotlin
 * val AppTheme = buildThemeV2 {
 *     properties[MaterialKolorTokens.colors] = scheme.toThemeValues()
 * }
 * ```
 */
public fun DynamicScheme.toThemeValues(): Map<ThemeToken<Color>, Color> = MaterialKolors(this).toThemeValues()

/**
 * Every [MaterialKolorTokens] token paired with the color these roles give it.
 *
 * ```kotlin
 * val kolors = MaterialKolors(scheme, isAmoled = true)
 * properties[MaterialKolorTokens.colors] = kolors.toThemeValues()
 * ```
 */
public fun MaterialKolors.toThemeValues(): Map<ThemeToken<Color>, Color> =
    mapOf(
        MaterialKolorTokens.primaryPaletteKeyColor to primaryPaletteKeyColor(),
        MaterialKolorTokens.secondaryPaletteKeyColor to secondaryPaletteKeyColor(),
        MaterialKolorTokens.tertiaryPaletteKeyColor to tertiaryPaletteKeyColor(),
        MaterialKolorTokens.errorPaletteKeyColor to errorPaletteKeyColor(),
        MaterialKolorTokens.neutralPaletteKeyColor to neutralPaletteKeyColor(),
        MaterialKolorTokens.neutralVariantPaletteKeyColor to neutralVariantPaletteKeyColor(),
        MaterialKolorTokens.background to background(),
        MaterialKolorTokens.onBackground to onBackground(),
        MaterialKolorTokens.surface to surface(),
        MaterialKolorTokens.surfaceDim to surfaceDim(),
        MaterialKolorTokens.surfaceBright to surfaceBright(),
        MaterialKolorTokens.surfaceContainerLowest to surfaceContainerLowest(),
        MaterialKolorTokens.surfaceContainerLow to surfaceContainerLow(),
        MaterialKolorTokens.surfaceContainer to surfaceContainer(),
        MaterialKolorTokens.surfaceContainerHigh to surfaceContainerHigh(),
        MaterialKolorTokens.surfaceContainerHighest to surfaceContainerHighest(),
        MaterialKolorTokens.onSurface to onSurface(),
        MaterialKolorTokens.surfaceVariant to surfaceVariant(),
        MaterialKolorTokens.onSurfaceVariant to onSurfaceVariant(),
        MaterialKolorTokens.inverseSurface to inverseSurface(),
        MaterialKolorTokens.inverseOnSurface to inverseOnSurface(),
        MaterialKolorTokens.outline to outline(),
        MaterialKolorTokens.outlineVariant to outlineVariant(),
        MaterialKolorTokens.shadow to shadow(),
        MaterialKolorTokens.scrim to scrim(),
        MaterialKolorTokens.surfaceTint to surfaceTint(),
        MaterialKolorTokens.primary to primary(),
        MaterialKolorTokens.onPrimary to onPrimary(),
        MaterialKolorTokens.primaryContainer to primaryContainer(),
        MaterialKolorTokens.onPrimaryContainer to onPrimaryContainer(),
        MaterialKolorTokens.inversePrimary to inversePrimary(),
        MaterialKolorTokens.secondary to secondary(),
        MaterialKolorTokens.onSecondary to onSecondary(),
        MaterialKolorTokens.secondaryContainer to secondaryContainer(),
        MaterialKolorTokens.onSecondaryContainer to onSecondaryContainer(),
        MaterialKolorTokens.tertiary to tertiary(),
        MaterialKolorTokens.onTertiary to onTertiary(),
        MaterialKolorTokens.tertiaryContainer to tertiaryContainer(),
        MaterialKolorTokens.onTertiaryContainer to onTertiaryContainer(),
        MaterialKolorTokens.error to error(),
        MaterialKolorTokens.onError to onError(),
        MaterialKolorTokens.errorContainer to errorContainer(),
        MaterialKolorTokens.onErrorContainer to onErrorContainer(),
        MaterialKolorTokens.primaryFixed to primaryFixed(),
        MaterialKolorTokens.primaryFixedDim to primaryFixedDim(),
        MaterialKolorTokens.onPrimaryFixed to onPrimaryFixed(),
        MaterialKolorTokens.onPrimaryFixedVariant to onPrimaryFixedVariant(),
        MaterialKolorTokens.secondaryFixed to secondaryFixed(),
        MaterialKolorTokens.secondaryFixedDim to secondaryFixedDim(),
        MaterialKolorTokens.onSecondaryFixed to onSecondaryFixed(),
        MaterialKolorTokens.onSecondaryFixedVariant to onSecondaryFixedVariant(),
        MaterialKolorTokens.tertiaryFixed to tertiaryFixed(),
        MaterialKolorTokens.tertiaryFixedDim to tertiaryFixedDim(),
        MaterialKolorTokens.onTertiaryFixed to onTertiaryFixed(),
        MaterialKolorTokens.onTertiaryFixedVariant to onTertiaryFixedVariant(),
        MaterialKolorTokens.controlActivated to controlActivated(),
        MaterialKolorTokens.controlNormal to controlNormal(),
        MaterialKolorTokens.controlHighlight to controlHighlight(),
        MaterialKolorTokens.textPrimaryInverse to textPrimaryInverse(),
        MaterialKolorTokens.textSecondaryAndTertiaryInverse to textSecondaryAndTertiaryInverse(),
        MaterialKolorTokens.textPrimaryInverseDisableOnly to textPrimaryInverseDisableOnly(),
        MaterialKolorTokens.textSecondaryAndTertiaryInverseDisabled to textSecondaryAndTertiaryInverseDisabled(),
        MaterialKolorTokens.textHintInverse to textHintInverse(),
    )
