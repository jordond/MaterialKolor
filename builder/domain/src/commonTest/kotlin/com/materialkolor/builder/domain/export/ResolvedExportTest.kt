package com.materialkolor.builder.domain.export

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResolvedExportTest {
    private val white = Argb(0xFFFFFF)

    private val black = Argb(0x000000)

    private val roleTable = RoleTable(
        light = Role.entries.associateWith { white },
        dark = Role.entries.associateWith { black },
    )

    private val slotValues = CustomSlotValues(
        light = CustomSlot.entries.associateWith { white },
        dark = CustomSlot.entries.associateWith { black },
    )

    @Test
    fun resolvedExport_everyVariant_keepsWhatItWasGiven() {
        val roles = ContrastVariant.entries.associateWith { roleTable }
        val slots = ContrastVariant.entries.associateWith { slotValues }
        val accent = AccentFamilyValues(
            name = "brand",
            light = AccentColors(color = black, onColor = white, container = white, onContainer = black),
            dark = AccentColors(color = white, onColor = black, container = black, onContainer = white),
        )
        val shades = FluentShades(
            light = FluentShadeValues(black, black, black, white, white, white, white),
            dark = FluentShadeValues(white, white, white, black, black, black, black),
        )

        val export = ResolvedExport(roles = roles, accents = listOf(accent), customSlots = slots, fluentShades = shades)

        assertEquals(roles, export.roles)
        assertEquals(listOf(accent), export.accents)
        assertEquals(slots, export.customSlots)
        assertEquals(shades, export.fluentShades)
        assertEquals(white, export.fluentShades?.light?.base)
        assertEquals(black, export.fluentShades?.dark?.base)
    }

    @Test
    fun resolvedExport_standardOnly_isAccepted() {
        val export = ResolvedExport(roles = mapOf(ContrastVariant.Standard to roleTable))

        assertEquals(emptyList(), export.accents)
        assertEquals(emptyMap(), export.customSlots)
        assertEquals(null, export.fluentShades)
    }

    @Test
    fun resolvedExport_withoutStandard_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            ResolvedExport(roles = mapOf(ContrastVariant.High to roleTable))
        }
    }

    @Test
    fun resolvedExport_slotsAtOtherVariantsThanRoles_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            ResolvedExport(
                roles = mapOf(ContrastVariant.Standard to roleTable),
                customSlots = ContrastVariant.entries.associateWith { slotValues },
            )
        }
    }

    @Test
    fun roleTable_missingRole_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            RoleTable(light = roleTable.light - Role.Scrim, dark = roleTable.dark)
        }
        assertFailsWith<IllegalArgumentException> {
            RoleTable(light = roleTable.light, dark = roleTable.dark - Role.Primary)
        }
    }

    @Test
    fun customSlotValues_missingSlot_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            CustomSlotValues(light = slotValues.light - CustomSlot.Shadow, dark = slotValues.dark)
        }
        assertFailsWith<IllegalArgumentException> {
            CustomSlotValues(light = slotValues.light, dark = slotValues.dark - CustomSlot.FocusRing)
        }
    }
}
