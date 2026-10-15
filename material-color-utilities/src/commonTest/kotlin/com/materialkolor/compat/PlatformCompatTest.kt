package com.materialkolor.compat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlatformCompatTest {
    @Test
    fun diagnosticRoundingIsEvenAndPreservesSign() {
        for ((value, text) in listOf(
            0.0 to "0.0",
            -0.0 to "-0.0",
            0.05 to "0.0",
            0.15 to "0.2",
            0.25 to "0.2",
            0.35 to "0.4",
            -0.05 to "-0.0",
            -0.15 to "-0.2",
            0.999 to "1.0",
            -1.0 to "-1.0",
            1e15 to "1000000000000000.0",
            1e20 to "100000000000000000000.0",
        )) {
            assertEquals(text, formatContrast(value), "value=$value")
        }
        assertFailsWith<IllegalArgumentException> { formatContrast(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { formatContrast(Double.POSITIVE_INFINITY) }
    }

    @Test
    fun randomRejectsNonpositiveBounds() {
        assertFailsWith<IllegalArgumentException> { JavaRandom(0).nextInt(0) }
        assertFailsWith<IllegalArgumentException> { JavaRandom(0).nextInt(-1) }
        assertEquals(0, JavaRandom(0).nextInt(1))
    }
}
