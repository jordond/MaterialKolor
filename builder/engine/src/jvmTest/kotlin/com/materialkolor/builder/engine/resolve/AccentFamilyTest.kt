package com.materialkolor.builder.engine.resolve

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.from
import com.materialkolor.ktx.harmonize
import com.materialkolor.palettes.TonalPalette
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AccentFamilyTest {
    private val themeSeed = Argb(0x6750A4)
    private val brand = Accent(name = "Brand", seed = Argb(0xB3261E))

    @Test
    fun of_defaultTones_cutsEachPartAtItsTone() {
        val family = AccentFamily.of(brand, themeSeed)
        val palette = family.palette

        assertEquals(Argb(palette.tone(40)), family.light.color)
        assertEquals(Argb(palette.tone(90)), family.light.container)
        assertEquals(Argb(palette.tone(80)), family.dark.color)
        assertEquals(Argb(palette.tone(30)), family.dark.container)
    }

    @Test
    fun of_movedTones_cutsAtTheAccentsOwnTones() {
        val accent = brand.copy(
            light = FamilyTones(color = 35, container = 95),
            dark = FamilyTones(color = 70, container = 20),
        )
        val family = AccentFamily.of(accent, themeSeed)

        assertEquals(Argb(family.palette.tone(35)), family.light.color)
        assertEquals(Argb(family.palette.tone(95)), family.light.container)
        assertEquals(Argb(family.palette.tone(70)), family.dark.color)
        assertEquals(Argb(family.palette.tone(20)), family.dark.container)
    }

    @Test
    fun of_everyThreshold_onColorsClearIt() {
        for (threshold in OnColorThreshold.entries) {
            val accent = brand.copy(
                light = FamilyTones(color = 30, container = 95),
                dark = FamilyTones(color = 90, container = 10),
                threshold = threshold,
            )
            val family = AccentFamily.of(accent, themeSeed)

            for (isDark in listOf(false, true)) {
                val colors = family.mode(isDark)
                val onColor = colors.onColor.toColor().contrastRatio(colors.color.toColor())
                val onContainer = colors.onContainer.toColor().contrastRatio(colors.container.toColor())

                assertTrue(onColor >= threshold.ratio, "$threshold onColor, dark $isDark, got $onColor")
                assertTrue(onContainer >= threshold.ratio, "$threshold onContainer, dark $isDark, got $onContainer")
            }
        }
    }

    @Test
    fun of_harmonizeOff_buildsTheRampFromTheRawSeed() {
        val family = AccentFamily.of(brand.copy(harmonize = false), themeSeed)
        val raw = TonalPalette.from(brand.seed.value)

        assertEquals(Argb(raw.tone(40)), family.light.color)
        assertEquals(Argb(raw.tone(30)), family.dark.container)
    }

    @Test
    fun of_harmonizeOn_pullsTheHueTowardTheThemeSeed() {
        val raw = AccentFamily.of(brand.copy(harmonize = false), themeSeed)
        val pulled = AccentFamily.of(brand, themeSeed)
        val expected = TonalPalette.from(brand.seed.toColor().harmonize(themeSeed.toColor()))
        val themeHue = TonalPalette.from(themeSeed.value).hue

        assertEquals(Argb(expected.tone(40)), pulled.light.color)
        assertNotEquals(raw.light.color, pulled.light.color)
        assertTrue(hueDistance(pulled.palette.hue, themeHue) < hueDistance(raw.palette.hue, themeHue))
    }

    @Test
    fun get_accentSlot_readsThatFamilysPart() {
        val green = Accent(name = "Status", seed = Argb(0x2E7D32), harmonize = false)
        val document = ThemeDocument(seed = themeSeed, accents = listOf(brand, green))
        val accents = ThemeResolver().resolve(document).accents
        val second = accents.families[1]

        assertEquals(2, accents.families.size)
        assertEquals(second.light.color, accents[AccentSlot(index = 1, part = AccentPart.Color), false])
        assertEquals(second.light.onColor, accents[AccentSlot(index = 1, part = AccentPart.OnColor), false])
        assertEquals(second.dark.container, accents[AccentSlot(index = 1, part = AccentPart.Container), true])
        assertEquals(second.dark.onContainer, accents[AccentSlot(index = 1, part = AccentPart.OnContainer), true])
        assertEquals(AccentFamily.of(green, themeSeed).light, second.light)
    }

    private fun hueDistance(
        first: Double,
        second: Double,
    ): Double {
        val difference = abs(first - second)
        return min(difference, 360.0 - difference)
    }
}
