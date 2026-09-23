package com.materialkolor.builder.domain.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json

class ContrastLevelTest {
    @Test
    fun contrastLevel_namedStops_carryTheExpectedHundredths() {
        assertEquals(-100, ContrastLevel.Reduced.hundredths)
        assertEquals(0, ContrastLevel.Standard.hundredths)
        assertEquals(50, ContrastLevel.Medium.hundredths)
        assertEquals(100, ContrastLevel.High.hundredths)
    }

    @Test
    fun contrastLevel_toDouble_isTheRangeTheEngineExpects() {
        assertEquals(-1.0, ContrastLevel.Reduced.toDouble())
        assertEquals(0.0, ContrastLevel.Standard.toDouble())
        assertEquals(0.5, ContrastLevel.Medium.toDouble())
        assertEquals(1.0, ContrastLevel.High.toDouble())
    }

    @Test
    fun contrastLevel_valueAboveTheRange_isRejected() {
        assertFailsWith<IllegalArgumentException> { ContrastLevel(101) }
    }

    @Test
    fun contrastLevel_valueBelowTheRange_isRejected() {
        assertFailsWith<IllegalArgumentException> { ContrastLevel(-101) }
    }

    @Test
    fun contrastLevel_everyStepInRange_survivesAJsonRoundTrip() {
        val json = Json

        (-100..100).forEach { hundredths ->
            val level = ContrastLevel(hundredths)
            val text = json.encodeToString(ContrastLevelSerializer, level)
            assertEquals(level, json.decodeFromString(ContrastLevelSerializer, text), text)
        }
    }

    @Test
    fun contrastLevel_decodingAValueOutOfRange_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(ContrastLevelSerializer, "500")
        }
    }
}
