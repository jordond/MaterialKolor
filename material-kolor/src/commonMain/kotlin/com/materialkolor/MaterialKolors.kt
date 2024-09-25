package com.materialkolor

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.ktx.getColor
import com.materialkolor.scheme.DynamicScheme

/**
 * A class that provides various color functions based on the given dynamic scheme.
 *
 * @property scheme The dynamic scheme used to generate colors.
 * @property isAmoled A flag indicating whether the device is AMOLED.
 * @param isExtendedFidelity A flag indicating whether extended fidelity is enabled.
 */
public class MaterialKolors(
    private val scheme: DynamicScheme,
    private val isAmoled: Boolean = false,
    isExtendedFidelity: Boolean = false,
) {

    private val colors = MaterialDynamicColors(isExtendedFidelity)

    /**
     * Returns the highest surface color based on the given scheme.
     *
     * @param scheme The dynamic scheme used to generate the color.
     * @see MaterialDynamicColors.highestSurface
     */
    public fun highestSurface(scheme: DynamicScheme): Color =
        colors.highestSurface(scheme).getColor(scheme)

    /**
     * Returns the primary palette key color.
     *
     * @see MaterialDynamicColors.primaryPaletteKeyColor
     */
    public fun primaryPaletteKeyColor(): Color = colors.primaryPaletteKeyColor().getColor(scheme)

    /**
     * Returns the secondary palette key color.
     *
     * @see MaterialDynamicColors.secondaryPaletteKeyColor
     */
    public fun secondaryPaletteKeyColor(): Color =
        colors.secondaryPaletteKeyColor().getColor(scheme)

    /**
     * Returns the tertiary palette key color.
     *
     * @see MaterialDynamicColors.tertiaryPaletteKeyColor
     */
    public fun tertiaryPaletteKeyColor(): Color = colors.tertiaryPaletteKeyColor().getColor(scheme)

    /**
     * Returns the error palette key color.
     *
     * @see MaterialDynamicColors.errorPaletteKeyColor
     */
    public fun errorPaletteKeyColor(): Color = colors.errorPaletteKeyColor().getColor(scheme)

    /**
     * Returns the neutral palette key color.
     *
     * @see MaterialDynamicColors.neutralPaletteKeyColor
     */
    public fun neutralPaletteKeyColor(): Color = colors.neutralPaletteKeyColor().getColor(scheme)

    /**
     * Returns the neutral variant palette key color.
     *
     * @see MaterialDynamicColors.neutralVariantPaletteKeyColor
     */
    public fun neutralVariantPaletteKeyColor(): Color =
        colors.neutralVariantPaletteKeyColor().getColor(scheme)

    /**
     * Returns the background color.
     *
     * @see MaterialDynamicColors.background
     */
    public fun background(): Color =
        if (isAmoled && scheme.isDark) Color.Black
        else colors.background().getColor(scheme)

    /**
     * Returns the on-background color.
     *
     * @see MaterialDynamicColors.onBackground
     */
    public fun onBackground(): Color =
        if (isAmoled && scheme.isDark) Color.White
        else colors.onBackground().getColor(scheme)

    /**
     * Returns the surface color.
     *
     * @see MaterialDynamicColors.surface
     */
    public fun surface(): Color =
        if (isAmoled && scheme.isDark) Color.Black
        else colors.surface().getColor(scheme)

    /**
     * Returns the surface dim color.
     *
     * @see MaterialDynamicColors.surfaceDim
     */
    public fun surfaceDim(): Color = colors.surfaceDim().getColor(scheme)

    /**
     * Returns the surface bright color.
     *
     * @see MaterialDynamicColors.surfaceBright
     */
    public fun surfaceBright(): Color = colors.surfaceBright().getColor(scheme)

    /**
     * Returns the surface container lowest color.
     *
     * @see MaterialDynamicColors.surfaceContainerLowest
     */
    public fun surfaceContainerLowest(): Color = colors.surfaceContainerLowest().getColor(scheme)

    /**
     * Returns the surface container low color.
     *
     * @see MaterialDynamicColors.surfaceContainerLow
     */
    public fun surfaceContainerLow(): Color = colors.surfaceContainerLow().getColor(scheme)

    /**
     * Returns the surface container color.
     *
     * @see MaterialDynamicColors.surfaceContainer
     */
    public fun surfaceContainer(): Color = colors.surfaceContainer().getColor(scheme)

    /**
     * Returns the surface container high color.
     *
     * @see MaterialDynamicColors.surfaceContainerHigh
     */
    public fun surfaceContainerHigh(): Color = colors.surfaceContainerHigh().getColor(scheme)

    /**
     * Returns the surface container highest color.
     *
     * @see MaterialDynamicColors.surfaceContainerHighest
     */
    public fun surfaceContainerHighest(): Color = colors.surfaceContainerHighest().getColor(scheme)

    /**
     * Returns the on-surface color.
     *
     * @see MaterialDynamicColors.onSurface
     */
    public fun onSurface(): Color =
        if (isAmoled && scheme.isDark) Color.White
        else colors.onSurface().getColor(scheme)

    /**
     * Returns the surface variant color.
     *
     * @see MaterialDynamicColors.surfaceVariant
     */
    public fun surfaceVariant(): Color = colors.surfaceVariant().getColor(scheme)

    /**
     * Returns the on-surface variant color.
     *
     * @see MaterialDynamicColors.onSurfaceVariant
     */
    public fun onSurfaceVariant(): Color = colors.onSurfaceVariant().getColor(scheme)

    /**
     * Returns the inverse surface color.
     *
     * @see MaterialDynamicColors.inverseSurface
     */
    public fun inverseSurface(): Color = colors.inverseSurface().getColor(scheme)

    /**
     * Returns the inverse on-surface color.
     *
     * @see MaterialDynamicColors.inverseOnSurface
     */
    public fun inverseOnSurface(): Color = colors.inverseOnSurface().getColor(scheme)

    /**
     * Returns the outline color.
     *
     * @see MaterialDynamicColors.outline
     */
    public fun outline(): Color = colors.outline().getColor(scheme)

    /**
     * Returns the outline variant color.
     *
     * @see MaterialDynamicColors.outlineVariant
     */
    public fun outlineVariant(): Color = colors.outlineVariant().getColor(scheme)

    /**
     * Returns the shadow color.
     *
     * @see MaterialDynamicColors.shadow
     */
    public fun shadow(): Color = colors.shadow().getColor(scheme)

    /**
     * Returns the scrim color.
     *
     * @see MaterialDynamicColors.scrim
     */
    public fun scrim(): Color = colors.scrim().getColor(scheme)

    /**
     * Returns the surface tint color.
     *
     * @see MaterialDynamicColors.surfaceTint
     */
    public fun surfaceTint(): Color = colors.surfaceTint().getColor(scheme)

    /**
     * Returns the primary color.
     *
     * @see MaterialDynamicColors.primary
     */
    public fun primary(): Color = colors.primary().getColor(scheme)

    /**
     * Returns the on-primary color.
     *
     * @see MaterialDynamicColors.onPrimary
     */
    public fun onPrimary(): Color = colors.onPrimary().getColor(scheme)

    /**
     * Returns the primary container color.
     *
     * @see MaterialDynamicColors.primaryContainer
     */
    public fun primaryContainer(): Color = colors.primaryContainer().getColor(scheme)

    /**
     * Returns the on-primary container color.
     *
     * @see MaterialDynamicColors.onPrimaryContainer
     */
    public fun onPrimaryContainer(): Color = colors.onPrimaryContainer().getColor(scheme)

    /**
     * Returns the inverse primary color.
     *
     * @see MaterialDynamicColors.inversePrimary
     */
    public fun inversePrimary(): Color = colors.inversePrimary().getColor(scheme)

    /**
     * Returns the secondary color.
     *
     * @see MaterialDynamicColors.secondary
     */
    public fun secondary(): Color = colors.secondary().getColor(scheme)

    /**
     * Returns the on-secondary color.
     *
     * @see MaterialDynamicColors.onSecondary
     */
    public fun onSecondary(): Color = colors.onSecondary().getColor(scheme)

    /**
     * Returns the secondary container color.
     *
     * @see MaterialDynamicColors.secondaryContainer
     */
    public fun secondaryContainer(): Color = colors.secondaryContainer().getColor(scheme)

    /**
     * Returns the on-secondary container color.
     *
     * @see MaterialDynamicColors.onSecondaryContainer
     */
    public fun onSecondaryContainer(): Color = colors.onSecondaryContainer().getColor(scheme)

    /**
     * Returns the tertiary color.
     *
     * @see MaterialDynamicColors.tertiary
     */
    public fun tertiary(): Color = colors.tertiary().getColor(scheme)

    /**
     * Returns the on-tertiary color.
     *
     * @see MaterialDynamicColors.onTertiary
     */
    public fun onTertiary(): Color = colors.onTertiary().getColor(scheme)

    /**
     * Returns the tertiary container color.
     *
     * @see MaterialDynamicColors.tertiaryContainer
     */
    public fun tertiaryContainer(): Color = colors.tertiaryContainer().getColor(scheme)

    /**
     * Returns the on-tertiary container color.
     *
     * @see MaterialDynamicColors.onTertiaryContainer
     */
    public fun onTertiaryContainer(): Color = colors.onTertiaryContainer().getColor(scheme)

    /**
     * Returns the error color.
     *
     * @see MaterialDynamicColors.error
     */
    public fun error(): Color = colors.error().getColor(scheme)

    /**
     * Returns the on-error color.
     *
     * @see MaterialDynamicColors.onError
     */
    public fun onError(): Color = colors.onError().getColor(scheme)

    /**
     * Returns the error container color.
     *
     * @see MaterialDynamicColors.errorContainer
     */
    public fun errorContainer(): Color = colors.errorContainer().getColor(scheme)

    /**
     * Returns the on-error container color.
     *
     * @see MaterialDynamicColors.onErrorContainer
     */
    public fun onErrorContainer(): Color = colors.onErrorContainer().getColor(scheme)

    /**
     * Returns the primary fixed color.
     *
     * @see MaterialDynamicColors.primaryFixed
     */
    public fun primaryFixed(): Color = colors.primaryFixed().getColor(scheme)

    /**
     * Returns the primary fixed dim color.
     *
     * @see MaterialDynamicColors.primaryFixedDim
     */
    public fun primaryFixedDim(): Color = colors.primaryFixedDim().getColor(scheme)

    /**
     * Returns the on-primary fixed color.
     *
     * @see MaterialDynamicColors.onPrimaryFixed
     */
    public fun onPrimaryFixed(): Color = colors.onPrimaryFixed().getColor(scheme)

    /**
     * Returns the on-primary fixed variant color.
     *
     * @see MaterialDynamicColors.onPrimaryFixedVariant
     */
    public fun onPrimaryFixedVariant(): Color = colors.onPrimaryFixedVariant().getColor(scheme)

    /**
     * Returns the secondary fixed color.
     *
     * @see MaterialDynamicColors.secondaryFixed
     */
    public fun secondaryFixed(): Color = colors.secondaryFixed().getColor(scheme)

    /**
     * Returns the secondary fixed dim color.
     *
     * @see MaterialDynamicColors.secondaryFixedDim
     */
    public fun secondaryFixedDim(): Color = colors.secondaryFixedDim().getColor(scheme)

    /**
     * Returns the on-secondary fixed color.
     *
     * @see MaterialDynamicColors.onSecondaryFixed
     */
    public fun onSecondaryFixed(): Color = colors.onSecondaryFixed().getColor(scheme)

    /**
     * Returns the on-secondary fixed variant color.
     *
     * @see MaterialDynamicColors.onSecondaryFixedVariant
     */
    public fun onSecondaryFixedVariant(): Color = colors.onSecondaryFixedVariant().getColor(scheme)

    /**
     * Returns the tertiary fixed color.
     *
     * @see MaterialDynamicColors.tertiaryFixed
     */
    public fun tertiaryFixed(): Color = colors.tertiaryFixed().getColor(scheme)

    /**
     * Returns the tertiary fixed dim color.
     *
     * @see MaterialDynamicColors.tertiaryFixedDim
     */
    public fun tertiaryFixedDim(): Color = colors.tertiaryFixedDim().getColor(scheme)

    /**
     * Returns the on-tertiary fixed color.
     *
     * @see MaterialDynamicColors.onTertiaryFixed
     */
    public fun onTertiaryFixed(): Color = colors.onTertiaryFixed().getColor(scheme)

    /**
     * Returns the on-tertiary fixed variant color.
     *
     * @see MaterialDynamicColors.onTertiaryFixedVariant
     */
    public fun onTertiaryFixedVariant(): Color = colors.onTertiaryFixedVariant().getColor(scheme)

    /**
     * Returns the control activated color.
     *
     * @see MaterialDynamicColors.controlActivated
     */
    public fun controlActivated(): Color = colors.controlActivated().getColor(scheme)

    /**
     * Returns the control normal color.
     *
     * @see MaterialDynamicColors.controlNormal
     */
    public fun controlNormal(): Color = colors.controlNormal().getColor(scheme)

    /**
     * Returns the control highlight color.
     *
     * @see MaterialDynamicColors.controlHighlight
     */
    public fun controlHighlight(): Color = colors.controlHighlight().getColor(scheme)

    /**
     * Returns the text primary inverse color.
     *
     * @see MaterialDynamicColors.textPrimaryInverse
     */
    public fun textPrimaryInverse(): Color = colors.textPrimaryInverse().getColor(scheme)

    /**
     * Returns the text secondary and tertiary inverse color.
     *
     * @see MaterialDynamicColors.textSecondaryAndTertiaryInverse
     */
    public fun textSecondaryAndTertiaryInverse(): Color =
        colors.textSecondaryAndTertiaryInverse().getColor(scheme)

    /**
     * Returns the text primary inverse disable only color.
     *
     * @see MaterialDynamicColors.textPrimaryInverseDisableOnly
     */
    public fun textPrimaryInverseDisableOnly(): Color =
        colors.textPrimaryInverseDisableOnly().getColor(scheme)

    /**
     * Returns the text secondary and tertiary inverse disabled color.
     *
     * @see MaterialDynamicColors.textSecondaryAndTertiaryInverseDisabled
     */
    public fun textSecondaryAndTertiaryInverseDisabled(): Color =
        colors.textSecondaryAndTertiaryInverseDisabled().getColor(scheme)

    /**
     * Returns the text hint inverse color.
     *
     * @see MaterialDynamicColors.textHintInverse
     */
    public fun textHintInverse(): Color = colors.textHintInverse().getColor(scheme)
}