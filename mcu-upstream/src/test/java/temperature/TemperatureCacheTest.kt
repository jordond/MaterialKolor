package temperature

import com.materialkolor.hct.Hct
import kotlin.test.Test
import kotlin.test.assertEquals
import com.materialkolor.temperature.TemperatureCache as KotlinTemperatureCache
import temperature.TemperatureCache as JavaTemperatureCache

class TemperatureCacheTest {

    @Test
    fun testRawTemperature() {
        val testColors = listOf(0xff0000ff, 0xffff0000, 0xff00ff00, 0xffffffff, 0xff000000)

        for (color in testColors) {
            val kotlinResult = KotlinTemperatureCache.rawTemperature(Hct.fromInt(color.toInt()))
            val javaResult = JavaTemperatureCache.rawTemperature(hct.Hct.fromInt(color.toInt()))
            assertEquals(kotlinResult, javaResult, 0.001, "Raw temperature mismatch for color $color")
        }
    }

    @Test
    fun testRelativeTemperature() {
        val testColors = listOf(0xff0000ff, 0xffff0000, 0xff00ff00, 0xffffffff, 0xff000000)

        for (color in testColors) {
            val kotlinCache = KotlinTemperatureCache(Hct.fromInt(color.toInt()))
            val javaCache = JavaTemperatureCache(hct.Hct.fromInt(color.toInt()))
            val kotlinRelativeTemp = kotlinCache.getRelativeTemperature(kotlinCache.complement)
            val javaRelativeTemp = javaCache.getRelativeTemperature(javaCache.complement)

            assertEquals(
                expected = kotlinRelativeTemp,
                actual = javaRelativeTemp,
                absoluteTolerance = 0.001,
                message = "Relative temperature mismatch for color ${color.toString(16)}",
            )
        }
    }

    @Test
    fun testComplement() {
        val testColors = listOf(0xff0000ff, 0xffff0000, 0xff00ff00, 0xffffffff, 0xff000000)

        for (color in testColors) {
            val kotlinCache = KotlinTemperatureCache(Hct.fromInt(color.toInt()))
            val javaCache = JavaTemperatureCache(hct.Hct.fromInt(color.toInt()))
            assertEquals(
                expected = kotlinCache.complement.toInt(),
                actual = javaCache.complement.toInt(),
                message = "Complement mismatch for color $color",
            )
        }
    }

    @Test
    fun testAnalogousColors() {
        val testColors = listOf(0xff0000ff, 0xffff0000, 0xff00ff00, 0xffffffff, 0xff000000)

        for (color in testColors) {
            val kotlinCache = KotlinTemperatureCache(Hct.fromInt(color.toInt()))
            val javaCache = JavaTemperatureCache(hct.Hct.fromInt(color.toInt()))

            val actual = kotlinCache.analogousColors.map { it.toInt() }
            val expected = javaCache.analogousColors.map { it.toInt() }

            assertEquals(actual, expected, "Analogous colors mismatch for color $color")
        }
    }
}