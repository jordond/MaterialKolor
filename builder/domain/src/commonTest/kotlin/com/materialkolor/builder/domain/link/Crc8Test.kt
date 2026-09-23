package com.materialkolor.builder.domain.link

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Crc8Test {
    @Test
    fun compute_standardCheckInput_matchesThePublishedValue() {
        assertEquals(0xF4, Crc8.compute("123456789".encodeToByteArray()))
    }

    @Test
    fun compute_nothing_isZero() {
        assertEquals(0, Crc8.compute(ByteArray(0)))
    }

    @Test
    fun compute_range_onlyReadsInsideIt() {
        val bytes = "xx123456789yy".encodeToByteArray()
        assertEquals(0xF4, Crc8.compute(bytes, start = 2, end = 11))
    }

    @Test
    fun compute_anySingleBitFlip_changesTheChecksum() {
        val random = Random(20260922)
        repeat(200) {
            val bytes = random.nextBytes(random.nextInt(from = 1, until = 64))
            val checksum = Crc8.compute(bytes)
            for (index in bytes.indices) {
                for (bit in 0 until 8) {
                    val flipped = bytes.copyOf()
                    flipped[index] = (flipped[index].toInt() xor (1 shl bit)).toByte()
                    assertNotEquals(checksum, Crc8.compute(flipped))
                }
            }
        }
    }
}
