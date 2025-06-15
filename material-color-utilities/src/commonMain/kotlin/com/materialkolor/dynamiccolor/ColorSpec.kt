package com.materialkolor.dynamiccolor

import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.Variant

/**
 * An interface defining all the necessary methods that could be different between specs.
 */
public interface ColorSpec {
    /**
     * All available spec versions.
     */
    public enum class SpecVersion {
        SPEC_2021,
        SPEC_2025,
        ;

        public companion object {
            public val Default: SpecVersion = SPEC_2025
        }
    }

    // Main Palettes

    public fun primaryPaletteKeyColor(): DynamicColor

    public fun secondaryPaletteKeyColor(): DynamicColor

    public fun tertiaryPaletteKeyColor(): DynamicColor

    public fun neutralPaletteKeyColor(): DynamicColor

    public fun neutralVariantPaletteKeyColor(): DynamicColor

    public fun errorPaletteKeyColor(): DynamicColor

    // Surfaces [S]

    public fun background(): DynamicColor

    public fun onBackground(): DynamicColor

    public fun surface(): DynamicColor

    public fun surfaceDim(): DynamicColor

    public fun surfaceBright(): DynamicColor

    public fun surfaceContainerLowest(): DynamicColor

    public fun surfaceContainerLow(): DynamicColor

    public fun surfaceContainer(): DynamicColor

    public fun surfaceContainerHigh(): DynamicColor

    public fun surfaceContainerHighest(): DynamicColor

    public fun onSurface(): DynamicColor

    public fun surfaceVariant(): DynamicColor

    public fun onSurfaceVariant(): DynamicColor

    public fun inverseSurface(): DynamicColor

    public fun inverseOnSurface(): DynamicColor

    public fun outline(): DynamicColor

    public fun outlineVariant(): DynamicColor

    public fun shadow(): DynamicColor

    public fun scrim(): DynamicColor

    public fun surfaceTint(): DynamicColor

    // Primaries [P]

    public fun primary(): DynamicColor

    public fun primaryDim(): DynamicColor?

    public fun onPrimary(): DynamicColor

    public fun primaryContainer(): DynamicColor

    public fun onPrimaryContainer(): DynamicColor

    public fun inversePrimary(): DynamicColor

    // Secondaries [Q]

    public fun secondary(): DynamicColor

    public fun secondaryDim(): DynamicColor?

    public fun onSecondary(): DynamicColor

    public fun secondaryContainer(): DynamicColor

    public fun onSecondaryContainer(): DynamicColor

    // Tertiaries [T]

    public fun tertiary(): DynamicColor

    public fun tertiaryDim(): DynamicColor?

    public fun onTertiary(): DynamicColor

    public fun tertiaryContainer(): DynamicColor

    public fun onTertiaryContainer(): DynamicColor

    // Errors [E]

    public fun error(): DynamicColor

    public fun errorDim(): DynamicColor?

    public fun onError(): DynamicColor

    public fun errorContainer(): DynamicColor

    public fun onErrorContainer(): DynamicColor

    // Primary Fixed Colors [PF]

    public fun primaryFixed(): DynamicColor

    public fun primaryFixedDim(): DynamicColor

    public fun onPrimaryFixed(): DynamicColor

    public fun onPrimaryFixedVariant(): DynamicColor

    // Secondary Fixed Colors [QF]

    public fun secondaryFixed(): DynamicColor

    public fun secondaryFixedDim(): DynamicColor

    public fun onSecondaryFixed(): DynamicColor

    public fun onSecondaryFixedVariant(): DynamicColor

    // Tertiary Fixed Colors [TF]

    public fun tertiaryFixed(): DynamicColor

    public fun tertiaryFixedDim(): DynamicColor

    public fun onTertiaryFixed(): DynamicColor

    public fun onTertiaryFixedVariant(): DynamicColor

    // Android-only Colors

    public fun controlActivated(): DynamicColor

    public fun controlNormal(): DynamicColor

    public fun controlHighlight(): DynamicColor

    public fun textPrimaryInverse(): DynamicColor

    public fun textSecondaryAndTertiaryInverse(): DynamicColor

    public fun textPrimaryInverseDisableOnly(): DynamicColor

    public fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor

    public fun textHintInverse(): DynamicColor

    // Other

    public fun highestSurface(s: DynamicScheme): DynamicColor

    // Color value calculations

    public fun getHct(
        scheme: DynamicScheme,
        color: DynamicColor,
    ): Hct

    public fun getTone(
        scheme: DynamicScheme,
        color: DynamicColor,
    ): Double

    // Scheme Palettes

    public fun getPrimaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    public fun getSecondaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    public fun getTertiaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    public fun getNeutralPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    public fun getNeutralVariantPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    public fun getErrorPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette?
}
