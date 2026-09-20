package com.materialkolor

import com.materialkolor.blend.Blend
import com.materialkolor.contrast.Contrast
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.Variant
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.score.Score
import com.materialkolor.utils.ColorUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConvenienceContractsTest {
    @Test
    fun floatContrastOverloadsDelegateWithoutChangingPrecision() {
        for (tone in listOf(0.0, 20.0, 50.0, 80.0, 100.0)) {
            for (ratio in listOf(1.0f, 3.0f, 4.5f, 21.0f)) {
                assertEquals(Contrast.lighter(tone, ratio.toDouble())?.toFloat(), Contrast.lighter(tone, ratio))
                assertEquals(Contrast.darker(tone, ratio.toDouble())?.toFloat(), Contrast.darker(tone, ratio))
                assertEquals(
                    Contrast.lighterUnsafe(tone, ratio.toDouble()).toFloat(),
                    Contrast.lighterUnsafe(tone, ratio),
                )
                assertEquals(
                    Contrast.darkerUnsafe(tone, ratio.toDouble()).toFloat(),
                    Contrast.darkerUnsafe(tone, ratio),
                )
            }
        }
    }

    @Test
    fun harmonizeAcceptsHctValuesAndReturnsAnHctValue() {
        val design = Hct.fromInt(0xffff0000.toInt())
        val source = Hct.fromInt(0xff4285f4.toInt())
        assertEquals(Hct.fromInt(Blend.harmonize(design.toInt(), source.toInt())), Blend.harmonize(design, source))
        assertEquals(0xffff0000.toInt(), design.toInt())
        assertEquals(0xff4285f4.toInt(), source.toInt())
    }

    @Test
    fun luminanceIsNormalizedAndIgnoresAlpha() {
        assertEquals(0.0, ColorUtils.calculateLuminance(0xff000000.toInt()))
        assertEquals(1.0, ColorUtils.calculateLuminance(0xffffffff.toInt()), 1e-12)
        assertEquals(0.2126, ColorUtils.calculateLuminance(0xffff0000.toInt()), 1e-12)
        assertEquals(ColorUtils.calculateLuminance(0xff4285f4.toInt()), ColorUtils.calculateLuminance(0x004285f4))
    }

    @Test
    fun scorePreservesDefaultExplicitAndAbsentFallback() {
        assertEquals(listOf(0xff4285f4.toInt()), Score.score(emptyMap()))
        assertEquals(listOf(0xff123456.toInt()), Score.score(emptyMap(), fallbackColorArgb = 0xff123456.toInt()))
        assertEquals(emptyList(), Score.score(emptyMap(), fallbackColorArgb = null))
        val monochrome = mapOf(0xff000000.toInt() to 4, 0xffffffff.toInt() to 4)
        assertEquals(listOf(0xff123456.toInt()), Score.score(monochrome, fallbackColorArgb = 0xff123456.toInt()))
        assertEquals(emptyList(), Score.score(monochrome, fallbackColorArgb = null))
    }

    @Test
    fun customPalettesSurviveSchemeFactories() {
        val palettes = listOf(20.0, 80.0, 140.0, 200.0, 260.0, 320.0).map { TonalPalette.fromHueAndChroma(it, 30.0) }
        val scheme = DynamicScheme(
            sourceColorHct = Hct.fromInt(0xff4285f4.toInt()),
            variant = Variant.TONAL_SPOT,
            isDark = false,
            contrastLevel = 0.0,
            primaryPalette = palettes[0],
            secondaryPalette = palettes[1],
            tertiaryPalette = palettes[2],
            neutralPalette = palettes[3],
            neutralVariantPalette = palettes[4],
            errorPalette = palettes[5],
        )
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, scheme.specVersion)
        assertEquals(DynamicScheme.Platform.PHONE, scheme.platform)
        val changed = DynamicScheme.from(scheme, isDark = true, contrastLevel = 0.5)
        assertTrue(changed.isDark)
        assertFalse(scheme.isDark)
        assertEquals(0.5, changed.contrastLevel)
        assertEquals(scheme.sourceColorHct, changed.sourceColorHct)
        val retained = listOf(
            changed.primaryPalette,
            changed.secondaryPalette,
            changed.tertiaryPalette,
            changed.neutralPalette,
            changed.neutralVariantPalette,
            changed.errorPalette,
        )
        palettes.zip(retained).forEach { (original, copy) -> assertSame(original, copy) }
    }

    @Test
    fun paletteFactoriesKeepToneAndBackgroundPolicy() {
        val palette: (DynamicScheme) -> TonalPalette = { it.primaryPalette }
        val tone: (DynamicScheme) -> Double = { 42.0 }
        val foreground = DynamicColor.fromPalette("custom", palette, tone)
        val background = DynamicColor.fromPalette("custom", palette, tone, true)
        assertSame(palette, foreground.palette)
        assertSame(tone, foreground.tone)
        assertFalse(foreground.isBackground)
        assertTrue(background.isBackground)
        assertEquals(foreground, DynamicColor.fromPalette("custom", palette, tone))
    }
}
