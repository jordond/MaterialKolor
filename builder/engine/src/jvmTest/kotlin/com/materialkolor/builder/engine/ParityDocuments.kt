package com.materialkolor.builder.engine

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.random.Random

/**
 * The documents the export parity gate runs on, replayable from their seed.
 *
 * Each one starts from [TestDocuments], which already picks key colors, contrast, platform, AMOLED
 * and pins, then adds what only an export reads, accents and custom tones. Style and spec are
 * walked in turn rather than drawn, so thirty documents or more hold every pair of the two.
 */
class ParityDocuments(
    seed: Int = 117,
) {
    private val random = Random(seed)

    // Seeded apart from random, so the two streams never replay each other at shifted positions.
    private val base = TestDocuments(seed = Random(seed xor BASE_SALT).nextInt())

    /**
     * [count] documents in a row.
     */
    fun documents(count: Int): List<ThemeDocument> = List(count) { index -> next(index) }

    /**
     * [count] documents whose only key color is a primary override, with no pins, AMOLED or custom
     * tones, so every adapter should give back the core scheme for the seed and that override.
     */
    fun primaryOverrides(count: Int): List<ThemeDocument> =
        documents(count).map { document ->
            document.copy(
                keyColors = KeyColors(primary = Argb(random.nextInt())),
                amoled = false,
                pins = emptyMap(),
                customTones = emptyMap(),
            )
        }

    private fun next(index: Int): ThemeDocument =
        base.next().copy(
            style = Style.entries[index % Style.entries.size],
            spec = SpecVersion.entries[index / Style.entries.size % SpecVersion.entries.size],
            accents = List(random.nextInt(0, 4)) { position -> nextAccent(position) },
            customTones = nextCustomTones(),
        )

    private fun nextAccent(position: Int): Accent =
        Accent(
            name = "accent$position",
            seed = Argb(random.nextInt()),
            harmonize = random.nextBoolean(),
            light = FamilyTones(color = nextTone(), container = nextTone()),
            dark = FamilyTones(color = nextTone(), container = nextTone()),
            threshold = OnColorThreshold.entries.random(random),
        )

    private fun nextCustomTones(): Map<CustomSlot, CustomTone> =
        CustomSlot.entries
            .shuffled(random)
            .take(random.nextInt(0, 5))
            .associateWith { nextCustomTone() }

    private fun nextCustomTone(): CustomTone =
        when (random.nextInt(3)) {
            0 -> CustomTone(light = nextTone())
            1 -> CustomTone(dark = nextTone())
            else -> CustomTone(light = nextTone(), dark = nextTone())
        }

    private fun nextTone(): Int = random.nextInt(0, 101)
}

private const val BASE_SALT = 0x5EED
