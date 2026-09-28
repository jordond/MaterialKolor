package com.materialkolor.builder.domain.color

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun contrastLevel_nearest_snapsToTheClosestNamedLevelWithTiesTowardStandard() {
        val cases =
            mapOf(
                0.25 to ContrastLevel.Standard,
                0.75 to ContrastLevel.Medium,
                -0.5 to ContrastLevel.Standard,
                0.2 to ContrastLevel.Standard,
                0.3 to ContrastLevel.Medium,
                -0.51 to ContrastLevel.Reduced,
                0.76 to ContrastLevel.High,
                1.5 to ContrastLevel.High,
                -3.0 to ContrastLevel.Reduced,
            )

        cases.forEach { (value, level) -> assertEquals(level, ContrastLevel.nearest(value), "$value") }
        assertEquals(ContrastLevel.Standard, ContrastLevel(-37).snapped())
        ContrastLevel.Stops.forEach { level -> assertEquals(level, level.snapped()) }
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
