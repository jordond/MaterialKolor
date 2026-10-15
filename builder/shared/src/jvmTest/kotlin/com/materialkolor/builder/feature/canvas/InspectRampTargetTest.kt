package com.materialkolor.builder.feature.canvas

import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Role
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InspectRampTargetTest {
    @Test
    fun rampTarget_roleAndAccent_keepTheModeTheyWereInspectedIn() {
        val slot = AccentSlot(index = 0, part = AccentPart.OnContainer)
        val role = ColorRef.OfRole(Role.Tertiary)

        role.rampTarget(isDark = true) shouldBe RampTarget.OfRole(Role.Tertiary, isDark = true)
        ColorRef.OfAccent(slot).rampTarget(isDark = false) shouldBe RampTarget.OfAccent(slot, isDark = false)
    }

    @Test
    fun rampTarget_colorsOnNoRampOfTheTab_areNone() {
        ColorRef.OfSlot(CustomSlot.entries.first()).rampTarget(isDark = false) shouldBe null
        ColorRef.OfFluentShade(FluentShade.Base).rampTarget(isDark = true) shouldBe null
    }
}
