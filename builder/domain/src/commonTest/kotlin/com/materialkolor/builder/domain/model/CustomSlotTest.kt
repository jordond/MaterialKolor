package com.materialkolor.builder.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CustomSlotTest {
    /**
     * Every slot of the custom theme sample, in the order it declares them.
     */
    private val sampleSlots = listOf(
        "primary",
        "onPrimary",
        "primaryContainer",
        "onPrimaryContainer",
        "primaryPressed",
        "primaryRaised",
        "secondary",
        "onSecondary",
        "secondaryContainer",
        "onSecondaryContainer",
        "tertiary",
        "onTertiary",
        "tertiaryContainer",
        "onTertiaryContainer",
        "error",
        "onError",
        "errorContainer",
        "onErrorContainer",
        "love",
        "onLove",
        "loveContainer",
        "onLoveContainer",
        "cold",
        "onCold",
        "coldContainer",
        "onColdContainer",
        "warm",
        "onWarm",
        "warmContainer",
        "onWarmContainer",
        "surface",
        "surfaceRaised",
        "surfaceSunken",
        "surfaceInverse",
        "onSurface",
        "onSurfaceInverse",
        "textStrong",
        "textMuted",
        "borderFaint",
        "borderSoft",
        "borderStrong",
        "drinkCoffee",
        "drinkMatcha",
        "drinkIced",
        "drinkTea",
        "drinkChoc",
        "scrim",
        "focusRing",
        "shadow",
    )

    @Test
    fun customSlot_entries_areTheSlotsOfTheCustomTarget() {
        val names = CustomSlot.entries.map { slot -> slot.name.replaceFirstChar { first -> first.lowercaseChar() } }

        assertEquals(sampleSlots, names)
    }

    @Test
    fun customSlot_codes_matchTheDeclarationOrderTheyWereGivenIn() {
        assertEquals(CustomSlot.entries.indices.toList(), CustomSlot.entries.map { slot -> slot.code })
    }

    @Test
    fun customSlot_rampTones_stayInsideTheTonalRange() {
        CustomSlot.entries.forEach { slot ->
            val tones = when (val resolution = slot.resolution) {
                is SlotResolution.FromRole -> emptyList()
                is SlotResolution.FromRamp -> listOf(resolution.light, resolution.dark)
                is SlotResolution.OnRamp -> listOf(resolution.light, resolution.dark)
            }

            tones.forEach { tone ->
                assertTrue(tone in 0..100, "${slot.name} cuts at tone $tone")
            }
        }
    }

    @Test
    fun customTone_toneOutsideTheTonalRange_isRejected() {
        assertFailsWith<IllegalArgumentException> { CustomTone(light = 101) }
        assertFailsWith<IllegalArgumentException> { CustomTone(dark = -1) }
    }

    @Test
    fun customTone_neitherModeMoved_isAllowed() {
        assertEquals(CustomTone(), CustomTone(light = null, dark = null))
    }
}
