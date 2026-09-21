package com.materialkolor.compat

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JavaRandomTest {
    @Test
    fun matchesJavaSequencesAcrossSeedsAndBounds() {
        val seeds = longArrayOf(
            0L,
            1L,
            -1L,
            42L,
            0x42688L,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            0x5DEECE66DL,
        ) + LongArray(56) { (it + 1L) * -0x61c8864680b583ebL }

        val bounds = intArrayOf(
            1,
            2,
            3,
            7,
            16,
            100,
            128,
            255,
            256,
            1000,
            65536,
            1 shl 30,
            (1 shl 30) + 1,
            Int.MAX_VALUE,
        )

        for (seed in seeds) {
            for (bound in bounds) {
                val expected = Random(seed)
                val actual = JavaRandom(seed)
                repeat(64) { draw ->
                    assertEquals(
                        expected.nextInt(bound),
                        actual.nextInt(bound),
                        "seed=$seed bound=$bound draw=$draw",
                    )
                }
            }
        }
    }

    @Test
    fun interleavedBoundsStayInSequence() {
        val bounds = intArrayOf(1, 2, 7, 128, 1000, Int.MAX_VALUE, 3, 1 shl 20)
        val expected = Random(-987654321L)
        val actual = JavaRandom(-987654321L)
        repeat(2048) { draw ->
            val bound = bounds[draw % bounds.size]
            assertEquals(expected.nextInt(bound), actual.nextInt(bound), "bound=$bound draw=$draw")
        }
    }

    @Test
    fun rejectsNonPositiveBounds() {
        assertFailsWith<IllegalArgumentException> { JavaRandom(1L).nextInt(0) }
        assertFailsWith<IllegalArgumentException> { JavaRandom(1L).nextInt(-5) }
    }
}
