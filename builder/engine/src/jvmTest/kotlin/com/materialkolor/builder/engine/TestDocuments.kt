package com.materialkolor.builder.engine

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.random.Random

/**
 * A seeded generator of documents for the engine tests.
 *
 * The domain module has a fuller one, but its test sources are not visible from here. This one
 * fills only what reaches the engine, the generation inputs plus AMOLED and pins, and hands the
 * same documents back for the same seed so a failure can be replayed.
 */
class TestDocuments(
    seed: Int = 115,
) {
    private val random = Random(seed)

    /** [count] documents in a row. */
    fun documents(count: Int): List<ThemeDocument> = List(count) { next() }

    /** One document with every engine input picked at random. */
    fun next(): ThemeDocument =
        ThemeDocument(
            seed = nextArgb(),
            keyColors = nextKeyColors(),
            style = Style.entries.random(random),
            cmfTertiarySeed = nextArgbOrNull(),
            contrast = ContrastLevel(random.nextInt(-100, 101)),
            spec = SpecVersion.entries.random(random),
            platform = SchemePlatform.entries.random(random),
            amoled = random.nextBoolean(),
            pins = nextPins(),
        )

    private fun nextArgb(): Argb = Argb(random.nextInt())

    private fun nextArgbOrNull(): Argb? = if (random.nextInt(3) == 0) nextArgb() else null

    private fun nextKeyColors(): KeyColors =
        KeyColors(
            primary = nextArgbOrNull(),
            secondary = nextArgbOrNull(),
            tertiary = nextArgbOrNull(),
            error = nextArgbOrNull(),
            neutral = nextArgbOrNull(),
            neutralVariant = nextArgbOrNull(),
        )

    private fun nextPins(): Map<Role, RolePin> =
        Role.entries
            .shuffled(random)
            .take(random.nextInt(0, 4))
            .associateWith { nextPin() }

    private fun nextPin(): RolePin =
        when (random.nextInt(3)) {
            0 -> RolePin(light = nextArgb())
            1 -> RolePin(dark = nextArgb())
            else -> RolePin(light = nextArgb(), dark = nextArgb())
        }
}
