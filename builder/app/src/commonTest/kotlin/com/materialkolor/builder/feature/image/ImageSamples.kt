package com.materialkolor.builder.feature.image

import androidx.compose.ui.graphics.ImageBitmap
import com.materialkolor.builder.core.platform.DecodedImage

/** The four quadrant colors the browser tests draw, red, green, blue and amber. */
internal val QuadrantColors: List<Int> = listOf(0xFFD32F2F, 0xFF388E3C, 0xFF1976D2, 0xFFFFA000).map(Long::toInt)

/** Two close grays, which gray covers entirely. */
internal val GrayColors: List<Int> = listOf(0xFF808080, 0xFF7A7A7A).map(Long::toInt)

/** A decoded 16 px square in upright bands of [colors], left to right, with a one pixel thumbnail and detail. */
internal fun decodedOf(colors: List<Int>): DecodedImage {
    val side = 16
    val pixels = IntArray(side * side) { index -> colors[(index % side) * colors.size / side] }
    return DecodedImage(
        width = side,
        height = side,
        pixels = pixels,
        thumbnail = ImageBitmap(1, 1),
        detail = ImageBitmap(1, 1),
    )
}
