@file:Suppress("DEPRECATION")

package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeCmf
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.materialkolor.dynamiccolor.DynamicScheme as McuDynamicScheme

class ConsumerContractsTest {
    @Test
    fun impossibleLighteningAndDarkeningReturnOriginalIncludingAlpha() {
        val colors = listOf(Color.White, Color.Black, Color(0x804285f4), Color.Transparent)
        colors.forEach { color ->
            assertEquals(color, color.lighten(100.0f))
            assertEquals(color, color.darken(100.0f))
        }
        assertTrue(Color(0xff4285f4).lighten(1.5f).toHct().tone > Color(0xff4285f4).toHct().tone)
        assertTrue(Color(0xff4285f4).darken(1.5f).toHct().tone < Color(0xff4285f4).toHct().tone)
    }

    @Test
    fun customColorsBecomeTheirOwnPalettes() {
        val overrides = listOf(Color.Red, Color.Green, Color.Blue, Color.Gray, Color.Yellow, Color.Magenta)
        val scheme = DynamicScheme(
            seedColor = Color(0xff4285f4),
            isDark = false,
            primary = overrides[0],
            secondary = overrides[1],
            tertiary = overrides[2],
            neutral = overrides[3],
            neutralVariant = overrides[4],
            error = overrides[5],
        )
        assertEquals(
            overrides.map { TonalPalette.fromInt(it.toArgb()) },
            listOf(
                scheme.primaryPalette,
                scheme.secondaryPalette,
                scheme.tertiaryPalette,
                scheme.neutralPalette,
                scheme.neutralVariantPalette,
                scheme.errorPalette,
            ),
        )
    }

    @Test
    fun absentOverridesUseTheSelectedStyle() {
        val seed = Color(0xff4285f4)
        for (style in PaletteStyle.KnownStyles) {
            val expected = styleScheme(style, Hct.fromInt(seed.toArgb()))
            val actual = DynamicScheme(seedColor = seed, isDark = true, style = style)
            assertEquals(expected.variant, actual.variant, style.name)
            assertEquals(expected.specVersion, actual.specVersion, style.name)
            assertEquals(expected.primaryPalette, actual.primaryPalette, style.name)
            assertEquals(expected.secondaryPalette, actual.secondaryPalette, style.name)
            assertEquals(expected.tertiaryPalette, actual.tertiaryPalette, style.name)
            assertEquals(expected.neutralPalette, actual.neutralPalette, style.name)
            assertEquals(expected.neutralVariantPalette, actual.neutralVariantPalette, style.name)
            assertEquals(expected.errorPalette, actual.errorPalette, style.name)
        }
    }

    @Test
    fun imageFallbackDistinguishesRequestedFallbackFromNoColor() {
        val image = SolidBitmap(0xff000000.toInt())
        val fallback = Color(0xff123456)
        assertEquals(listOf(fallback), image.themeColors(fallback = fallback))
        assertEquals(fallback, image.themeColor(fallback = fallback))
        assertNull(image.themeColorOrNull())
        assertEquals(Color.Black, image.themeColorOrNull(filter = false))
    }

    private fun styleScheme(
        style: PaletteStyle,
        seed: Hct,
    ): McuDynamicScheme {
        val spec = ColorSpec.SpecVersion.SPEC_2025
        val platform = McuDynamicScheme.Platform.PHONE
        return when (style) {
            PaletteStyle.TonalSpot -> SchemeTonalSpot(seed, true, 0.0, spec, platform)
            PaletteStyle.Neutral -> SchemeNeutral(seed, true, 0.0, spec, platform)
            PaletteStyle.Vibrant -> SchemeVibrant(seed, true, 0.0, spec, platform)
            PaletteStyle.Expressive -> SchemeExpressive(seed, true, 0.0, spec, platform)
            PaletteStyle.Rainbow -> SchemeRainbow(seed, true, 0.0, spec, platform)
            PaletteStyle.FruitSalad -> SchemeFruitSalad(seed, true, 0.0, spec, platform)
            PaletteStyle.Monochrome -> SchemeMonochrome(seed, true, 0.0, spec, platform)
            PaletteStyle.Fidelity -> SchemeFidelity(seed, true, 0.0, spec, platform)
            PaletteStyle.Content -> SchemeContent(seed, true, 0.0, spec, platform)
            is PaletteStyle.Cmf -> SchemeCmf(listOf(seed), true, 0.0, ColorSpec.SpecVersion.SPEC_2026, platform)
        }
    }

    private class SolidBitmap(
        private val argb: Int,
    ) : ImageBitmap {
        override val width = 2
        override val height = 2
        override val colorSpace = ColorSpaces.Srgb
        override val hasAlpha = true
        override val config = ImageBitmapConfig.Argb8888

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) {
            for (row in 0 until height) {
                for (column in 0 until width) {
                    buffer[bufferOffset + row * stride + column] = argb
                }
            }
        }

        override fun prepareToDraw() = Unit
    }
}
