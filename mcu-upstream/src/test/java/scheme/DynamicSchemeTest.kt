package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import io.kotest.matchers.doubles.shouldBeExactly
import kotlin.test.Test

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
}