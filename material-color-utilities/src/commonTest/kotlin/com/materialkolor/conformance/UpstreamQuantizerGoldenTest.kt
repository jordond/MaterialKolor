package com.materialkolor.conformance

import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlin.test.Test
import kotlin.test.assertEquals

class UpstreamQuantizerGoldenTest {
    @Test
    fun seededClusteringMatchesRawOrderedArgbAndPopulationFixtures() {
        val colors = QuantizerCelebi.quantize(quantizerGoldenPixels, 8)
        assertEquals(quantizerGoldenEntries, colors.toList(), "Celebi ordered ARGB/population entries")
        assertEquals(96, colors.values.sum(), "total population")
        assertEquals(quantizerGoldenScores, Score.score(colors), "ordered score output")
    }
}
