package com.materialkolor.builder.engine.image

import com.kmpalette.palette.graphics.Palette
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.toDomain
import com.materialkolor.hct.Hct
import com.materialkolor.palette.themeColorOrNull
import com.materialkolor.palette.themeColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * What a picture offers as seeds.
 *
 * @property[candidates] Up to [SeedExtractor.MAX_CANDIDATES] seeds, the best first. There is
 * always at least one.
 * @property[mostlyGray] Whether the picture is mostly gray, so the seed panel can say why its
 * candidates look muted.
 */
public data class ImageSeeds(
    public val candidates: ImmutableList<Argb>,
    public val mostlyGray: Boolean,
)

/**
 * Pulls ranked seed candidates out of a picture.
 *
 * kmpalette quantizes the pixels with its default settings, the same way a theme seeded from an
 * image at runtime does, and `material-kolor-palette` scores the swatches for how well each would
 * seed a theme. It runs straight through on the calling thread, so the caller decides when to
 * yield around it.
 *
 * kmpalette's default filter drops near black, near white and the colors along the red I line,
 * the browns, tans and skin tones of hue 10 to 37. When that leaves nothing worth scoring the
 * pixels are quantized again with only near black and near white dropped, so a picture of wood
 * or skin still offers its own colors.
 */
public object SeedExtractor {
    /** The most candidates one picture offers, which is what the seed panel has room for. */
    public const val MAX_CANDIDATES: Int = 5

    /**
     * The seeds [sample] offers.
     *
     * A picture with no color worth scoring still offers its own best grays. Only a picture with
     * nothing left once near black and near white are dropped gives [fallback].
     */
    public fun extract(
        sample: PixelSample,
        fallback: Argb = DEFAULT_SEED,
    ): ImageSeeds {
        val runtime = palette(sample, Palette.DEFAULT_FILTER)
        val palette = if (runtime.themeColorOrNull() != null) runtime else palette(sample, NOT_BLACK_OR_WHITE)
        val colorful = palette.themeColorOrNull() != null
        val candidates = palette
            .themeColors(fallback = fallback.toColor(), desired = MAX_CANDIDATES, filter = colorful)
            .map { color -> color.toDomain() }
            .toImmutableList()
        return ImageSeeds(candidates = candidates, mostlyGray = isMostlyGray(sample))
    }

    /**
     * kmpalette quantized with [filter] alone. It writes its quantized colors back into the array
     * it is handed, so it gets a copy and [sample] stays as it came.
     */
    private fun palette(
        sample: PixelSample,
        filter: Palette.Filter,
    ): Palette =
        Palette
            .from(sample.pixels.copyOf(), sample.width, sample.height)
            .clearFilters()
            .addFilter(filter)
            .generate()

    /**
     * Whether gray covers most of [sample]'s own pixels, near black and near white included, so
     * the answer does not hang on what kmpalette's filter kept. A large sample is read at a stride
     * so no more pixels are looked at than kmpalette would quantize.
     */
    private fun isMostlyGray(sample: PixelSample): Boolean {
        val pixels = sample.pixels
        val stride = maxOf(1, pixels.size / Palette.DEFAULT_RESIZE_BITMAP_AREA)
        var read = 0
        var gray = 0
        for (index in pixels.indices step stride) {
            read++
            if (Hct.fromInt(pixels[index]).chroma < GRAY_CHROMA) gray++
        }
        return gray >= read * MOSTLY_GRAY_SHARE
    }

    /** kmpalette's default filter without the red I line, which drops only near black and near white. */
    private val NOT_BLACK_OR_WHITE = Palette.Filter { _, hsl ->
        hsl[LIGHTNESS] > NEAR_BLACK_LIGHTNESS && hsl[LIGHTNESS] < NEAR_WHITE_LIGHTNESS
    }

    /** Where lightness sits in the HSL array kmpalette hands a filter. */
    private const val LIGHTNESS: Int = 2

    /** At or under this HSL lightness kmpalette's default filter drops a color as near black. */
    private const val NEAR_BLACK_LIGHTNESS: Float = 0.05f

    /** At or over this HSL lightness kmpalette's default filter drops a color as near white. */
    private const val NEAR_WHITE_LIGHTNESS: Float = 0.95f

    /** Below this chroma a pixel reads as gray. */
    private const val GRAY_CHROMA: Double = 10.0

    /** The share of pixels that has to be gray for the picture to count as mostly gray. */
    private const val MOSTLY_GRAY_SHARE: Double = 0.8
}
