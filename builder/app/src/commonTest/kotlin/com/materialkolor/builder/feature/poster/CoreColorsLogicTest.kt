package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.capability.Reason
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CoreColorsLogicTest {
    @Test
    fun reasonText_everyReason_isTheStringNamedByItsKey() {
        Reason.entries.forEach { reason -> reasonText(reason).key shouldBe reason.key }
    }

    @Test
    fun explanation_eachState_isItsNoteOrItsReason() {
        ControlState.Enabled().explanation shouldBe null
        ControlState.Enabled(Reason.FluentStyleChroma).explanation shouldBe Reason.FluentStyleChroma
        ControlState.Disabled(Reason.FluentOneRamp).explanation shouldBe Reason.FluentOneRamp
        ControlState.Hidden(Reason.DimRolesUnexposed).explanation shouldBe Reason.DimRolesUnexposed
        ControlState.Enabled(Reason.FluentStyleChroma).usable shouldBe true
        ControlState.Disabled(Reason.FluentOneRamp).usable shouldBe false
    }

    @Test
    fun keyColorName_sixPalettes_eachHaveTheirOwnName() {
        KeyColor.entries.map(::keyColorName).shouldBeUnique()
    }

    @Test
    fun pinnedModes_listRoleOrderThenLightBeforeDark() {
        val pins = mapOf(
            Role.Surface to RolePin(dark = Argb(0x111111)),
            Role.OnPrimary to RolePin(light = Argb(0xFFFFFF), dark = Argb(0x222222)),
            Role.Primary to RolePin(light = Argb(0x6750A4)),
        )

        PinnedMode.of(pins) shouldBe listOf(
            PinnedMode(Role.Primary, PinMode.Light, Argb(0x6750A4)),
            PinnedMode(Role.OnPrimary, PinMode.Light, Argb(0xFFFFFF)),
            PinnedMode(Role.OnPrimary, PinMode.Dark, Argb(0x222222)),
            PinnedMode(Role.Surface, PinMode.Dark, Argb(0x111111)),
        )
        PinnedMode.of(emptyMap()).shouldBeEmpty()
    }
}
