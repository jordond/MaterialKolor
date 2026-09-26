@file:Suppress("DEPRECATION")

package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ImageBitmapDesktopTest {
    private val fallback = Color(0xff123456)

    @Test
    fun cameraSizedPhotoIsSampledInsteadOfReadInFull() {
        val image = PixelCountingBitmap(gradientBitmap(width = 4000, height = 3000))

        val seed = image.themeColor(fallback = fallback)

        assertNotEquals(fallback, seed)
        assertTrue(
            image.pixelsRead < image.width * image.height / 20,
            "seeding a 4000 by 3000 photo read ${image.pixelsRead} pixels, it should sample instead of reading every pixel",
        )
    }

    @Test
    fun rememberThemeColorAnswersFallbackUntilTheWorkFinishes() =
        runComposeUiTest {
            val image = gradientBitmap(width = 4000, height = 3000)
            var firstFrame: Color? = null
            var latest: Color? = null

            setContent {
                val color = rememberThemeColor(image = image, fallback = fallback)
                if (firstFrame == null) firstFrame = color
                latest = color
            }

            assertTrue(firstFrame == fallback, "the first frame should paint with the fallback, not block on the photo")

            waitForIdle()
            waitUntil(timeoutMillis = 30_000) { latest != fallback }
            assertNotEquals(fallback, latest)
        }

    private fun gradientBitmap(
        width: Int,
        height: Int,
    ): ImageBitmap {
        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val row = IntArray(width)
        for (y in 0 until height) {
            val green = (y * 255) / (height - 1)
            for (x in 0 until width) {
                val red = (x * 255) / (width - 1)
                row[x] = (0xff shl 24) or (red shl 16) or (green shl 8)
            }
            buffered.setRGB(0, y, width, 1, row, 0, width)
        }

        return buffered.toComposeImageBitmap()
    }

    private class PixelCountingBitmap(
        private val delegate: ImageBitmap,
    ) : ImageBitmap by delegate {
        var pixelsRead = 0L
            private set

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) {
            pixelsRead += width.toLong() * height
            delegate.readPixels(buffer, startX, startY, width, height, bufferOffset, stride)
        }
    }
}
