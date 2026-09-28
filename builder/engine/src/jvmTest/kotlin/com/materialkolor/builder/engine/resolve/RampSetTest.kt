package com.materialkolor.builder.engine.resolve

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class RampSetTest {
    private val document = ThemeDocument(seed = Argb(0x6750A4))

    @Test
    fun from_everyMode_laysOutSixPalettesAtEighteenTones() {
        val result = ThemeResolver().resolve(document)
        for (isDark in listOf(false, true)) {
            val scheme = result.scheme(isDark)
            val ramps = result.ramps.mode(isDark)

            assertEquals(RampSet.Palettes, ramps.map { ramp -> ramp.palette })
            for (ramp in ramps) {
                val palette = scheme.palette(ramp.palette)
                val expected = RampSet.Tones.map { tone -> Argb(palette.tone(tone)) }

                assertEquals(18, ramp.steps.size)
                assertEquals(RampSet.Tones, ramp.steps.map { step -> step.tone })
                assertEquals(expected, ramp.steps.map { step -> step.argb })
                assertEquals(Argb(palette.keyColor.toInt()), ramp.keyColor)
                assertEquals(palette.keyColor.tone, ramp.keyTone)
            }
        }
    }

    @Test
    fun from_tonalSpot2021Light_marksPrimaryAtToneForty() {
        val result = ThemeResolver().resolve(document)
        val marker = result.ramps[KeyColor.Primary, false].markers.single { marker -> marker.role == Role.Primary }

        assertEquals(40.0, marker.tone, absoluteTolerance = 0.5)
        assertEquals(result.roles[Role.Primary, false].argb, marker.argb)
    }

    @Test
    fun from_everyStyleAndSpec_marksEveryRoleOnce() {
        val resolver = ThemeResolver()
        for ((style, spec) in styleSpecs()) {
            val ramps = resolver.resolve(document.copy(style = style, spec = spec)).ramps
            for (isDark in listOf(false, true)) {
                val marked = ramps.mode(isDark).flatMap { ramp -> ramp.markers.map { marker -> marker.role } }

                assertEquals(Role.entries.sorted(), marked.sorted(), "$style at $spec, dark $isDark")
            }
        }
    }

    @Test
    fun from_monochrome2021_marksOutlineOnTheNeutralVariantRamp() {
        val ramps = ThemeResolver().resolve(document.copy(style = Style.Monochrome)).ramps
        for (isDark in listOf(false, true)) {
            val neutralVariant = ramps[KeyColor.NeutralVariant, isDark].markers.map { marker -> marker.role }
            val neutral = ramps[KeyColor.Neutral, isDark].markers.map { marker -> marker.role }

            assertEquals(true, Role.Outline in neutralVariant, "dark $isDark")
            assertEquals(false, Role.Outline in neutral, "dark $isDark")
        }
    }

    @Test
    fun from_surfaceRoles_areMarkedOnTheNeutralRamp() {
        val ramps = ThemeResolver().resolve(document).ramps
        val neutral = ramps[KeyColor.Neutral, true].markers.map { marker -> marker.role }

        assertEquals(true, Role.Surface in neutral)
        assertEquals(true, Role.Background in neutral)
        assertEquals(true, Role.Scrim in neutral)
    }

    /**
     * Every style at 2021 and 2025, and Cmf at the 2026 spec it belongs to.
     */
    private fun styleSpecs(): List<Pair<Style, SpecVersion>> =
        Style.entries.flatMap { style ->
            when (style) {
                Style.Cmf -> listOf(style to SpecVersion.Spec2026)
                Style.TonalSpot,
                Style.Neutral,
                Style.Vibrant,
                Style.Expressive,
                Style.Rainbow,
                Style.FruitSalad,
                Style.Monochrome,
                Style.Fidelity,
                Style.Content,
                -> listOf(style to SpecVersion.Spec2021, style to SpecVersion.Spec2025)
            }
        }

    private fun DynamicScheme.palette(palette: KeyColor): TonalPalette =
        when (palette) {
            KeyColor.Primary -> primaryPalette
            KeyColor.Secondary -> secondaryPalette
            KeyColor.Tertiary -> tertiaryPalette
            KeyColor.Error -> errorPalette
            KeyColor.Neutral -> neutralPalette
            KeyColor.NeutralVariant -> neutralVariantPalette
        }
}
