package com.materialkolor.builder.engine.image

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeedExtractorTest {
    @Test
    fun extract_twoColorImage_ranksTheLargerShareFirst() {
        val seeds = SeedExtractor.extract(sample(BLUE to 7_000, GREEN to 3_000))

        assertEquals(2, seeds.candidates.size)
        assertNear(BLUE, seeds.candidates[0])
        assertNear(GREEN, seeds.candidates[1])
        assertFalse(seeds.mostlyGray)
    }

    @Test
    fun extract_twoColorImageSwapped_ranksTheOtherColorFirst() {
        val seeds = SeedExtractor.extract(sample(BLUE to 3_000, GREEN to 7_000))

        assertNear(GREEN, seeds.candidates[0])
        assertNear(BLUE, seeds.candidates[1])
    }

    @Test
    fun extract_grayImage_saysSoAndOffersItsOwnGray() {
        val grays = sample(0x7A7A7A to 4_000, 0x8C8C8C to 3_000, 0x666666 to 3_000)
        val seeds = SeedExtractor.extract(grays, fallback = FALLBACK)

        assertTrue(seeds.mostlyGray)
        assertTrue(seeds.candidates.isNotEmpty())
        assertFalse(FALLBACK in seeds.candidates, "a gray image fell back to the fallback")
        seeds.candidates.forEach { candidate ->
            assertTrue(Hct.fromInt(candidate.value).chroma < 10.0, "$candidate is not one of the image's grays")
        }
    }

    @Test
    fun extract_mostlyGrayImageWithAPatchOfColor_saysSoAndOffersThePatch() {
        val seeds = SeedExtractor.extract(sample(0x7A7A7A to 5_000, 0x8C8C8C to 4_200, BLUE to 800))

        assertTrue(seeds.mostlyGray)
        assertNear(BLUE, seeds.candidates.first())
    }

    @Test
    fun extract_blackAndWhiteImage_offersTheFallback() {
        val seeds = SeedExtractor.extract(sample(0x000000 to 5_000, 0xFFFFFF to 5_000), fallback = FALLBACK)

        assertEquals(listOf(FALLBACK), seeds.candidates)
        assertTrue(seeds.mostlyGray)
    }

    @Test
    fun extract_manyHues_offersAtMostFiveCandidates() {
        val hues = List(10) { index -> (Hct.from(index * 36.0, 60.0, 55.0).toInt() and 0xFFFFFF) to 1_000 }
        val seeds = SeedExtractor.extract(sample(*hues.toTypedArray()))

        assertEquals(SeedExtractor.MAX_CANDIDATES, seeds.candidates.size)
        assertEquals(seeds.candidates.size, seeds.candidates.toSet().size)
        assertFalse(seeds.mostlyGray)
    }

    @Test
    fun extract_samePixels_sameSeeds() {
        val pixels = arrayOf(BLUE to 6_000, GREEN to 2_500, 0x7A7A7A to 1_500)

        assertEquals(SeedExtractor.extract(sample(*pixels)), SeedExtractor.extract(sample(*pixels)))
    }

    @Test
    fun pixelSample_sizeDisagreesWithPixels_throws() {
        assertFailsWith<IllegalArgumentException> { PixelSample(IntArray(5), width = 2, height = 2) }
        assertFailsWith<IllegalArgumentException> { PixelSample(IntArray(0), width = 0, height = 0) }
    }

    /**
     * A 100 by 100 sample filled with each RGB color for its count of pixels, in order. The counts
     * have to add up to the 10,000 pixels, which stays under kmpalette's resize area so it
     * quantizes every pixel.
     */
    private fun sample(vararg fills: Pair<Int, Int>): PixelSample {
        val pixels = fills.flatMap { (rgb, count) -> List(count) { OPAQUE or rgb } }.toIntArray()
        return PixelSample(pixels, width = SIDE, height = SIDE)
    }

    /** kmpalette quantizes to five bits a channel, so a swatch sits a few steps off the color it came from. */
    private fun assertNear(
        expected: Int,
        actual: Argb,
    ) {
        val wanted = Argb(expected)
        val off = maxOf(
            abs(wanted.red - actual.red),
            abs(wanted.green - actual.green),
            abs(wanted.blue - actual.blue),
        )
        assertTrue(off <= QUANTIZE_STEP, "expected about $wanted, got $actual")
    }

    private companion object {
        const val SIDE = 100
        const val OPAQUE = 0xFF shl 24
        const val QUANTIZE_STEP = 8
        const val BLUE = 0x1E5BD8
        const val GREEN = 0x2E9E4A
        val FALLBACK = Argb(0x6750A4)
    }
}
