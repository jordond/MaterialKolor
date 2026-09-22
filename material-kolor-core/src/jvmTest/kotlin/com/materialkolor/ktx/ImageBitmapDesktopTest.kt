@file:Suppress("DEPRECATION")

package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

@OptIn(ExperimentalTestApi::class)
class ImageBitmapDesktopTest {
    private val fallback = Color(0xff123456)

    @Test
    fun cameraSizedPhotoIsSeededInMilliseconds() {
        // Warm the quantizer up so the measurement below times the work, not the class loading.
        gradientBitmap(width = 64, height = 64).themeColor(fallback = fallback)

        val image = gradientBitmap(width = 4000, height = 3000)
        val started = TimeSource.Monotonic.markNow()
        val seed = image.themeColor(fallback = fallback)
        val elapsed = started.elapsedNow()

        assertNotEquals(fallback, seed)
        assertTrue(
            elapsed.inWholeMilliseconds < 500,
            "seeding a 4000 by 3000 photo took $elapsed, it should sample instead of reading every pixel",
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
}
