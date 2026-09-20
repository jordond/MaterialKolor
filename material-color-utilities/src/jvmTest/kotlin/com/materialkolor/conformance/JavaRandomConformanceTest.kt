package com.materialkolor.conformance

import com.materialkolor.compat.JavaRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JavaRandomConformanceTest {
    @Test
    fun boundedSequencesMatchJavaIncludingRejectionSamplingAndSignedSeeds() {
        for (seed in listOf(0L, 0x42688L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)) {
            for (bound in listOf(1, 2, 3, 16, 128, 255, 1000, 1073741825, Int.MAX_VALUE)) {
                val expected = java.util.Random(seed)
                val actual = JavaRandom(seed)
                repeat(1000) { iteration ->
                    assertEquals(
                        expected.nextInt(bound),
                        actual.nextInt(bound),
                        "seed=$seed bound=$bound iteration=$iteration",
                    )
                }
            }
        }
    }

    @Test
    fun invalidBoundsFailWithoutAdvancingTheSequence() {
        val expected = java.util.Random(0x42688L)
        val actual = JavaRandom(0x42688L)
        for (bound in listOf(0, -1, Int.MIN_VALUE)) {
            assertFailsWith<IllegalArgumentException> { expected.nextInt(bound) }
            assertFailsWith<IllegalArgumentException> { actual.nextInt(bound) }
        }
        assertEquals(expected.nextInt(Int.MAX_VALUE), actual.nextInt(Int.MAX_VALUE))
    }
}
