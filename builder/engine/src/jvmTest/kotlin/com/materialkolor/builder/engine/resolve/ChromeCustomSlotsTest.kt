package com.materialkolor.builder.engine.resolve

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ChromeCustomSlotsTest {
    @Test
    fun chromeCustomSlots_reducedContrastWithPinAmoledAndTone_matchTheStandardSlots() {
        val resolver = ThemeResolver()
        val standard = resolver.resolve(ThemeDocument(seed = Argb(0x6750A4)))
        val pin = Argb(0x123456)
        val reduced = resolver.resolve(
            standard.document.copy(
                contrast = ContrastLevel.Reduced,
                amoled = true,
                pins = mapOf(Role.Primary to RolePin(light = pin, dark = pin)),
                customTones = mapOf(CustomSlot.PrimaryPressed to CustomTone(light = 20, dark = 20)),
            ),
        )

        assertEquals(pin, reduced.customSlots[CustomSlot.Primary, false])
        assertNotEquals(standard.customSlots.light, reduced.customSlots.light)
        assertEquals(standard.customSlots.light, reduced.chromeCustomSlots.light)
        assertEquals(standard.customSlots.dark, reduced.chromeCustomSlots.dark)
        assertNotEquals(pin, reduced.chromeCustomSlots[CustomSlot.Primary, false])
    }
}
