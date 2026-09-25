package com.materialkolor.builder.feature.image

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.image.PixelSample
import com.materialkolor.builder.engine.image.SeedExtractor
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.test.Test

/**
 * The long edge the extractor reads an image at, as the browser scales it.
 */
private const val PIXEL_EDGE = 128

/**
 * Keeps the preset candidates in `Presets.kt` honest. Each picture is decoded with Skia, scaled to
 * 128 px the way an image the user brings is, and handed to the extractor again.
 */
@OptIn(ExperimentalResourceApi::class)
class PresetCandidatesTest {
    @Test
    fun presetCandidates_matchWhatTheExtractorMakesOfEachPicture() =
        runTest {
            val environment = getSystemResourceEnvironment()
            val extracted = Presets.images.associate { preset ->
                val bytes = getDrawableResourceBytes(environment, preset.drawable)
                preset.id to SeedExtractor.extract(sampleOf(bytes)).candidates
            }

            val written = Presets.images.associate { preset -> preset.id to preset.candidates }

            // Should they drift, the message holds the lists to paste back.
            written.mapValues { (_, candidates) -> candidates.hexes() } shouldBe
                extracted.mapValues { (_, candidates) -> candidates.hexes() }
        }

    @Test
    fun presets_eachKeepAnIdOfTheirOwn_andThePicturesKeepTheOldBuildersIds() {
        Presets.all
            .map { preset -> preset.id }
            .distinct()
            .size shouldBe Presets.all.size
        Presets.images.map { preset -> preset.id } shouldBe listOf("res-0", "res-1", "res-2", "res-3", "res-4")
        Presets.starters.size shouldBe 8
    }

    /**
     * The picture in [bytes] at 128 px on its long edge, as ARGB pixels.
     */
    private fun sampleOf(bytes: ByteArray): PixelSample {
        val image = Image.makeFromEncoded(bytes)
        val scale = PIXEL_EDGE.toFloat() / max(image.width, image.height)
        val width = (image.width * scale).roundToInt().coerceAtLeast(1)
        val height = (image.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
        Canvas(bitmap).drawImageRect(
            image = image,
            src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
            dst = Rect.makeWH(width.toFloat(), height.toFloat()),
            samplingMode = FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
            paint = null,
            strict = true,
        )
        val bgra = requireNotNull(bitmap.readPixels()) { "Skia gave no pixels back" }
        val pixels = IntArray(width * height) { index ->
            val at = index * 4
            val blue = bgra[at].toInt() and 0xFF
            val green = bgra[at + 1].toInt() and 0xFF
            val red = bgra[at + 2].toInt() and 0xFF
            val alpha = bgra[at + 3].toInt() and 0xFF
            (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
        return PixelSample(pixels, width, height)
    }

    private fun List<Argb>.hexes(): List<String> = map { candidate -> candidate.toHex() }
}
