package com.materialkolor.builder.kit.widget

import com.materialkolor.hct.Hct
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlin.test.Test
import kotlin.test.assertEquals

class GamutTrackTest {
    @Test
    fun edge_hundredHueAndTonePairs_matchesABruteForceScanWithinHalfAUnit() {
        for (index in 0 until 100) {
            val hue = (index * 37 % 360).toDouble()
            val tone = (5 + index * 13 % 91).toDouble()

            assertEquals(
                expected = pickerBruteMaxChroma(hue, tone),
                actual = GamutLimit.edge(hue, tone),
                absoluteTolerance = 0.5,
                message = "hue $hue tone $tone",
            )
        }
    }

    @Test
    fun edge_everyWholeHueAtBlackAndTheLightTones_matchesABruteForceScanWithinHalfAUnit() {
        for (tone in listOf(0.0, 95.0, 96.0, 97.0, 99.0, 99.5, 100.0)) {
            for (hue in 0 until 360) {
                assertEquals(
                    expected = pickerBruteMaxChroma(hue.toDouble(), tone),
                    actual = GamutLimit.edge(hue.toDouble(), tone),
                    absoluteTolerance = 0.5,
                    message = "hue $hue tone $tone",
                )
            }
        }
    }

    /**
     * Just above black and at tone 98, eight bit rounding hands some asks back as a neighbor or a
     * neutral grey off the asked hue, such as the grey at hue 209 with 2.8 chroma that an ask at hue
     * 0 and tone 98 lands on. The scan counts those, so it can land up to two above the edge there.
     * The edge still never claims more than an ask reaches.
     */
    @Test
    fun edge_justAboveBlackAndAtTone98_neverClaimsMoreThanAnAskReaches() {
        for (tone in listOf(0.5, 1.0, 2.0, 3.0, 98.0)) {
            for (hue in 0 until 360 step 10) {
                val edge = GamutLimit.edge(hue.toDouble(), tone)
                val scanned = pickerBruteMaxChroma(hue.toDouble(), tone)

                withClue("hue $hue tone $tone") { edge shouldBeLessThan scanned + 0.5 }
            }
        }
    }

    @Test
    fun maxChroma_fractionalHueAndTone_answersForTheWholeNumbers() {
        assertEquals(GamutLimit.edge(282.0, 40.0), GamutLimit.maxChroma(282.4, 39.6))
        assertEquals(GamutLimit.edge(0.0, 50.0), GamutLimit.maxChroma(359.7, 50.0))
    }

    @Test
    fun edge_colorsOnTheSrgbEdge_reachesTheirOwnChroma() {
        for (argb in listOf(0xFF00FF00, 0xFF000011, 0xFFFF0000, 0xFF0000FF, 0xFFFFFF00)) {
            val hct = Hct.fromInt(argb.toInt())

            GamutLimit.edge(hct.hue, hct.tone) shouldBeGreaterThan hct.chroma - 0.5
        }
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

/**
 * The most chroma any ask from zero to the ceiling comes back with, in half unit steps.
 */
private fun pickerBruteMaxChroma(
    hue: Double,
    tone: Double,
): Double =
    (0..(ChromaCeiling * 2).toInt())
        .maxOf { step -> Hct.from(hue, step / 2.0, tone).chroma }
        .coerceAtMost(ChromaCeiling)

private const val PickerFarPastSrgb: Double = 200.0
