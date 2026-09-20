package com.materialkolor

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.ContrastCurve
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.ToneDeltaPair
import com.materialkolor.dynamiccolor.ToneDeltaPair.TonePolarity
import com.materialkolor.dynamiccolor.extendSpecVersion
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.SchemeCmf
import com.materialkolor.scheme.SchemeTonalSpot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KotlinApiContractsTest {
    @Test
    fun floatHctAdjustmentMembersDelegateToDoubleMembers() {
        val original = Hct.fromInt(0xff4285f4.toInt())
        assertEquals(original.withHue(120.0), original.withHue(120.0f))
        assertEquals(original.withChroma(5.0), original.withChroma(5.0f))
        assertEquals(original.withTone(90.0), original.withTone(90.0f))
        assertEquals(0xff4285f4.toInt(), original.toInt())
    }

    @Test
    fun dynamicColorDefaultsAndCopyKeepValueSemantics() {
        val scheme = SchemeTonalSpot(Hct.fromInt(0xff4285f4.toInt()), false, 0.0)
        val color = DynamicColor(name = "custom", palette = { it.primaryPalette })
        assertFalse(color.isBackground)
        assertEquals(50.0, color.tone(scheme))
        assertEquals(50.0, DynamicColor.getInitialToneFromBackground()(scheme))
        color.getHct(scheme)
        val copy = color.copy()
        assertNotSame(color, copy)
        assertEquals(color, copy)
        assertEquals(color.hashCode(), copy.hashCode())
        assertEquals(color.getArgb(scheme), copy.getArgb(scheme))
        val background = color.copy(isBackground = true)
        assertNotEquals(color, background)
        assertTrue(background.isBackground)
        val onBackground = DynamicColor(
            name = "on_custom",
            palette = color.palette,
            background = { color },
            contrastCurve = { ContrastCurve(1.0, 1.0, 1.0, 1.0) },
        )
        assertEquals(color.getTone(scheme), onBackground.tone(scheme))
    }

    @Test
    fun toneDeltaCopyUsesConstraintSpellingAndKeepsTheOriginal() {
        val first = DynamicColor.fromArgb("first", 0xff4285f4.toInt())
        val second = DynamicColor.fromArgb("second", 0xffff0000.toInt())
        val original = ToneDeltaPair(first, second, 15.0, TonePolarity.DARKER)
        assertEquals(original, original.copy())
        val changed = original.copy(constraint = ToneDeltaPair.DeltaConstraint.FARTHER)
        assertEquals(ToneDeltaPair.DeltaConstraint.EXACT, original.constraint)
        assertEquals(ToneDeltaPair.DeltaConstraint.FARTHER, changed.constraint)
        assertNotEquals(original, changed)
    }

    @Test
    fun specificationExtensionsApplyToLaterSpecifications() {
        val source = Hct.fromInt(0xff4285f4.toInt())
        val oldPalette = TonalPalette.fromHueAndChroma(20.0, 20.0)
        val newPalette = TonalPalette.fromHueAndChroma(200.0, 40.0)
        val original = DynamicColor(name = "custom", palette = { oldPalette }, tone = { 30.0 })
        val extended = original.extendSpecVersion(
            SpecVersion.SPEC_2025,
            original.copy(palette = { newPalette }, tone = { 70.0 }),
        )
        val old = SchemeTonalSpot(source, false, 0.0, SpecVersion.SPEC_2021)
        val current = SchemeTonalSpot(source, false, 0.0, SpecVersion.SPEC_2025)
        val later = SchemeCmf(source, false, 0.0)
        assertSame(oldPalette, extended.palette(old))
        assertEquals(30.0, extended.tone(old))
        for (scheme in listOf(current, later)) {
            assertSame(newPalette, extended.palette(scheme))
            assertEquals(70.0, extended.tone(scheme))
        }
        assertSame(oldPalette, original.palette(later))
    }

    @Test
    fun schemeCopyPreservesAllSourcesAndCustomPalettes() {
        val source = listOf(Hct.fromInt(0xff4285f4.toInt()), Hct.fromInt(0xffff0000.toInt()))
        val original = SchemeCmf(source, false, 0.0)
        val customPrimary = TonalPalette.fromHueAndChroma(120.0, 40.0)
        val copy = original.copy(isDark = true, primaryPalette = customPrimary)
        assertEquals(source, copy.sourceColorHctList)
        assertEquals(SpecVersion.SPEC_2026, copy.specVersion)
        assertTrue(copy.isDark)
        assertFalse(original.isDark)
        assertSame(customPrimary, copy.primaryPalette)
        assertSame(original.secondaryPalette, copy.secondaryPalette)
        assertSame(original.tertiaryPalette, copy.tertiaryPalette)
        assertSame(original.neutralPalette, copy.neutralPalette)
        assertSame(original.neutralVariantPalette, copy.neutralVariantPalette)
        assertSame(original.errorPalette, copy.errorPalette)
        val requested2026 = SchemeTonalSpot(source.first(), false, 0.0, SpecVersion.SPEC_2026)
        assertEquals(SpecVersion.SPEC_2025, requested2026.specVersion)
        assertEquals(SpecVersion.SPEC_2025, requested2026.copy().specVersion)
    }
}
