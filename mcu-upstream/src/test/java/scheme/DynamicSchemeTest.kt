package scheme

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.ints.shouldBeExactly
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

        mkOnPrimary.toHexString() shouldBeEqualIgnoringCase  mcuOnPrimary.toHexString()
    }
}
