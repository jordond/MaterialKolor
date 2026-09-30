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
