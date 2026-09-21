package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        for (style in PaletteStyle.KnownStyles) {
            val seed = Color(0xff4285f4)
            val expected = seed.toDynamicScheme(isDark = true, style = style)
            val actual = DynamicScheme(seedColor = seed, isDark = true, style = style)
            assertEquals(expected.primaryPalette, actual.primaryPalette)
            assertEquals(expected.secondaryPalette, actual.secondaryPalette)
            assertEquals(expected.tertiaryPalette, actual.tertiaryPalette)
            assertEquals(expected.errorPalette, actual.errorPalette)
        }
    }

    @Test
    fun imageFallbackDistinguishesRequestedFallbackFromNoColor() {
        val image = SolidBitmap(0xff000000.toInt())
        val fallback = Color(0xff123456)
        assertEquals(listOf(fallback), image.themeColors(fallback = fallback))
        assertEquals(fallback, image.themeColor(fallback = fallback))
        assertNull(image.themeColorOrNull())
        assertEquals(listOf(Color(0xff4285f4)), image.themeColors())
        assertEquals(Color.Black, image.themeColorOrNull(filter = false))
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
