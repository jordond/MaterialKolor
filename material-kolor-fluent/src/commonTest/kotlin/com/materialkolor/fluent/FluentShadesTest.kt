package com.materialkolor.fluent

import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.from
import com.materialkolor.ktx.toHct
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import io.github.composefluent.Shades
import io.github.composefluent.generateShades
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FluentShadesTest {
    private val windowsBlue = Color(0xFF0078D4)

    /** Microsoft's own Windows blue family, the ramp the tone choices are anchored to. */
    private val microsoft = mapOf(
        "dark3" to Color(0xFF001968),
        "dark2" to Color(0xFF003D92),
        "dark1" to Color(0xFF005EB7),
        "base" to Color(0xFF0078D4),
        "light1" to Color(0xFF0093F9),
        "light2" to Color(0xFF60CCFE),
        "light3" to Color(0xFF98ECFE),
    )

    private val seeds = listOf(
        Color(0xFF0078D4),
        Color(0xFF6750A4),
        Color(0xFFB3261E),
        Color(0xFFFFEB3B),
        Color(0xFF2E7D32),
        Color(0xFF00BCD4),
        Color(0xFFE91E63),
        Color(0xFF7A7A7E),
        Color(0xFF808080),
    )

    @Test
    fun toFluentShades_takesTheTonesThisAdapterFixed() {
        val palette = TonalPalette.from(windowsBlue)
        val shades = palette.toFluentShades()

        assertEquals(palette.toneColor(15), shades.dark3)
        assertEquals(palette.toneColor(30), shades.dark2)
        assertEquals(palette.toneColor(40), shades.dark1)
        assertEquals(palette.toneColor(50), shades.base)
        assertEquals(palette.toneColor(60), shades.light1)
        assertEquals(palette.toneColor(80), shades.light2)
        assertEquals(palette.toneColor(90), shades.light3)
    }

    @Test
    fun toFluentShades_landsNearMicrosoftsOwnRampInLightness() {
        val shades = TonalPalette.from(windowsBlue).toFluentShades()

        // Lightness only. Microsoft's ramp shifts hue across the seven shades and a tonal palette
        // does not, so the hues are not expected to line up and the tones are.
        for ((name, expected) in microsoft) {
            val actual = shades.named(name)
            val drift = abs(actual.toHct().tone - expected.toHct().tone)
            assertTrue(
                actual = drift <= 2.5,
                message = "$name drifted $drift tones from ${expected.toHct().tone}",
            )
        }
    }

    @Test
    fun toFluentShades_staysInOrderForEverySeed() {
        for (seed in seeds) {
            val shades = TonalPalette.from(seed).toFluentShades()
            val ramp = listOf(
                "dark3" to shades.dark3,
                "dark2" to shades.dark2,
                "dark1" to shades.dark1,
                "base" to shades.base,
                "light1" to shades.light1,
                "light2" to shades.light2,
                "light3" to shades.light3,
            )

            for ((darker, lighter) in ramp.zipWithNext()) {
                assertTrue(
                    actual = darker.second.toHct().tone < lighter.second.toHct().tone,
                    message = "seed=$seed ${darker.first} was not darker than ${lighter.first}",
                )
            }
        }
    }

    @Test
    fun toFluentShades_reachesTheRequestedToneForEverySeed() {
        val tones = mapOf(
            "dark3" to 15.0,
            "dark2" to 30.0,
            "dark1" to 40.0,
            "base" to 50.0,
            "light1" to 60.0,
            "light2" to 80.0,
            "light3" to 90.0,
        )

        for (seed in seeds) {
            val shades = TonalPalette.from(seed).toFluentShades()
            for ((name, tone) in tones) {
                val drift = abs(shades.named(name).toHct().tone - tone)
                assertTrue(
                    actual = drift < 0.5,
                    message = "seed=$seed $name asked for tone $tone and drifted $drift",
                )
            }
        }
    }

    @Test
    fun toFluentShades_ignoresTheOneAccentFluentKnows() {
        // The whole point of the adapter. Fluent's own lookup has one entry, so every seed that is
        // not exactly Windows blue comes back as Windows blue without us.
        val seed = Color(0xFFE91E63)

        assertEquals(generateShades(windowsBlue), generateShades(seed))
        assertNotEquals(generateShades(seed), TonalPalette.from(seed).toFluentShades())
    }

    @Test
    fun toFluentColors_readsTheSchemesOwnDarkFlag() {
        for (isDark in listOf(true, false)) {
            val scheme = DynamicScheme(seedColor = windowsBlue, isDark = isDark)
            val colors = scheme.toFluentColors()

            assertEquals(isDark, colors.darkMode)
            assertEquals(scheme.primaryPalette.toFluentShades(), colors.shades)
        }
    }

    @Test
    fun toFluentColors_usesTheSchemesRamp_notTheSeedsOwn() {
        // Worth pinning because both are reasonable and they are not the same. TonalSpot pulls
        // chroma towards its own target, so a vivid seed gives a calmer Fluent accent through the
        // scheme than it does through TonalPalette.from.
        val seed = Color(0xFFE91E63)
        val scheme = DynamicScheme(seedColor = seed, isDark = false)

        assertEquals(scheme.primaryPalette.toFluentShades(), scheme.toFluentColors().shades)
        assertNotEquals(TonalPalette.from(seed).toFluentShades(), scheme.toFluentColors().shades)
    }

    private fun Shades.named(name: String): Color =
        when (name) {
            "dark3" -> dark3
            "dark2" -> dark2
            "dark1" -> dark1
            "base" -> base
            "light1" -> light1
            "light2" -> light2
            "light3" -> light3
            else -> error("unknown shade $name")
        }
}
