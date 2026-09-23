package com.materialkolor.builder.engine.poster

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.from
import com.materialkolor.palettes.TonalPalette
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PosterColorsTest {
    @Test
    fun of_thousandRandomSeeds_inkReadsOnTheExactSeed() {
        val random = Random(116)
        repeat(1_000) {
            val seed = Argb(random.nextInt())
            val poster = PosterColors.of(seed)

            assertAtLeast(4.5, poster.ink, seed, "ink on $seed")
            assertAtLeast(3.0, poster.inkMuted, seed, "muted ink on $seed")
            assertAtLeast(3.0, poster.outline, seed, "outline on $seed")
            assertAtLeast(4.5, poster.ink, poster.raised, "ink on raised for $seed")
            assertAtLeast(4.5, poster.ink, poster.sunken, "ink on sunken for $seed")
        }
    }

    @Test
    fun of_defaultSeed_inkIsToneTen() {
        val poster = PosterColors.of(Argb(0xD9653B))

        assertEquals(Argb(0x390C00), poster.ink)
        assertEquals(10.0, Hct.fromInt(poster.ink.value).tone, absoluteTolerance = 0.5)
        assertTrue(poster.isLight)
    }

    @Test
    fun of_darkSeed_inkIsLighterThanThePage() {
        val seed = Argb(0x1B1B3A)
        val poster = PosterColors.of(seed)

        assertFalse(poster.isLight)
        assertTrue(Hct.fromInt(poster.ink.value).tone > Hct.fromInt(seed.value).tone)
    }

    @Test
    fun of_seed_paintsThePageInTheExactSeedAndCutsEverythingFromItsRamp() {
        val seed = Argb(0x3F7A5C)
        val poster = PosterColors.of(seed)
        val ramp = TonalPalette.from(seed.value)

        assertEquals(seed, poster.background)
        assertEquals(ramp.tone(40), poster.ramp.tone(40))
        assertEquals(ramp.tone(90), poster.ramp.tone(90))
    }

    private fun assertAtLeast(
        ratio: Double,
        foreground: Argb,
        background: Argb,
        message: String,
    ) {
        val actual = foreground.toColor().contrastRatio(background.toColor())
        assertTrue(actual >= ratio, "$message, got $actual")
    }
}
