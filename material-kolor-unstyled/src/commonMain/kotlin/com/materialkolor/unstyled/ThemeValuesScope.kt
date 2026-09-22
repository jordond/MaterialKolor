package com.materialkolor.unstyled

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.MaterialKolors

/**
 * Marks the receiver of [themeValues] so an enclosing builder cannot be reached by accident.
 */
@DslMarker
public annotation class ThemeValuesDsl

/**
 * The receiver of [themeValues].
 *
 * It forwards every [MaterialKolors] role under the same name and turns `token to color` into a
 * recorded pair. Instances come from [themeValues] only.
 */
@ThemeValuesDsl
public class ThemeValuesScope internal constructor(
    private val kolors: MaterialKolors,
) {
    private val values = LinkedHashMap<ThemeToken<Color>, Color>()

    /**
     * Records [color] under this token. A token assigned twice keeps the last color.
     */
    public infix fun ThemeToken<Color>.to(color: Color) {
        values[this] = color
    }

    /**
     * The [MaterialKolors.primaryPaletteKeyColor] color of the scheme this block builds values for.
     */
    public fun primaryPaletteKeyColor(): Color = kolors.primaryPaletteKeyColor()

    /**
     * The [MaterialKolors.secondaryPaletteKeyColor] color of the scheme this block builds values for.
     */
    public fun secondaryPaletteKeyColor(): Color = kolors.secondaryPaletteKeyColor()

    /**
     * The [MaterialKolors.tertiaryPaletteKeyColor] color of the scheme this block builds values for.
     */
    public fun tertiaryPaletteKeyColor(): Color = kolors.tertiaryPaletteKeyColor()

    /**
     * The [MaterialKolors.errorPaletteKeyColor] color of the scheme this block builds values for.
     */
    public fun errorPaletteKeyColor(): Color = kolors.errorPaletteKeyColor()

    /**
     * The [MaterialKolors.neutralPaletteKeyColor] color of the scheme this block builds values for.
     */
    public fun neutralPaletteKeyColor(): Color = kolors.neutralPaletteKeyColor()

    /**
     * The [MaterialKolors.neutralVariantPaletteKeyColor] color of the scheme this block builds values for.
     */
    public fun neutralVariantPaletteKeyColor(): Color = kolors.neutralVariantPaletteKeyColor()

    /**
     * The [MaterialKolors.background] color of the scheme this block builds values for.
     */
    public fun background(): Color = kolors.background()

    /**
     * The [MaterialKolors.onBackground] color of the scheme this block builds values for.
     */
    public fun onBackground(): Color = kolors.onBackground()

    /**
     * The [MaterialKolors.surface] color of the scheme this block builds values for.
     */
    public fun surface(): Color = kolors.surface()

    /**
     * The [MaterialKolors.surfaceDim] color of the scheme this block builds values for.
     */
    public fun surfaceDim(): Color = kolors.surfaceDim()

    /**
     * The [MaterialKolors.surfaceBright] color of the scheme this block builds values for.
     */
    public fun surfaceBright(): Color = kolors.surfaceBright()

    /**
     * The [MaterialKolors.surfaceContainerLowest] color of the scheme this block builds values for.
     */
    public fun surfaceContainerLowest(): Color = kolors.surfaceContainerLowest()

    /**
     * The [MaterialKolors.surfaceContainerLow] color of the scheme this block builds values for.
     */
    public fun surfaceContainerLow(): Color = kolors.surfaceContainerLow()

    /**
     * The [MaterialKolors.surfaceContainer] color of the scheme this block builds values for.
     */
    public fun surfaceContainer(): Color = kolors.surfaceContainer()

    /**
     * The [MaterialKolors.surfaceContainerHigh] color of the scheme this block builds values for.
     */
    public fun surfaceContainerHigh(): Color = kolors.surfaceContainerHigh()

    /**
     * The [MaterialKolors.surfaceContainerHighest] color of the scheme this block builds values for.
     */
    public fun surfaceContainerHighest(): Color = kolors.surfaceContainerHighest()

    /**
     * The [MaterialKolors.onSurface] color of the scheme this block builds values for.
     */
    public fun onSurface(): Color = kolors.onSurface()

    /**
     * The [MaterialKolors.surfaceVariant] color of the scheme this block builds values for.
     */
    public fun surfaceVariant(): Color = kolors.surfaceVariant()

    /**
     * The [MaterialKolors.onSurfaceVariant] color of the scheme this block builds values for.
     */
    public fun onSurfaceVariant(): Color = kolors.onSurfaceVariant()

    /**
     * The [MaterialKolors.inverseSurface] color of the scheme this block builds values for.
     */
    public fun inverseSurface(): Color = kolors.inverseSurface()

    /**
     * The [MaterialKolors.inverseOnSurface] color of the scheme this block builds values for.
     */
    public fun inverseOnSurface(): Color = kolors.inverseOnSurface()

    /**
     * The [MaterialKolors.outline] color of the scheme this block builds values for.
     */
    public fun outline(): Color = kolors.outline()

    /**
     * The [MaterialKolors.outlineVariant] color of the scheme this block builds values for.
     */
    public fun outlineVariant(): Color = kolors.outlineVariant()

    /**
     * The [MaterialKolors.shadow] color of the scheme this block builds values for.
     */
    public fun shadow(): Color = kolors.shadow()

    /**
     * The [MaterialKolors.scrim] color of the scheme this block builds values for.
     */
    public fun scrim(): Color = kolors.scrim()

    /**
     * The [MaterialKolors.surfaceTint] color of the scheme this block builds values for.
     */
    public fun surfaceTint(): Color = kolors.surfaceTint()

    /**
     * The [MaterialKolors.primary] color of the scheme this block builds values for.
     */
    public fun primary(): Color = kolors.primary()

    /**
     * The [MaterialKolors.onPrimary] color of the scheme this block builds values for.
     */
    public fun onPrimary(): Color = kolors.onPrimary()

    /**
     * The [MaterialKolors.primaryContainer] color of the scheme this block builds values for.
     */
    public fun primaryContainer(): Color = kolors.primaryContainer()

    /**
     * The [MaterialKolors.onPrimaryContainer] color of the scheme this block builds values for.
     */
    public fun onPrimaryContainer(): Color = kolors.onPrimaryContainer()

    /**
     * The [MaterialKolors.inversePrimary] color of the scheme this block builds values for.
     */
    public fun inversePrimary(): Color = kolors.inversePrimary()

    /**
     * The [MaterialKolors.secondary] color of the scheme this block builds values for.
     */
    public fun secondary(): Color = kolors.secondary()

    /**
     * The [MaterialKolors.onSecondary] color of the scheme this block builds values for.
     */
    public fun onSecondary(): Color = kolors.onSecondary()

    /**
     * The [MaterialKolors.secondaryContainer] color of the scheme this block builds values for.
     */
    public fun secondaryContainer(): Color = kolors.secondaryContainer()

    /**
     * The [MaterialKolors.onSecondaryContainer] color of the scheme this block builds values for.
     */
    public fun onSecondaryContainer(): Color = kolors.onSecondaryContainer()

    /**
     * The [MaterialKolors.tertiary] color of the scheme this block builds values for.
     */
    public fun tertiary(): Color = kolors.tertiary()

    /**
     * The [MaterialKolors.onTertiary] color of the scheme this block builds values for.
     */
    public fun onTertiary(): Color = kolors.onTertiary()

    /**
     * The [MaterialKolors.tertiaryContainer] color of the scheme this block builds values for.
     */
    public fun tertiaryContainer(): Color = kolors.tertiaryContainer()

    /**
     * The [MaterialKolors.onTertiaryContainer] color of the scheme this block builds values for.
     */
    public fun onTertiaryContainer(): Color = kolors.onTertiaryContainer()

    /**
     * The [MaterialKolors.error] color of the scheme this block builds values for.
     */
    public fun error(): Color = kolors.error()

    /**
     * The [MaterialKolors.onError] color of the scheme this block builds values for.
     */
    public fun onError(): Color = kolors.onError()

    /**
     * The [MaterialKolors.errorContainer] color of the scheme this block builds values for.
     */
    public fun errorContainer(): Color = kolors.errorContainer()

    /**
     * The [MaterialKolors.onErrorContainer] color of the scheme this block builds values for.
     */
    public fun onErrorContainer(): Color = kolors.onErrorContainer()

    /**
     * The [MaterialKolors.primaryFixed] color of the scheme this block builds values for.
     */
    public fun primaryFixed(): Color = kolors.primaryFixed()

    /**
     * The [MaterialKolors.primaryFixedDim] color of the scheme this block builds values for.
     */
    public fun primaryFixedDim(): Color = kolors.primaryFixedDim()

    /**
     * The [MaterialKolors.onPrimaryFixed] color of the scheme this block builds values for.
     */
    public fun onPrimaryFixed(): Color = kolors.onPrimaryFixed()

    /**
     * The [MaterialKolors.onPrimaryFixedVariant] color of the scheme this block builds values for.
     */
    public fun onPrimaryFixedVariant(): Color = kolors.onPrimaryFixedVariant()

    /**
     * The [MaterialKolors.secondaryFixed] color of the scheme this block builds values for.
     */
    public fun secondaryFixed(): Color = kolors.secondaryFixed()

    /**
     * The [MaterialKolors.secondaryFixedDim] color of the scheme this block builds values for.
     */
    public fun secondaryFixedDim(): Color = kolors.secondaryFixedDim()

    /**
     * The [MaterialKolors.onSecondaryFixed] color of the scheme this block builds values for.
     */
    public fun onSecondaryFixed(): Color = kolors.onSecondaryFixed()

    /**
     * The [MaterialKolors.onSecondaryFixedVariant] color of the scheme this block builds values for.
     */
    public fun onSecondaryFixedVariant(): Color = kolors.onSecondaryFixedVariant()

    /**
     * The [MaterialKolors.tertiaryFixed] color of the scheme this block builds values for.
     */
    public fun tertiaryFixed(): Color = kolors.tertiaryFixed()

    /**
     * The [MaterialKolors.tertiaryFixedDim] color of the scheme this block builds values for.
     */
    public fun tertiaryFixedDim(): Color = kolors.tertiaryFixedDim()

    /**
     * The [MaterialKolors.onTertiaryFixed] color of the scheme this block builds values for.
     */
    public fun onTertiaryFixed(): Color = kolors.onTertiaryFixed()

    /**
     * The [MaterialKolors.onTertiaryFixedVariant] color of the scheme this block builds values for.
     */
    public fun onTertiaryFixedVariant(): Color = kolors.onTertiaryFixedVariant()

    /**
     * The [MaterialKolors.controlActivated] color of the scheme this block builds values for.
     */
    public fun controlActivated(): Color = kolors.controlActivated()

    /**
     * The [MaterialKolors.controlNormal] color of the scheme this block builds values for.
     */
    public fun controlNormal(): Color = kolors.controlNormal()

    /**
     * The [MaterialKolors.controlHighlight] color of the scheme this block builds values for.
     */
    public fun controlHighlight(): Color = kolors.controlHighlight()

    /**
     * The [MaterialKolors.textPrimaryInverse] color of the scheme this block builds values for.
     */
    public fun textPrimaryInverse(): Color = kolors.textPrimaryInverse()

    /**
     * The [MaterialKolors.textSecondaryAndTertiaryInverse] color of the scheme this block builds values for.
     */
    public fun textSecondaryAndTertiaryInverse(): Color = kolors.textSecondaryAndTertiaryInverse()

    /**
     * The [MaterialKolors.textPrimaryInverseDisableOnly] color of the scheme this block builds values for.
     */
    public fun textPrimaryInverseDisableOnly(): Color = kolors.textPrimaryInverseDisableOnly()

    /**
     * The [MaterialKolors.textSecondaryAndTertiaryInverseDisabled] color of the scheme this block builds values for.
     */
    public fun textSecondaryAndTertiaryInverseDisabled(): Color = kolors.textSecondaryAndTertiaryInverseDisabled()

    /**
     * The [MaterialKolors.textHintInverse] color of the scheme this block builds values for.
     */
    public fun textHintInverse(): Color = kolors.textHintInverse()

    internal fun recorded(): Map<ThemeToken<Color>, Color> = LinkedHashMap(values)
}
