package scheme

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class DynamicSchemeTest {
    @Test
    fun testGetRotatedHue() {
        val sourceColor = Hct.from(180.0, 50.0, 50.0)
        val rotations = doubleArrayOf(0.0, 42.0, 360.0)
        val hues = doubleArrayOf(0.0, 15.0, 0.0)

        val expectedHue = DynamicScheme.getRotatedHue(sourceColor, rotations, hues)
        val actualHue = dynamiccolor.DynamicScheme.getRotatedHue(hct.Hct.from(180.0, 50.0, 50.0), rotations, hues)

        expectedHue shouldBeExactly actualHue
    }

    @Test
    fun testOnColors() {
        val mkSourceColor = Hct.from(131.0, 70.0, 62.0)
        val mkScheme =
            SchemeTonalSpot(mkSourceColor, true, 1.0, ColorSpec.SpecVersion.SPEC_2025, DynamicScheme.Platform.PHONE)

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
        val mkScheme = SchemeTonalSpot(mkSourceColor, true, 1.0, specVersion, DynamicScheme.Platform.PHONE)

        val mcuSourceColor = hct.Hct.from(131.0, 70.0, 62.0)
        val mcuScheme = scheme.SchemeTonalSpot(
            mcuSourceColor,
            true,
            1.0,
            when (specVersion) {
                ColorSpec.SpecVersion.SPEC_2021 -> dynamiccolor.ColorSpec.SpecVersion.SPEC_2021
                ColorSpec.SpecVersion.SPEC_2025 -> dynamiccolor.ColorSpec.SpecVersion.SPEC_2025
                ColorSpec.SpecVersion.SPEC_2026 -> dynamiccolor.ColorSpec.SpecVersion.SPEC_2026
            },
            dynamiccolor.DynamicScheme.Platform.PHONE,
        )

        val mkColors = MaterialDynamicColors()
        val javaColors = dynamiccolor.MaterialDynamicColors()
        for (role in conformance.ROLE_NAMES) {
            val getter = "get${role.replaceFirstChar { it.uppercase() }}"
            val mkColor = mkColors.javaClass
                .getMethod(
                    getter,
                ).invoke(mkColors) as com.materialkolor.dynamiccolor.DynamicColor?
            val javaColor = javaColors.javaClass.getMethod(role).invoke(javaColors) as dynamiccolor.DynamicColor?
            assertEquals(javaColor?.getArgb(mcuScheme), mkColor?.getArgb(mkScheme), role)
        }
    }
}
