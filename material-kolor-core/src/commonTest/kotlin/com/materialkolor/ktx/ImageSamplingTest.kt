@file:Suppress("DEPRECATION")

package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageSamplingTest {
    @Test
    fun bitmapWithinTheBudgetKeepsEveryPixel() {
        val image = FakeImageBitmap(width = 64, height = 64) { x, y -> 0xff000000.toInt() or (y * 64 + x) }

        val sampled = image.samplePixels()

        assertEquals(64, sampled.width)
        assertEquals(64, sampled.height)
        assertTrue(
            IntArray(64 * 64) { index -> 0xff000000.toInt() or index }.contentEquals(sampled.pixels),
            "every pixel should survive a bitmap that already fits the budget",
        )
    }

    @Test
    fun oversizedBitmapShrinksToTheBudget() {
        val image = FakeImageBitmap(width = 1000, height = 500) { _, _ -> 0xffff0000.toInt() }

        val sampled = image.samplePixels()

        assertEquals(182, sampled.width)
        assertEquals(91, sampled.height)
        assertEquals(182 * 91, sampled.pixels.size)
        assertTrue(
            sampled.pixels.size < image.width * image.height / 30,
            "half a megapixel should come back as a budget sized grid",
        )
    }

    @Test
    fun samplingKeepsTheLayoutOfTheSource() {
        val red = 0xffff0000.toInt()
        val blue = 0xff0000ff.toInt()
        val image = FakeImageBitmap(width = 1000, height = 500) { x, _ -> if (x < 500) red else blue }

        val sampled = image.samplePixels()

        for (y in 0 until sampled.height) {
            val row = sampled.pixels.copyOfRange(y * sampled.width, (y + 1) * sampled.width)
            assertEquals(List(91) { red } + List(91) { blue }, row.toList(), "row $y lost the split")
        }
    }

    @Test
    fun budgetOfZeroReadsEveryPixel() {
        val image = FakeImageBitmap(width = 300, height = 200) { x, y -> 0xff000000.toInt() or (y * 300 + x) }

        val sampled = image.samplePixels(sampleArea = 0)

        assertEquals(300, sampled.width)
        assertEquals(200, sampled.height)
        assertEquals(300 * 200, sampled.pixels.size)
    }

    @Test
    fun largeBitmapStillScoresItsOwnColor() {
        val image = FakeImageBitmap(width = 2000, height = 1500) { _, _ -> 0xffff0000.toInt() }

        assertEquals(Color.Red, image.themeColorOrNull())
    }

    private class FakeImageBitmap(
        override val width: Int,
        override val height: Int,
        private val argbAt: (x: Int, y: Int) -> Int,
    ) : ImageBitmap {
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
                    buffer[bufferOffset + row * stride + column] = argbAt(startX + column, startY + row)
                }
            }
        }

        override fun prepareToDraw() = Unit
    }
}
