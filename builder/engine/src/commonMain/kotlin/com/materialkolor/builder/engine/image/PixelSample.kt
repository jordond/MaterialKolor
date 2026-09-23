package com.materialkolor.builder.engine.image

/**
 * A downscaled picture as ARGB pixels, row by row from the top left, which is what seeds are
 * extracted from.
 *
 * The array is used as it is and never copied, since a sample runs to thousands of pixels. Hand
 * over one that nobody writes to afterwards.
 *
 * @property[pixels] Every pixel as an ARGB int, `width * height` of them.
 * @property[width] How many pixels one row holds.
 * @property[height] How many rows there are.
 */
public class PixelSample(
    public val pixels: IntArray,
    public val width: Int,
    public val height: Int,
) {
    init {
        require(width > 0 && height > 0) { "A pixel sample needs a size, got $width by $height" }
        require(pixels.size == width * height) {
            "A $width by $height sample holds ${width * height} pixels, got ${pixels.size}"
        }
    }
}
