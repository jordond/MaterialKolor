package com.materialkolor.builder.kit.widget

import com.materialkolor.hct.Hct
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlin.test.Test
import kotlin.test.assertEquals

class GamutTrackTest {
    @Test
    fun search_hundredHueAndTonePairs_matchesABruteForceScanWithinHalfAUnit() {
        for (index in 0 until 100) {
            val hue = (index * 37 % 360).toDouble()
            val tone = (5 + index * 13 % 91).toDouble()

            assertEquals(
                expected = pickerBruteMaxChroma(hue, tone),
                actual = GamutLimit.search(hue, tone),
                absoluteTolerance = 0.5,
                message = "hue $hue tone $tone",
            )
        }
    }

    @Test
    fun maxChroma_fractionalHueAndTone_answersForTheWholeNumbers() {
        assertEquals(GamutLimit.search(282.0, 40.0), GamutLimit.maxChroma(282.4, 39.6))
        assertEquals(GamutLimit.search(0.0, 50.0), GamutLimit.maxChroma(359.7, 50.0))
    }

    @Test
    fun maxChroma_blackAndWhite_reachOnlyTheirOwnChroma() {
        val white = Hct.fromInt(0xFFFFFFFF.toInt()).chroma

        assertEquals(0.0, GamutLimit.maxChroma(30.0, 0.0), absoluteTolerance = 0.5)
        assertEquals(white, GamutLimit.maxChroma(30.0, 100.0), absoluteTolerance = 0.5)
    }

    @Test
    fun chromaCeiling_everyHueAndTone_liesPastWhatSrgbReaches() {
        for (hue in 0 until 360 step 3) {
            for (tone in 0..100 step 2) {
                Hct.from(hue.toDouble(), PickerFarPastSrgb, tone.toDouble()).chroma shouldBeLessThan ChromaCeiling
            }
        }
    }

    @Test
    fun hctChannel_valueAndFraction_undoEachOther() {
        for (channel in HctChannel.entries) {
            for (step in 0..10) {
                val fraction = step / 10f

                assertEquals(fraction, channel.fractionOf(channel.valueAt(fraction)), absoluteTolerance = 1e-6f)
            }
        }
    }
}

/** The most chroma any ask from zero to the ceiling comes back with, in half unit steps. */
private fun pickerBruteMaxChroma(
    hue: Double,
    tone: Double,
): Double =
    (0..(ChromaCeiling * 2).toInt())
        .maxOf { step -> Hct.from(hue, step / 2.0, tone).chroma }
        .coerceAtMost(ChromaCeiling)

private const val PickerFarPastSrgb: Double = 200.0
