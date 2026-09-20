package com.materialkolor

import com.materialkolor.dynamiccolor.ContrastCurve
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.ToneDeltaPair
import com.materialkolor.dynamiccolor.ToneDeltaPair.TonePolarity
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.temperature.TemperatureCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class ValueContractsTest {
    @Test
    fun hctEqualityUsesArgbIncludingAlpha() {
        val first = Hct.fromInt(0xff4285f4.toInt())
        val second = Hct.fromInt(0xff4285f4.toInt())
        assertNotSame(first, second)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, Hct.fromInt(0x004285f4))
        assertNotEquals(first, Hct.fromInt(0xff4285f5.toInt()))
        assertEquals("value", mapOf(first to "value")[second])
    }

    @Test
    fun hctAdjustmentsLeaveTheOriginalUnchanged() {
        val original = Hct.fromInt(0xff4285f4.toInt())
        val argb = original.toInt()
        val coordinates = listOf(original.hue, original.chroma, original.tone)
        val adjustments = listOf(
            original.withHue(120.0),
            original.withChroma(5.0),
            original.withTone(90.0),
        )
        adjustments.forEach {
            assertNotSame(original, it)
            assertNotEquals(argb, it.toInt())
        }
        assertEquals(argb, original.toInt())
        assertEquals(coordinates, listOf(original.hue, original.chroma, original.tone))
    }

    @Test
    fun instanceHueClassifiersMatchCompanionBoundaries() {
        for (hue in listOf(0.0, 104.9, 105.0, 124.9, 125.0, 170.0, 206.9, 207.0, 250.0, 269.9, 270.0)) {
            val color = Hct.from(hue, 30.0, 50.0)
            assertEquals(Hct.isBlue(color.hue), color.isBlue())
            assertEquals(Hct.isYellow(color.hue), color.isYellow())
            assertEquals(Hct.isCyan(color.hue), color.isCyan())
        }
    }

    @Test
    fun tonalPaletteEqualityExcludesToneCache() {
        val first = TonalPalette.fromHueAndChroma(120.0, 40.0)
        val second = TonalPalette.fromHueAndChroma(120.0, 40.0)
        val hash = first.hashCode()
        val diagnostic = first.toString()
        val lookup = mapOf(first to "palette")
        listOf(0, 40, 50, 98, 99, 100).forEach(first::tone)
        assertEquals(first, second)
        assertEquals(hash, first.hashCode())
        assertEquals(hash, second.hashCode())
        assertEquals(diagnostic, first.toString())
        assertEquals(diagnostic, second.toString())
        assertEquals("palette", lookup[second])
        assertNotEquals(first, TonalPalette.fromHueAndChroma(120.0, 41.0))
        val low = Hct.from(120.0, 40.0, 20.0)
        val high = Hct.from(120.0, 40.0, 80.0)
        assertNotEquals(TonalPalette.fromHct(low), TonalPalette.fromHct(high))
    }

    @Test
    fun tonalPaletteFloatingPointEqualityRetainsSignedZero() {
        assertNotEquals(
            TonalPalette.fromHueAndChroma(0.0, 0.0),
            TonalPalette.fromHueAndChroma(-0.0, 0.0),
        )
        assertNotEquals(
            TonalPalette.fromHueAndChroma(120.0, 0.0),
            TonalPalette.fromHueAndChroma(120.0, -0.0),
        )
    }

    @Test
    fun tonalPaletteRejectsNanHueLikeUpstreamKotlin() {
        assertFailsWith<IllegalArgumentException> {
            TonalPalette.fromHueAndChroma(Double.NaN, 0.0)
        }
    }

    @Test
    fun temperatureEqualityExcludesEveryLazyCache() {
        val color = Hct.fromInt(0xff4285f4.toInt())
        val first = TemperatureCache(color)
        val second = TemperatureCache(Hct.fromInt(color.toInt()))
        val hash = first.hashCode()
        val diagnostic = first.toString()
        val lookup = mapOf(first to "temperature")
        first.complement
        first.analogousColors
        first.getAnalogousColors(7, 12)
        assertTrue(first.getRelativeTemperature(color) in 0.0..1.0)
        assertEquals(first, second)
        assertEquals(hash, first.hashCode())
        assertEquals(hash, second.hashCode())
        assertEquals(diagnostic, first.toString())
        assertEquals(diagnostic, second.toString())
        assertEquals("temperature", lookup[second])
        assertNotEquals(first, TemperatureCache(Hct.fromInt(0xffff0000.toInt())))
    }

    @Test
    fun contrastCurveEqualityIncludesEveryInput() {
        fun curve(values: List<Double>) = ContrastCurve(values[0], values[1], values[2], values[3])
        val values = listOf(1.0, 3.0, 4.5, 7.0)
        val original = curve(values)
        // Rendered through the running platform. Kotlin/JS prints whole doubles as "1", not "1.0".
        val (low, normal, medium, high) = values
        assertEquals(
            "ContrastCurve(low=$low, normal=$normal, medium=$medium, high=$high)",
            original.toString(),
        )
        assertEquals(original, curve(values))
        assertEquals(original.hashCode(), curve(values).hashCode())
        for (index in values.indices) {
            assertNotEquals(original, curve(values.mapIndexed { i, value -> if (i == index) value + 1 else value }))
            val nanValues = values.mapIndexed { i, value -> if (i == index) Double.NaN else value }
            assertEquals(curve(nanValues), curve(nanValues))
            assertEquals(curve(nanValues).hashCode(), curve(nanValues).hashCode())
            assertNotEquals(
                curve(values.mapIndexed { i, value -> if (i == index) 0.0 else value }),
                curve(values.mapIndexed { i, value -> if (i == index) -0.0 else value }),
            )
        }
    }

    @Test
    fun dynamicColorEqualityUsesFunctionIdentityAndExcludesHctCache() {
        val palette: (DynamicScheme) -> TonalPalette = { it.primaryPalette }
        val tone: (DynamicScheme) -> Double = { 42.0 }
        val first = DynamicColor.fromPalette("custom", palette, tone)
        val second = DynamicColor.fromPalette("custom", palette, tone)
        val hash = first.hashCode()
        first.getHct(SchemeTonalSpot(Hct.fromInt(0xff4285f4.toInt()), false, 0.0))
        assertEquals(first, second)
        assertEquals(hash, first.hashCode())
        assertEquals(hash, second.hashCode())
        assertNotEquals(first, DynamicColor.fromPalette("other", palette, tone))
        assertNotEquals(first, DynamicColor.fromPalette("custom", palette, tone, true))
        val otherTone: (DynamicScheme) -> Double = { 42.0 }
        assertNotEquals(first, DynamicColor.fromPalette("custom", palette, otherTone))
    }

    @Test
    fun toneDeltaPairEqualityIncludesNanSignedZeroAndPolicy() {
        val first = DynamicColor.fromArgb("first", 0xff4285f4.toInt())
        val second = DynamicColor.fromArgb("second", 0xffff0000.toInt())

        fun pair(
            delta: Double,
            stayTogether: Boolean = true,
        ) = ToneDeltaPair(first, second, delta, TonePolarity.DARKER, stayTogether)
        assertEquals(pair(Double.NaN), pair(Double.NaN))
        assertEquals(pair(Double.NaN).hashCode(), pair(Double.NaN).hashCode())
        assertNotEquals(pair(0.0), pair(-0.0))
        assertNotEquals(pair(15.0), pair(15.0, false))
        assertNotEquals(
            pair(15.0),
            ToneDeltaPair(first, second, 15.0, TonePolarity.LIGHTER),
        )
        assertNotEquals(
            pair(15.0),
            ToneDeltaPair(first, second, 15.0, TonePolarity.DARKER, constraint = ToneDeltaPair.DeltaConstraint.FARTHER),
        )
    }
}
