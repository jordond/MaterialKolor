package com.materialkolor.builder.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomSlotTest {
    /**
     * Every slot of the custom target, in the order the enum declares them.
     */
    private val slotNames = listOf(
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
        "scrim",
        "focusRing",
        "shadow",
    )

    /**
     * The code the share codec writes for every slot.
     *
     * Written out rather than read off the declaration order. Adding a slot in the middle of the
     * list is then a one line change here instead of a silent renumbering of every shared link.
     */
    private val slotCodes = mapOf(
        "primary" to 0,
        "onPrimary" to 1,
        "primaryContainer" to 2,
        "onPrimaryContainer" to 3,
        "primaryPressed" to 4,
        "primaryRaised" to 5,
        "secondary" to 6,
        "onSecondary" to 7,
        "secondaryContainer" to 8,
        "onSecondaryContainer" to 9,
        "tertiary" to 10,
        "onTertiary" to 11,
        "tertiaryContainer" to 12,
        "onTertiaryContainer" to 13,
        "error" to 14,
        "onError" to 15,
        "errorContainer" to 16,
        "onErrorContainer" to 17,
        "surface" to 30,
        "surfaceRaised" to 31,
        "surfaceSunken" to 32,
        "surfaceInverse" to 33,
        "onSurface" to 34,
        "onSurfaceInverse" to 35,
        "textStrong" to 36,
        "textMuted" to 37,
        "borderFaint" to 38,
        "borderSoft" to 39,
        "borderStrong" to 40,
        "scrim" to 46,
        "focusRing" to 47,
        "shadow" to 48,
    )

    /**
     * Where every slot gets its color before an override is applied.
     *
     * Checked by hand, one slot at a time. B-115 builds its export against these tones, so a tone
     * that drifts here drifts in every custom theme anyone exports.
     */
    private val slotResolutions = mapOf(
        CustomSlot.Primary to SlotResolution.FromRole(Role.Primary),
        CustomSlot.OnPrimary to SlotResolution.FromRole(Role.OnPrimary),
        CustomSlot.PrimaryContainer to SlotResolution.FromRole(Role.PrimaryContainer),
        CustomSlot.OnPrimaryContainer to SlotResolution.FromRole(Role.OnPrimaryContainer),
        CustomSlot.PrimaryPressed to SlotResolution.FromRamp(TonalRamp.Primary, light = 32, dark = 70),
        CustomSlot.PrimaryRaised to SlotResolution.FromRamp(TonalRamp.Primary, light = 46, dark = 88),
        CustomSlot.Secondary to SlotResolution.FromRole(Role.Secondary),
        CustomSlot.OnSecondary to SlotResolution.FromRole(Role.OnSecondary),
        CustomSlot.SecondaryContainer to SlotResolution.FromRole(Role.SecondaryContainer),
        CustomSlot.OnSecondaryContainer to SlotResolution.FromRole(Role.OnSecondaryContainer),
        CustomSlot.Tertiary to SlotResolution.FromRole(Role.Tertiary),
        CustomSlot.OnTertiary to SlotResolution.FromRole(Role.OnTertiary),
        CustomSlot.TertiaryContainer to SlotResolution.FromRole(Role.TertiaryContainer),
        CustomSlot.OnTertiaryContainer to SlotResolution.FromRole(Role.OnTertiaryContainer),
        CustomSlot.Error to SlotResolution.FromRole(Role.Error),
        CustomSlot.OnError to SlotResolution.FromRole(Role.OnError),
        CustomSlot.ErrorContainer to SlotResolution.FromRole(Role.ErrorContainer),
        CustomSlot.OnErrorContainer to SlotResolution.FromRole(Role.OnErrorContainer),
        CustomSlot.Surface to SlotResolution.FromRole(Role.Surface),
        CustomSlot.SurfaceRaised to SlotResolution.FromRamp(TonalRamp.Neutral, light = 100, dark = 12),
        CustomSlot.SurfaceSunken to SlotResolution.FromRamp(TonalRamp.Neutral, light = 94, dark = 4),
        CustomSlot.SurfaceInverse to SlotResolution.FromRole(Role.InverseSurface),
        CustomSlot.OnSurface to SlotResolution.FromRole(Role.OnSurface),
        CustomSlot.OnSurfaceInverse to SlotResolution.FromRole(Role.InverseOnSurface),
        CustomSlot.TextStrong to SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 10, dark = 90),
        CustomSlot.TextMuted to SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 40, dark = 70),
        CustomSlot.BorderFaint to SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 92, dark = 22),
        CustomSlot.BorderSoft to SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 85, dark = 32),
        CustomSlot.BorderStrong to SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 55, dark = 65),
        CustomSlot.Scrim to SlotResolution.FromRole(Role.Scrim),
        CustomSlot.FocusRing to SlotResolution.FromRamp(TonalRamp.Primary, light = 50, dark = 60),
        CustomSlot.Shadow to SlotResolution.FromRamp(TonalRamp.Neutral, light = 0, dark = 0),
    )

    @Test
    fun customSlot_entries_areTheSlotsOfTheCustomTarget() {
        assertEquals(slotNames, CustomSlot.entries.map { slot -> slot.propertyName() })
    }

    @Test
    fun customSlot_everySlot_carriesTheCodeTheShareCodecWrites() {
        assertEquals(slotCodes.size, CustomSlot.entries.size, "A slot was added or dropped without touching the table")

        CustomSlot.entries.forEach { slot ->
            val name = slot.propertyName()
            val expected = assertNotNull(slotCodes[name], "$name is missing from the table")
            assertEquals(expected, slot.code, "$name carries a code no shared link knows about")
        }
    }

    @Test
    fun customSlot_everySlot_resolvesWhereTheTableSays() {
        assertEquals(slotResolutions.size, CustomSlot.entries.size, "A slot resolves nowhere in particular")

        CustomSlot.entries.forEach { slot ->
            val expected = assertNotNull(slotResolutions[slot], "${slot.name} is missing from the table")
            assertEquals(expected, slot.resolution, "${slot.name} takes its color from somewhere else now")
        }
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

    @Test
    fun customTone_onARampSlot_movesThatSlotsOwnTone() {
        val muted = CustomSlot.TextMuted.resolution as? SlotResolution.FromRamp
        val resolution = assertNotNull(muted, "TextMuted stopped being cut off a ramp")
        val moved = CustomTone(light = 12, dark = 96)

        val document = ThemeDocument.Default.copy(customTones = mapOf(CustomSlot.TextMuted to moved))

        // Decision D22 hangs the tone on the slot it is attached to, so no neighbour follows it.
        assertEquals(moved, document.customTones[CustomSlot.TextMuted])
        assertEquals(40, resolution.light, "The slot's own light tone followed the override")
        assertEquals(70, resolution.dark, "The slot's own dark tone followed the override")
        assertNull(document.customTones[CustomSlot.TextStrong], "The override landed on another slot")
    }

    @Test
    fun customSlot_retiredCodes_areCarriedByNothing() {
        // The Love, Cold and Warm families and the five drinks left in B-102b and became accents.
        // A link shared before that still carries these numbers, so nothing may claim one again.
        val retired = listOf(18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 41, 42, 43, 44, 45)

        retired.forEach { code ->
            assertNull(CustomSlot.entries.withCodeOrNull(code), "Code $code was retired and something took it back")
        }
    }

    @Test
    fun tonalRamp_entries_areThePalettesTheThemeBuilds() {
        // A ramp only exists if a seed somewhere fills it, and the seeds are the key colors. A ramp
        // with no key color behind it is one the engine cannot resolve for any document.
        assertEquals(KeyColor.entries.map { color -> color.name }, TonalRamp.entries.map { ramp -> ramp.name })
    }

    @Test
    fun customSlot_everyRampItNames_isOneTheThemeBuilds() {
        val live = KeyColor.entries.map { color -> color.name }.toSet()

        CustomSlot.entries.forEach { slot ->
            val ramp = when (val resolution = slot.resolution) {
                is SlotResolution.FromRole -> null
                is SlotResolution.FromRamp -> resolution.ramp
                is SlotResolution.OnRamp -> resolution.ramp
            }

            if (ramp != null) {
                assertTrue(ramp.name in live, "${slot.name} cuts off $ramp, which no theme can hand back")
            }
        }
    }

    private fun CustomSlot.propertyName(): String = name.replaceFirstChar { first -> first.lowercaseChar() }
}
