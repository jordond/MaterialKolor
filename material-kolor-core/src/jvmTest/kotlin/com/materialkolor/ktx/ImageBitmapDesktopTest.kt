@file:Suppress("DEPRECATION")

package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val THEME_COLOR_TIMEOUT_MILLIS = 30_000L

@OptIn(ExperimentalTestApi::class)
class ImageBitmapDesktopTest {
    private val fallback = Color(0xff123456)

    @Test
    fun cameraSizedPhotoIsSampledInsteadOfReadInFull() {
        val image = GradientBitmap(width = 4000, height = 3000)

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
            val image = GradientBitmap(width = 4000, height = 3000)
            var firstFrame: Color? = null
            var latest: Color? = null

            setContent {
                val color = rememberThemeColor(image = image, fallback = fallback)
                if (firstFrame == null) firstFrame = color
                latest = color
            }

            assertTrue(firstFrame == fallback, "the first frame should paint with the fallback, not block on the photo")

            waitForIdle()
            waitUntil(timeoutMillis = THEME_COLOR_TIMEOUT_MILLIS) { latest != fallback }
            assertNotEquals(fallback, latest)
        }

    /**
     * A red to green gradient that computes each pixel on read, so a camera sized photo costs no memory.
     */
    private class GradientBitmap(
        override val width: Int,
        override val height: Int,
    ) : ImageBitmap {
        override val colorSpace = ColorSpaces.Srgb
        override val hasAlpha = false
        override val config = ImageBitmapConfig.Argb8888

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
            for (row in 0 until height) {
                val green = ((startY + row) * 255) / (this.height - 1)
                for (column in 0 until width) {
                    val red = ((startX + column) * 255) / (this.width - 1)
                    buffer[bufferOffset + row * stride + column] = (0xff shl 24) or (red shl 16) or (green shl 8)
                }
            }
        }

        override fun prepareToDraw() = Unit
    }
}
