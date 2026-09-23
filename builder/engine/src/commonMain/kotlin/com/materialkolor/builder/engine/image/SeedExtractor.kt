package com.materialkolor.builder.engine.image

import com.kmpalette.palette.graphics.Palette
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.toDomain
import com.materialkolor.palette.themeColorOrNull
import com.materialkolor.palette.themeColors
import com.materialkolor.palette.toHct
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
 */
public object SeedExtractor {
    /** The most candidates one picture offers, which is what the seed panel has room for. */
    public const val MAX_CANDIDATES: Int = 5

    /**
     * The seeds [sample] offers.
     *
     * A picture with no color worth scoring still offers its own best grays. Only a picture with
     * nothing left after kmpalette drops the near black and near white pixels gives [fallback].
     */
    public fun extract(
        sample: PixelSample,
        fallback: Argb = DEFAULT_SEED,
    ): ImageSeeds {
        val palette = Palette.from(sample.pixels, sample.width, sample.height).generate()
        val colorful = palette.themeColorOrNull() != null
        val candidates = palette
            .themeColors(fallback = fallback.toColor(), desired = MAX_CANDIDATES, filter = colorful)
            .map { color -> color.toDomain() }
            .toImmutableList()
        return ImageSeeds(candidates = candidates, mostlyGray = isMostlyGray(palette))
    }

    /**
     * Whether gray swatches cover most of the pixels kmpalette kept. A picture it kept nothing of
     * was all near black or near white, which counts as gray too.
     */
    private fun isMostlyGray(palette: Palette): Boolean {
        val total = palette.swatches.sumOf { swatch -> swatch.population }
        if (total == 0) return true
        val gray = palette.swatches
            .filter { swatch -> swatch.toHct().chroma < GRAY_CHROMA }
            .sumOf { swatch -> swatch.population }
        return gray >= total * MOSTLY_GRAY_SHARE
    }

    /** Below this chroma a swatch reads as gray. */
    private const val GRAY_CHROMA: Double = 10.0

    /** The share of kept pixels that has to be gray for the picture to count as mostly gray. */
    private const val MOSTLY_GRAY_SHARE: Double = 0.8
}
