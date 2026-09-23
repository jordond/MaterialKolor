package com.materialkolor.builder.engine.shuffle

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.engine.TestDocuments
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShuffleTest {
    private val resolver = ThemeResolver()

    @Test
    fun next_thousandSeededShuffles_noRepeatNoDislikeAndPrimaryReads() {
        val random = Random(117)
        var document = ThemeDocument.Default
        repeat(1_000) { index ->
            // Every other shuffle frees the style, so each style is exercised along the way.
            val locks = ShuffleLocks(style = index % 2 == 0)
            val result = shuffled(random, document, locks)

            assertNotEquals(document.seed, result.seed, "shuffle $index repeated ${document.seed}")
            assertFalse(isDisliked(result.seed), "shuffle $index gave the disliked ${result.seed}")
            document = result.applyTo(document)
            assertPrimaryReads(document, "shuffle $index to ${result.seed} in ${document.style}")
        }
    }

    @Test
    fun next_sameRandomSeed_sameResults() {
        val locks = ShuffleLocks(style = false)

        fun run() = List(20) { index -> Shuffle.next(Random(index), ThemeDocument.Default, locks) }

        assertEquals(run(), run())
    }

    @Test
    fun next_hueLocked_keepsHueAndVariesChromaAndTone() {
        val document = ThemeDocument(seed = Argb(0x2F6FD8))
        val hue = Hct.fromInt(document.seed.value).hue
        val random = Random(1)
        val found = List(100) { Hct.fromInt(shuffled(random, document, ShuffleLocks(hue = true)).seed.value) }

        found.forEach { hct ->
            assertTrue(hueDistance(hue, hct.hue) < HUE_TOLERANCE, "${Argb(hct.toInt())} moved off hue $hue")
        }
        assertTrue(found.map { hct -> hct.tone.toInt() }.toSet().size > 10, "tone never varied")
        assertTrue(found.map { hct -> hct.chroma.toInt() }.toSet().size > 10, "chroma never varied")
    }

    @Test
    fun next_hueUnlocked_spreadsAcrossTheHueCircle() {
        val random = Random(2)
        val sextants = List(200) { shuffled(random, ThemeDocument.Default, ShuffleLocks()).seed }
            .map { seed -> (Hct.fromInt(seed.value).hue / 60).toInt() }
            .toSet()

        assertEquals(6, sextants.size)
    }

    @Test
    fun next_styleLocked_keepsTheStyle() {
        val document = ThemeDocument.Default.copy(style = Style.Vibrant)
        val result = shuffled(Random(3), document, ShuffleLocks(style = true))

        assertNull(result.style)
        assertEquals(Style.Vibrant, result.applyTo(document).style)
    }

    @Test
    fun next_styleUnlocked_picksEveryOtherStyleTheTargetOffers() {
        Library.entries.forEach { library ->
            val document = ThemeDocument.Default.copy(library = library, style = Style.Content)
            val random = Random(4)
            val picked = List(150) { shuffled(random, document, ShuffleLocks(style = false)).style }

            assertEquals(Shuffle.pickableStyles(document).toSet(), picked.toSet(), "styles picked for $library")
            assertFalse(Style.Content in picked, "the current style came back for $library")
        }
    }

    @Test
    fun next_styleUnlocked_neverPicksMonochrome() {
        Library.entries.forEach { library ->
            listOf(Style.Monochrome, Style.Content).forEach { current ->
                val document = ThemeDocument.Default.copy(library = library, style = current)
                val random = Random(8)
                val picked = List(150) { shuffled(random, document, ShuffleLocks(style = false)).style }

                assertFalse(Style.Monochrome in picked, "Monochrome was picked for $library from $current")
            }
        }
    }

    @Test
    fun next_hueLockedOnADislikedHue_liftsADarkDrawToTheFixedTone() {
        val document = ThemeDocument.Default.copy(seed = Argb(Hct.from(DISLIKED_HUE, 40.0, 50.0).toInt()))
        // Every draw asks for the lowest tone, which is disliked on this hue until it is fixed.
        val result = shuffled(ConstantRandom, document, ShuffleLocks(hue = true))
        val found = Hct.fromInt(result.seed.value)

        assertTrue(abs(found.tone - FIXED_TONE) < TONE_TOLERANCE, "${result.seed} sits at tone ${found.tone}")
        assertTrue(hueDistance(DISLIKED_HUE, found.hue) < HUE_TOLERANCE, "${result.seed} moved off the locked hue")
    }

    @Test
    fun next_seedLocked_keepsTheSeedAndItsSource() {
        val document = ThemeDocument.Default.copy(seedSource = SeedSource.Preset(id = "sunset"))
        val result = shuffled(Random(5), document, ShuffleLocks(style = false, seed = true))
        val applied = result.applyTo(document)

        assertEquals(document.seed, result.seed)
        assertNotNull(result.style)
        assertEquals(document.seedSource, applied.seedSource)
        assertNotEquals(document.style, applied.style)
    }

    @Test
    fun next_seedAndStyleLocked_nothingToShuffle() {
        val result = Shuffle.next(Random(6), ThemeDocument.Default, ShuffleLocks(style = true, seed = true))

        assertEquals(ShuffleResult.NothingToShuffle, result)
    }

    @Test
    fun next_everyDrawRepeatsTheSeed_fallsBackToAFreshSeed() {
        val repeated = shuffled(ConstantRandom, ThemeDocument.Default, ShuffleLocks()).seed
        val document = ThemeDocument.Default.copy(seed = repeated)
        val result = shuffled(ConstantRandom, document, ShuffleLocks())

        assertNotEquals(repeated, result.seed)
        assertFalse(isDisliked(result.seed))
        assertPrimaryReads(result.applyTo(document), "fallback ${result.seed}")
        assertEquals(result, shuffled(ConstantRandom, document, ShuffleLocks()))
    }

    @Test
    fun applyTo_busyDocuments_movesOnlySeedSourceAndStyle() {
        val random = Random(7)
        TestDocuments(seed = 117).documents(50).forEach { document ->
            val applied = shuffled(random, document, ShuffleLocks(style = false)).applyTo(document)

            assertEquals(SeedSource.Shuffled, applied.seedSource)
            assertEquals(
                document,
                applied.copy(seed = document.seed, seedSource = document.seedSource, style = document.style),
            )
        }
    }

    @Test
    fun shuffleLocks_preferences_readsEachLock() {
        assertEquals(ShuffleLocks(), Preferences().shuffleLocks())
        assertEquals(
            ShuffleLocks(hue = true, style = false, seed = true),
            Preferences(hueLock = true, styleLock = false, seedLock = true).shuffleLocks(),
        )
    }

    private fun shuffled(
        random: Random,
        document: ThemeDocument,
        locks: ShuffleLocks,
    ): ShuffleResult.Shuffled = assertIs<ShuffleResult.Shuffled>(Shuffle.next(random, document, locks, resolver))

    private fun hueDistance(
        first: Double,
        second: Double,
    ): Double = 180.0 - abs(abs(first - second) - 180.0)

    private fun isDisliked(seed: Argb): Boolean = DislikeAnalyzer.isDisliked(Hct.fromInt(seed.value))

    private fun assertPrimaryReads(
        document: ThemeDocument,
        message: String,
    ) {
        val roles = resolver.resolve(document.copy(contrast = ContrastLevel.Standard)).roles
        listOf(false, true).forEach { isDark ->
            val primary = roles[Role.Primary, isDark].argb.toColor()
            val onPrimary = roles[Role.OnPrimary, isDark].argb.toColor()
            val ratio = primary.contrastRatio(onPrimary)
            assertTrue(ratio >= Shuffle.MIN_PRIMARY_CONTRAST, "$message dark=$isDark reads at $ratio")
        }
    }

    /** Hands back the same draw every time, so every draw lands on the same seed. */
    private object ConstantRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private companion object {
        /** How far rounding to ARGB can move a hue at the lowest chroma a shuffle draws. */
        const val HUE_TOLERANCE = 2.0

        /** A hue inside the yellow green band [DislikeAnalyzer] dislikes at darker tones. */
        const val DISLIKED_HUE = 100.0

        /** The tone [DislikeAnalyzer] lifts a disliked color to. */
        const val FIXED_TONE = 70.0

        /** How far rounding to ARGB can move a tone. */
        const val TONE_TOLERANCE = 1.0
    }
}
