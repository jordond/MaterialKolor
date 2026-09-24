package com.materialkolor.builder.domain

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.random.Random

/**
 * A generator of whole documents, for the tests that have to hold for any document at all.
 *
 * It is seeded, so a failure reported by the codec or the persistence tests can be reproduced by
 * handing the same seed back. Every field is filled, including the ones that are usually left at
 * their default, because those are exactly the ones a format forgets about.
 */
class DocumentArb(
    seed: Int = DEFAULT_ARB_SEED,
) {
    private val random = Random(seed)

    /**
     * [count] documents in a row, each one different from the last.
     */
    fun documents(count: Int): List<ThemeDocument> = List(count) { nextDocument() }

    /**
     * One document with every field filled at random.
     */
    fun nextDocument(): ThemeDocument =
        ThemeDocument(
            seed = nextArgb(),
            seedSource = nextSeedSource(),
            keyColors = nextKeyColors(),
            style = Style.entries.random(random),
            cmfTertiarySeed = nextArgbOrNull(),
            contrast = nextContrast(),
            spec = SpecVersion.entries.random(random),
            platform = SchemePlatform.entries.random(random),
            amoled = random.nextBoolean(),
            accents = nextAccents(),
            pins = nextPins(),
            library = Library.entries.random(random),
            expressive = random.nextBoolean(),
            motionScheme = MotionSchemeChoice.entries.random(random),
            themeName = "${nextWord()}Theme",
            customTones = nextCustomTones(),
        )

    /** An opaque color. */
    fun nextArgb(): Argb = Argb(random.nextInt())

    /** One of the four contrast levels a document holds (D53). */
    fun nextContrast(): ContrastLevel = ContrastLevel.Stops.random(random)

    private fun nextArgbOrNull(): Argb? = if (random.nextBoolean()) nextArgb() else null

    private fun nextSeedSource(): SeedSource =
        when (random.nextInt(until = 6)) {
            0 -> SeedSource.Typed
            1 -> SeedSource.Picked
            2 -> SeedSource.Eyedropper
            3 -> SeedSource.Shuffled
            4 -> SeedSource.Preset(id = nextWord())
            else -> SeedSource.Image(
                name = "${nextWord()}.png",
                candidates = List(random.nextInt(from = 0, until = 6)) { nextArgb() },
            )
        }

    private fun nextKeyColors(): KeyColors =
        KeyColor.entries.fold(KeyColors()) { colors, slot -> colors.with(slot, nextArgbOrNull()) }

    private fun nextAccents(): List<Accent> = List(random.nextInt(from = 0, until = 4)) { nextAccent() }

    private fun nextAccent(): Accent =
        Accent(
            name = nextWord(),
            seed = nextArgb(),
            harmonize = random.nextBoolean(),
            light = FamilyTones(color = nextTone(), container = nextTone()),
            dark = FamilyTones(color = nextTone(), container = nextTone()),
            threshold = OnColorThreshold.entries.random(random),
        )

    private fun nextPins(): Map<Role, RolePin> =
        Role.entries
            .shuffled(random)
            .take(random.nextInt(from = 0, until = 5))
            .associateWith { role -> nextRolePin() }

    private fun nextRolePin(): RolePin =
        when (random.nextInt(until = 3)) {
            0 -> RolePin(light = nextArgb())
            1 -> RolePin(dark = nextArgb())
            else -> RolePin(light = nextArgb(), dark = nextArgb())
        }

    private fun nextCustomTones(): Map<CustomSlot, CustomTone> =
        CustomSlot.entries
            .shuffled(random)
            .take(random.nextInt(from = 0, until = 5))
            .associateWith { slot -> CustomTone(light = nextToneOrNull(), dark = nextToneOrNull()) }

    private fun nextTone(): Int = random.nextInt(from = 0, until = 101)

    private fun nextToneOrNull(): Int? = if (random.nextBoolean()) nextTone() else null

    private fun nextWord(): String = WORDS.random(random)

    companion object {
        /**
         * The seed the generator runs with when a test does not care which documents it gets.
         */
        const val DEFAULT_ARB_SEED: Int = 20260922

        private val WORDS = listOf("brand", "cactus", "monstera", "snakegrass", "amber", "cobalt", "plum", "sage")
    }
}
