package scheme

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import kotlin.test.Test

@OptIn(ExperimentalStdlibApi::class)
class DynamicSchemeTest {
    @Test
    fun testGetRotatedHue() {
        val sourceColor = Hct.from(180.0, 50.0, 50.0)
        val rotations = doubleArrayOf(0.0, 42.0, 360.0)
        val hues = doubleArrayOf(0.0, 15.0, 0.0)

        val expectedHue = DynamicScheme.getRotatedHue(sourceColor, rotations, hues)
        val actualHue = DynamicScheme.getRotatedHue(sourceColor, rotations, hues)

        expectedHue shouldBeExactly actualHue
    }

    @Test
    fun testOnColors() {
        val mkSourceColor = Hct.from(131.0, 70.0, 62.0)
        val mkScheme = SchemeTonalSpot(mkSourceColor, true, 1.0, ColorSpec.SpecVersion.SPEC_2025)

        val mcuSourceColor = hct.Hct.from(131.0, 70.0, 62.0)
        val mcuScheme = scheme.SchemeTonalSpot(
            mcuSourceColor,
            true,
            1.0,
            dynamiccolor.ColorSpec.SpecVersion.SPEC_2025,
            dynamiccolor.DynamicScheme.Platform.PHONE,
        )

        val mkOnPrimary = mkScheme.onPrimaryContainer
        val mcuOnPrimary = mcuScheme.onPrimaryContainer

        mkOnPrimary.toHexString() shouldBeEqualIgnoringCase mcuOnPrimary.toHexString()
    }

    @Test
    fun testAllColors2021() {
        testAllColors(ColorSpec.SpecVersion.SPEC_2021)
    }

    @Test
    fun testAllColors2025() {
        testAllColors(ColorSpec.SpecVersion.SPEC_2025)
    }

    private fun testAllColors(specVersion: ColorSpec.SpecVersion) {
        val mkSourceColor = Hct.from(131.0, 70.0, 62.0)
        val mkScheme = SchemeTonalSpot(mkSourceColor, true, 1.0, specVersion)

        val mcuSourceColor = hct.Hct.from(131.0, 70.0, 62.0)
        val mcuScheme = scheme.SchemeTonalSpot(
            mcuSourceColor,
            true,
            1.0,
            when (specVersion) {
                ColorSpec.SpecVersion.SPEC_2021 -> dynamiccolor.ColorSpec.SpecVersion.SPEC_2021
                ColorSpec.SpecVersion.SPEC_2025 -> dynamiccolor.ColorSpec.SpecVersion.SPEC_2025
            },
            dynamiccolor.DynamicScheme.Platform.PHONE,
        )

        val mkColors = MaterialDynamicColors()
            .allDynamicColors()
            .map {
                val color = it()
                "${color.name} -> #${color.getArgb(mkScheme).toHexString()}"
            }

        val mcuColors = dynamiccolor.MaterialDynamicColors()
            .allInOrder()
            .map {
                val color = it()
                "${color.name} -> #${color.getArgb(mcuScheme).toHexString()}"
            }

        val incorrect = mkColors.zip(mcuColors).filter { it.first != it.second }.joinToString("\n")
        incorrect shouldBeEqual ""
    }

    private fun dynamiccolor.MaterialDynamicColors.allInOrder() = listOf(
        ::primaryPaletteKeyColor,
        ::secondaryPaletteKeyColor,
        ::tertiaryPaletteKeyColor,
        ::errorPaletteKeyColor,
        ::neutralPaletteKeyColor,
        ::neutralVariantPaletteKeyColor,
        ::background,
        ::onBackground,
        ::surface,
        ::surfaceDim,
        ::surfaceBright,
        ::surfaceContainerLowest,
        ::surfaceContainerLow,
        ::surfaceContainer,
        ::surfaceContainerHigh,
        ::surfaceContainerHighest,
        ::onSurface,
        ::surfaceVariant,
        ::onSurfaceVariant,
        ::inverseSurface,
        ::inverseOnSurface,
        ::outline,
        ::outlineVariant,
        ::shadow,
        ::scrim,
        ::surfaceTint,
        ::primary,
        ::onPrimary,
        ::primaryContainer,
        ::onPrimaryContainer,
        ::inversePrimary,
        ::secondary,
        ::onSecondary,
        ::secondaryContainer,
        ::onSecondaryContainer,
        ::tertiary,
        ::onTertiary,
        ::tertiaryContainer,
        ::onTertiaryContainer,
        ::error,
        ::onError,
        ::errorContainer,
        ::onErrorContainer,
        ::primaryFixed,
        ::primaryFixedDim,
        ::onPrimaryFixed,
        ::onPrimaryFixedVariant,
        ::secondaryFixed,
        ::secondaryFixedDim,
        ::onSecondaryFixed,
        ::onSecondaryFixedVariant,
        ::tertiaryFixed,
        ::tertiaryFixedDim,
        ::onTertiaryFixed,
        ::onTertiaryFixedVariant,
        ::controlActivated,
        ::controlNormal,
        ::controlHighlight,
        ::textPrimaryInverse,
        ::textSecondaryAndTertiaryInverse,
        ::textPrimaryInverseDisableOnly,
        ::textSecondaryAndTertiaryInverseDisabled,
        ::textHintInverse,
    )
}
