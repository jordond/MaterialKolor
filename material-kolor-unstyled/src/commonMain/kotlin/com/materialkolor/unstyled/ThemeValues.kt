package com.materialkolor.unstyled

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.MaterialKolors
import com.materialkolor.dynamiccolor.DynamicScheme

/**
 * Every [MaterialKolorTokens] token paired with the color this scheme gives it.
 *
 * The result is a plain map, so it can go straight into an Unstyled property or be reshaped first.
 */
public fun DynamicScheme.toThemeValues(): Map<ThemeToken<Color>, Color> = MaterialKolors(this).toThemeValues()

/**
 * Every [MaterialKolorTokens] token paired with the color these roles give it.
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

/**
 * Builds the values for an application-owned vocabulary from this scheme.
 *
 * Inside [build] the [MaterialKolors] roles are plain functions and `to` records a token, so a
 * design system maps its own tokens onto the scheme without learning the MaterialKolor ones.
 *
 * ```kotlin
 * val values = scheme.themeValues {
 *     AppTokens.accent to primary()
 *     AppTokens.onAccent to onPrimary()
 *     AppTokens.card to surfaceContainer()
 * }
 * ```
 *
 * Assigning the same token twice keeps the last color.
 */
public fun DynamicScheme.themeValues(build: ThemeValuesScope.() -> Unit): Map<ThemeToken<Color>, Color> =
    MaterialKolors(this).themeValues(build)

/**
 * Builds the values for an application-owned vocabulary from these roles.
 *
 * @see DynamicScheme.themeValues
 */
public fun MaterialKolors.themeValues(build: ThemeValuesScope.() -> Unit): Map<ThemeToken<Color>, Color> {
    val scope = ThemeValuesScope(this)
    scope.build()
    return scope.recorded()
}
