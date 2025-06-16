package com.materialkolor.builder.export.standard

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.export.model.standard.createScheme
import com.materialkolor.builder.export.model.standard.standardColorsKt
import com.materialkolor.builder.settings.model.ColorSettings
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.ktx.getColor
import com.materialkolor.ktx.toHex
import kotlin.test.Test
import kotlin.test.assertEquals

class StandardColorsTest {

    @Test
    fun testStandardColors() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            selectedImage = null,
            packageName = "foo.bar.biz.buzz",
            includeMiscColors = true,
        )

        compare(settings)
    }

    @Test
    fun testStandardFullColors() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
                primary = Color(0xFF111111),
                secondary = Color(0xFF222222),
                tertiary = Color(0xFF333333),
                error = Color(0xFF444444),
                neutral = Color(0xFF555555),
                neutralVariant = Color(0xFF666666),
            ),
            isDarkMode = false,
            selectedImage = null,
            includeMiscColors = true,
        )

        compare(settings)
    }

    @Suppress("VisibleForTests")
    private fun compare(settings: Settings) {
        val map = MaterialDynamicColors()
        val lightScheme = createScheme(isDark = false, settings = settings)
        val light = { s: MaterialDynamicColors.() -> DynamicColor -> map.s().getColor(lightScheme).h() }

        val darkScheme = createScheme(isDark = true, settings = settings)
        val dark = { s: MaterialDynamicColors.() -> DynamicColor -> map.s().getColor(darkScheme).h() }

        val actual = standardColorsKt(settings)

        val expected = """
         package ${settings.packageName}

         import androidx.compose.ui.graphics.Color

         val Seed = ${settings.colors.seed.h()}

         val PrimaryLight = ${light { primary() }}
         val OnPrimaryLight = ${light { onPrimary() }}
         val PrimaryContainerLight = ${light { primaryContainer() }}
         val OnPrimaryContainerLight = ${light { onPrimaryContainer() }}
         val InversePrimaryLight = ${light { inversePrimary() }}
         val SecondaryLight = ${light { secondary() }}
         val OnSecondaryLight = ${light { onSecondary() }}
         val SecondaryContainerLight = ${light { secondaryContainer() }}
         val OnSecondaryContainerLight = ${light { onSecondaryContainer() }}
         val TertiaryLight = ${light { tertiary() }}
         val OnTertiaryLight = ${light { onTertiary() }}
         val TertiaryContainerLight = ${light { tertiaryContainer() }}
         val OnTertiaryContainerLight = ${light { onTertiaryContainer() }}
         val BackgroundLight = ${light { background() }}
         val OnBackgroundLight = ${light { onBackground() }}
         val SurfaceLight = ${light { surface() }}
         val OnSurfaceLight = ${light { onSurface() }}
         val SurfaceVariantLight = ${light { surfaceVariant() }}
         val OnSurfaceVariantLight = ${light { onSurfaceVariant() }}
         val SurfaceTintLight = ${light { surfaceTint() }}
         val InverseSurfaceLight = ${light { inverseSurface() }}
         val InverseOnSurfaceLight = ${light { inverseOnSurface() }}
         val ErrorLight = ${light { error() }}
         val OnErrorLight = ${light { onError() }}
         val ErrorContainerLight = ${light { errorContainer() }}
         val OnErrorContainerLight = ${light { onErrorContainer() }}
         val OutlineLight = ${light { outline() }}
         val OutlineVariantLight = ${light { outlineVariant() }}
         val ScrimLight = ${light { scrim() }}
         val SurfaceBrightLight = ${light { surfaceBright() }}
         val SurfaceContainerLight = ${light { surfaceContainer() }}
         val SurfaceContainerHighLight = ${light { surfaceContainerHigh() }}
         val SurfaceContainerHighestLight = ${light { surfaceContainerHighest() }}
         val SurfaceContainerLowLight = ${light { surfaceContainerLow() }}
         val SurfaceContainerLowestLight = ${light { surfaceContainerLowest() }}
         val SurfaceDimLight = ${light { surfaceDim() }}
         val ShadowLight = ${light { shadow() }}
         val ControlActivatedLight = ${light { controlActivated() }}
         val ControlNormalLight = ${light { controlNormal() }}
         val ControlHighlightLight = ${light { controlHighlight() }}
         val TextPrimaryInverseLight = ${light { textPrimaryInverse() }}
         val TextSecondaryAndTertiaryInverseLight = ${light { textSecondaryAndTertiaryInverse() }}
         val TextPrimaryInverseDisableOnlyLight = ${light { textPrimaryInverseDisableOnly() }}
         val TextSecondaryAndTertiaryInverseDisabledLight = ${light { textSecondaryAndTertiaryInverseDisabled() }}
         val TextHintInverseLight = ${light { textHintInverse() }}

         val PrimaryDark = ${dark { primary() }}
         val OnPrimaryDark = ${dark { onPrimary() }}
         val PrimaryContainerDark = ${dark { primaryContainer() }}
         val OnPrimaryContainerDark = ${dark { onPrimaryContainer() }}
         val InversePrimaryDark = ${dark { inversePrimary() }}
         val SecondaryDark = ${dark { secondary() }}
         val OnSecondaryDark = ${dark { onSecondary() }}
         val SecondaryContainerDark = ${dark { secondaryContainer() }}
         val OnSecondaryContainerDark = ${dark { onSecondaryContainer() }}
         val TertiaryDark = ${dark { tertiary() }}
         val OnTertiaryDark = ${dark { onTertiary() }}
         val TertiaryContainerDark = ${dark { tertiaryContainer() }}
         val OnTertiaryContainerDark = ${dark { onTertiaryContainer() }}
         val BackgroundDark = ${dark { background() }}
         val OnBackgroundDark = ${dark { onBackground() }}
         val SurfaceDark = ${dark { surface() }}
         val OnSurfaceDark = ${dark { onSurface() }}
         val SurfaceVariantDark = ${dark { surfaceVariant() }}
         val OnSurfaceVariantDark = ${dark { onSurfaceVariant() }}
         val SurfaceTintDark = ${dark { surfaceTint() }}
         val InverseSurfaceDark = ${dark { inverseSurface() }}
         val InverseOnSurfaceDark = ${dark { inverseOnSurface() }}
         val ErrorDark = ${dark { error() }}
         val OnErrorDark = ${dark { onError() }}
         val ErrorContainerDark = ${dark { errorContainer() }}
         val OnErrorContainerDark = ${dark { onErrorContainer() }}
         val OutlineDark = ${dark { outline() }}
         val OutlineVariantDark = ${dark { outlineVariant() }}
         val ScrimDark = ${dark { scrim() }}
         val SurfaceBrightDark = ${dark { surfaceBright() }}
         val SurfaceContainerDark = ${dark { surfaceContainer() }}
         val SurfaceContainerHighDark = ${dark { surfaceContainerHigh() }}
         val SurfaceContainerHighestDark = ${dark { surfaceContainerHighest() }}
         val SurfaceContainerLowDark = ${dark { surfaceContainerLow() }}
         val SurfaceContainerLowestDark = ${dark { surfaceContainerLowest() }}
         val SurfaceDimDark = ${dark { surfaceDim() }}
         val ShadowDark = ${dark { shadow() }}
         val ControlActivatedDark = ${dark { controlActivated() }}
         val ControlNormalDark = ${dark { controlNormal() }}
         val ControlHighlightDark = ${dark { controlHighlight() }}
         val TextPrimaryInverseDark = ${dark { textPrimaryInverse() }}
         val TextSecondaryAndTertiaryInverseDark = ${dark { textSecondaryAndTertiaryInverse() }}
         val TextPrimaryInverseDisableOnlyDark = ${dark { textPrimaryInverseDisableOnly() }}
         val TextSecondaryAndTertiaryInverseDisabledDark = ${dark { textSecondaryAndTertiaryInverseDisabled() }}
         val TextHintInverseDark = ${dark { textHintInverse() }}
         
         val PrimaryFixed = ${dark { primaryFixed() }}
         val PrimaryFixedDim = ${dark { primaryFixedDim() }}
         val OnPrimaryFixed = ${dark { onPrimaryFixed() }}
         val OnPrimaryFixedVariant = ${dark { onPrimaryFixedVariant() }}
         val SecondaryFixed = ${dark { secondaryFixed() }}
         val SecondaryFixedDim = ${dark { secondaryFixedDim() }}
         val OnSecondaryFixed = ${dark { onSecondaryFixed() }}
         val OnSecondaryFixedVariant = ${dark { onSecondaryFixedVariant() }}
         val TertiaryFixed = ${dark { tertiaryFixed() }}
         val TertiaryFixedDim = ${dark { tertiaryFixedDim() }}
         val OnTertiaryFixed = ${dark { onTertiaryFixed() }}
         val OnTertiaryFixedVariant = ${dark { onTertiaryFixedVariant() }}
         val PrimaryPaletteKeyColor = ${dark { primaryPaletteKeyColor() }}
         val SecondaryPaletteKeyColor = ${dark { secondaryPaletteKeyColor() }}
         val TertiaryPaletteKeyColor = ${dark { tertiaryPaletteKeyColor() }}
         val NeutralPaletteKeyColor = ${dark { neutralPaletteKeyColor() }}
         val NeutralVariantPaletteKeyColor = ${dark { neutralVariantPaletteKeyColor() }}
         val ErrorPaletteKeyColor = ${dark { errorPaletteKeyColor() }}
     """.trimIndent()
        assertEquals(expected, actual)
    }

    private fun Color.h() = "Color(0x${toHex(includePrefix = false, alwaysIncludeAlpha = true)})"
}
