package com.materialkolor.ktx

import androidx.compose.ui.graphics.ImageBitmap
import dev.drewhamilton.poko.Poko
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The pixel budget the image helpers quantize by default. Upstream Material Color Utilities
 * recommends resizing to 128 by 128 before quantizing.
 */
public const val DEFAULT_SAMPLE_AREA: Int = 128 * 128

/**
 * The pixels [samplePixels] kept, and the size of the grid they came back in.
 *
 * @property[pixels] ARGB pixels in row-major order, [width] times [height] of them.
 * @property[width] Width of the sampled grid, never wider than the source.
 * @property[height] Height of the sampled grid, never taller than the source.
 */
@Poko
public class SampledPixels(
    public val pixels: IntArray,
    public val width: Int,
    public val height: Int,
)

/**
 * Nearest-neighbour samples this bitmap down to at most [sampleArea] pixels.
 *
 * Reads only the rows it keeps, so a 12 megapixel photo costs about 128 row reads and one
 * 16k int array, never a full-size copy. Pass a [sampleArea] of zero or less to read every
 * pixel.
 *
 * Nearest neighbour is deliberate. Averaging neighbours would blend a vibrant pixel into the
 * dull ones beside it, and those vibrant pixels are exactly the ones a theme wants.
 *
 * @param[sampleArea] The most pixels to keep, or zero and below to keep them all.
 * @return ARGB pixels in row-major order, plus the sampled width and height.
 */
public fun ImageBitmap.samplePixels(sampleArea: Int = DEFAULT_SAMPLE_AREA): SampledPixels {
    val area = width * height
    if (sampleArea <= 0 || area <= sampleArea) {
        val pixels = IntArray(area)
        readPixels(buffer = pixels, startX = 0, startY = 0)
        return SampledPixels(pixels = pixels, width = width, height = height)
    }

    val scale = sqrt(sampleArea.toDouble() / area)
    val sampledWidth = ceil(width * scale).toInt().coerceAtLeast(1)
    val sampledHeight = ceil(height * scale).toInt().coerceAtLeast(1)

    val row = IntArray(width)
    val out = IntArray(sampledWidth * sampledHeight)
    for (y in 0 until sampledHeight) {
        readPixels(
            buffer = row,
            startX = 0,
            startY = (y * height) / sampledHeight,
            width = width,
            height = 1,
        )
        for (x in 0 until sampledWidth) {
            out[y * sampledWidth + x] = row[(x * width) / sampledWidth]
        }
    }

    return SampledPixels(pixels = out, width = sampledWidth, height = sampledHeight)
}
