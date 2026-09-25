package com.materialkolor.builder.engine.resolve

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.MaterialKolors
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.model.TonalRamp
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomSlotColorsTest {
    private val document = ThemeDocument(seed = Argb(0x6750A4))

    @Test
    fun from_defaultDocument_matchesTheSampleRecipe() {
        val slots = ThemeResolver().resolve(document).customSlots

        for (isDark in listOf(false, true)) {
            val scheme = DynamicScheme(
                seedColor = Color(0xFF6750A4),
                isDark = isDark,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            )
            val expected = sampleColors(scheme)
            for (slot in CustomSlot.entries) {
                assertEquals(expected.getValue(slot), slots[slot, isDark], "$slot, dark $isDark")
            }
        }
    }

    @Test
    fun from_pinsAndAmoled_reachTheRoleSlots() {
        val pin = Argb(0x123456)
        val result = ThemeResolver().resolve(
            document.copy(amoled = true, pins = mapOf(Role.Primary to RolePin(light = pin))),
        )

        assertEquals(pin, result.customSlots[CustomSlot.Primary, false])
        assertEquals(result.roles[Role.Primary, true].argb, result.customSlots[CustomSlot.Primary, true])
        assertEquals(Argb(0x000000), result.customSlots[CustomSlot.Surface, true])
        assertEquals(Argb(0xFFFFFF), result.customSlots[CustomSlot.OnSurface, true])
    }

    @Test
    fun from_customToneOnRampSlot_cutsThatModeAtTheMovedTone() {
        val result = ThemeResolver().resolve(
            document.copy(customTones = mapOf(CustomSlot.PrimaryPressed to CustomTone(light = 20))),
        )

        assertEquals(Argb(result.light.primaryPalette.tone(20)), result.customSlots[CustomSlot.PrimaryPressed, false])
        assertEquals(Argb(result.dark.primaryPalette.tone(70)), result.customSlots[CustomSlot.PrimaryPressed, true])
    }

    @Test
    fun from_customToneOnRoleSlot_keepsTheRole() {
        val result = ThemeResolver().resolve(
            document.copy(customTones = mapOf(CustomSlot.Primary to CustomTone(light = 20, dark = 20))),
        )

        assertEquals(result.roles[Role.Primary, false].argb, result.customSlots[CustomSlot.Primary, false])
        assertEquals(result.roles[Role.Primary, true].argb, result.customSlots[CustomSlot.Primary, true])
    }

    @Test
    fun resolve_onRampWithoutTone_readsOnItsBackground() {
        val result = ThemeResolver().resolve(document)
        val onRamp = SlotResolution.OnRamp(TonalRamp.Tertiary, light = 40, dark = 80)

        for (isDark in listOf(false, true)) {
            val scheme = result.scheme(isDark)
            val background = if (isDark) 80 else 40
            val resolved = CustomSlotColors.resolve(onRamp, tone = null, scheme, result.roles, isDark)
            val ratio = resolved.toColor().contrastRatio(scheme.tertiaryPalette.toneColor(background))

            assertEquals(argb(scheme.tertiaryPalette.onTone(background)), resolved, "dark $isDark")
            assertTrue(ratio >= 4.5, "dark $isDark, got $ratio")
        }
    }

    @Test
    fun resolve_onRampWithTone_takesTheOnColorsOwnToneAsIs() {
        val result = ThemeResolver().resolve(document)
        val onRamp = SlotResolution.OnRamp(TonalRamp.Primary, light = 40, dark = 80)
        val tone = CustomTone(light = 55)

        val light = CustomSlotColors.resolve(onRamp, tone, result.light, result.roles, isDark = false)
        val dark = CustomSlotColors.resolve(onRamp, tone, result.dark, result.roles, isDark = true)

        // Tone 55 on tone 40 is well under 4.5 to 1, which proves the contrast search was skipped.
        assertEquals(Argb(result.light.primaryPalette.tone(55)), light)
        assertTrue(light.toColor().contrastRatio(result.light.primaryPalette.toneColor(40)) < 4.5)
        assertEquals(argb(result.dark.primaryPalette.onTone(80)), dark)
    }

    @Test
    fun resolve_fromRampWithTone_cutsAtTheMovedTone() {
        val result = ThemeResolver().resolve(document)
        val fromRamp = SlotResolution.FromRamp(TonalRamp.Secondary, light = 40, dark = 80)

        val tone = CustomTone(dark = 60)

        val light = CustomSlotColors.resolve(fromRamp, tone, result.light, result.roles, isDark = false)
        val dark = CustomSlotColors.resolve(fromRamp, tone, result.dark, result.roles, isDark = true)

        assertEquals(Argb(result.light.secondaryPalette.tone(40)), light)
        assertEquals(Argb(result.dark.secondaryPalette.tone(60)), dark)
    }

    /**
     * Every slot the way the custom theme sample's `AppColorsFactory` builds it.
     */
    private fun sampleColors(scheme: DynamicScheme): Map<CustomSlot, Argb> {
        val kolors = MaterialKolors(scheme)
        val dark = scheme.isDark
        val primary = scheme.primaryPalette
        val neutral = scheme.neutralPalette
        val neutralVariant = scheme.neutralVariantPalette

        fun tone(
            light: Int,
            darkTone: Int,
        ): Int = if (dark) darkTone else light

        return CustomSlot.entries.associateWith { slot ->
            val color = when (slot) {
                CustomSlot.Primary -> kolors.primary()
                CustomSlot.OnPrimary -> kolors.onPrimary()
                CustomSlot.PrimaryContainer -> kolors.primaryContainer()
                CustomSlot.OnPrimaryContainer -> kolors.onPrimaryContainer()
                CustomSlot.PrimaryPressed -> primary.toneColor(tone(32, 70))
                CustomSlot.PrimaryRaised -> primary.toneColor(tone(46, 88))
                CustomSlot.Secondary -> kolors.secondary()
                CustomSlot.OnSecondary -> kolors.onSecondary()
                CustomSlot.SecondaryContainer -> kolors.secondaryContainer()
                CustomSlot.OnSecondaryContainer -> kolors.onSecondaryContainer()
                CustomSlot.Tertiary -> kolors.tertiary()
                CustomSlot.OnTertiary -> kolors.onTertiary()
                CustomSlot.TertiaryContainer -> kolors.tertiaryContainer()
                CustomSlot.OnTertiaryContainer -> kolors.onTertiaryContainer()
                CustomSlot.Error -> kolors.error()
                CustomSlot.OnError -> kolors.onError()
                CustomSlot.ErrorContainer -> kolors.errorContainer()
                CustomSlot.OnErrorContainer -> kolors.onErrorContainer()
                CustomSlot.Surface -> kolors.surface()
                CustomSlot.SurfaceRaised -> neutral.toneColor(tone(100, 12))
                CustomSlot.SurfaceSunken -> neutral.toneColor(tone(94, 4))
                CustomSlot.SurfaceInverse -> kolors.inverseSurface()
                CustomSlot.OnSurface -> kolors.onSurface()
                CustomSlot.OnSurfaceInverse -> kolors.inverseOnSurface()
                CustomSlot.TextStrong -> neutralVariant.toneColor(tone(10, 90))
                CustomSlot.TextMuted -> neutralVariant.toneColor(tone(40, 70))
                CustomSlot.BorderFaint -> neutralVariant.toneColor(tone(92, 22))
                CustomSlot.BorderSoft -> neutralVariant.toneColor(tone(85, 32))
                CustomSlot.BorderStrong -> neutralVariant.toneColor(tone(55, 65))
                CustomSlot.Scrim -> kolors.scrim()
                CustomSlot.FocusRing -> primary.toneColor(tone(50, 60))
                CustomSlot.Shadow -> kolors.shadow()
            }
            argb(color)
        }
    }

    private fun argb(color: Color): Argb = Argb(color.toArgb())
}
