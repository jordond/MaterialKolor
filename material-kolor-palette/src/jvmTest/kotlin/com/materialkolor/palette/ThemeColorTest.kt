package com.materialkolor.palette

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.kmpalette.loader.ImageBitmapLoader
import com.kmpalette.rememberPaletteState
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.DynamicScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThemeColorTest {
    private val fallback = Color.Blue

    @Test
    fun rememberThemeColor_showsTheFallbackUntilTheImageIsRead() =
        runComposeUiTest {
            val image = solidBitmap(Color.Red)
            var firstFrame: Color? = null
            var latest: Color? = null

            setContent {
                val color = rememberThemeColor(loader = IdentityLoader, input = image, fallback = fallback)
                if (firstFrame == null) firstFrame = color
                latest = color
            }

            assertEquals(fallback, firstFrame)

            waitUntil { latest != fallback }
            assertCloseTo(Color.Red, assertNotNull(latest))
        }

    @Test
    fun rememberThemeColor_followsANewInput() =
        runComposeUiTest {
            val red = solidBitmap(Color.Red)
            val green = solidBitmap(Color.Green)
            var input by mutableStateOf(red)
            var latest: Color? = null

            setContent {
                latest = rememberThemeColor(loader = IdentityLoader, input = input, fallback = fallback)
            }

            waitUntil { latest != fallback }
            assertCloseTo(Color.Red, assertNotNull(latest))

            input = green
            waitUntil { latest?.let { color -> color.green > color.red } == true }
            assertCloseTo(Color.Green, assertNotNull(latest))
        }

    @Test
    fun paletteStateThemeColorOrNull_isNullUntilTheImageIsRead() =
        runComposeUiTest {
            val image = solidBitmap(Color.Red)
            var firstFrame: Color? = null
            var composed = false
            var latest: Color? = null

            setContent {
                val paletteState = rememberPaletteState(IdentityLoader)
                LaunchedEffect(image) { paletteState.generate(image) }

                val color = paletteState.themeColorOrNull()
                if (!composed) {
                    firstFrame = color
                    composed = true
                }
                latest = color
            }

            assertNull(firstFrame)

            waitUntil { latest != null }
            assertCloseTo(Color.Red, assertNotNull(latest))
        }

    @Test
    fun rememberDynamicScheme_seedsTheSchemeWithTheScoredColor() =
        runComposeUiTest {
            val image = solidBitmap(Color.Red)
            var seed: Color? = null
            var scheme: DynamicScheme? = null

            setContent {
                val state = rememberPaletteState(IdentityLoader)
                LaunchedEffect(image) { state.generate(image) }

                seed = state.themeColor(fallback)
                scheme = rememberDynamicScheme(palette = state, fallback = fallback, isDark = false)
            }

            waitUntil { seed != fallback }
            waitForIdle()

            val expected = DynamicScheme(seedColor = assertNotNull(seed), isDark = false)
            assertEquals(expected.primary, assertNotNull(scheme).primary)
        }

    @Test
    fun rememberThemeColor_readsALargeImageQuickly() =
        runComposeUiTest {
            val image = gradientBitmap(width = 4000, height = 3000)
            var latest: Color? = null

            setContent {
                latest = rememberThemeColor(loader = IdentityLoader, input = image, fallback = fallback)
            }

            // kmpalette scales before it quantizes. Without that this takes seconds, not milliseconds.
            waitUntil(timeoutMillis = 500) { latest != fallback }
        }
}

private object IdentityLoader : ImageBitmapLoader<ImageBitmap> {
    override suspend fun load(input: ImageBitmap): ImageBitmap = input
}

private fun solidBitmap(
    color: Color,
    width: Int = 64,
    height: Int = 64,
): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    Canvas(bitmap).drawRect(
        left = 0f,
        top = 0f,
        right = width.toFloat(),
        bottom = height.toFloat(),
        paint = Paint().apply { this.color = color },
    )

    return bitmap
}

private fun gradientBitmap(
    width: Int,
    height: Int,
): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val bands = 64
    val bandWidth = width.toFloat() / bands

    repeat(bands) { band ->
        val fraction = band.toFloat() / bands
        paint.color = Color(red = fraction, green = 1f - fraction, blue = 0.5f)
        canvas.drawRect(
            left = band * bandWidth,
            top = 0f,
            right = (band + 1) * bandWidth,
            bottom = height.toFloat(),
            paint = paint,
        )
    }

    return bitmap
}

private fun assertCloseTo(
    expected: Color,
    actual: Color,
) {
    val distance =
        listOf(expected.red - actual.red, expected.green - actual.green, expected.blue - actual.blue)
            .maxOf { channel -> kotlin.math.abs(channel) }

    assertTrue(distance < 0.15f, "expected $actual to be close to $expected")
}
