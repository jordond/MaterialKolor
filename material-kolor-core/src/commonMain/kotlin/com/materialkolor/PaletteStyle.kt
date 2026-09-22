package com.materialkolor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle.Companion.parse
import com.materialkolor.PaletteStyle.Companion.parseOrNull
import com.materialkolor.dynamiccolor.Variant
import com.materialkolor.serialization.PaletteStyleSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/**
 * The style of the palette to generate.
 *
 * Mapped to [Variant] in the Material Design guidelines.
 *
 * `toString()` is a stable round trip format you can put in a URL, a preference or a database
 * column, the way `Duration` and `Uuid` do it. `parse(style.toString())`
 * gives back an equal style for every style, and [parseOrNull] answers null instead of throwing
 * when the text came from somewhere else.
 *
 * Every style except [Cmf] writes its [name]. A [Cmf] with no tertiary seed color writes `"Cmf"`,
 * and one that carries a seed writes `Cmf:AARRGGBB`, eight uppercase hex digits read through
 * `toArgb`, so a wide gamut seed collapses to its sRGB eight bit form and comes back in sRGB.
 */
@Immutable
@Serializable(with = PaletteStyleSerializer::class)
public sealed interface PaletteStyle {
    /**
     * Stable identifier for the style, matching the entry names the old enum had. [Cmf] is always
     * `"Cmf"` no matter which seed it carries, so use `toString()` when the seed must survive.
     */
    public val name: String
        get() = when (this) {
            is Cmf -> "Cmf"
            is Content -> "Content"
            is Expressive -> "Expressive"
            is Fidelity -> "Fidelity"
            is FruitSalad -> "FruitSalad"
            is Monochrome -> "Monochrome"
            is Neutral -> "Neutral"
            is Rainbow -> "Rainbow"
            is TonalSpot -> "TonalSpot"
            is Vibrant -> "Vibrant"
        }

    /**
     * A calm theme, sedated colors that aren't particularly chromatic.
     */
    public data object TonalSpot : PaletteStyle

    /**
     * A theme that's slightly more chromatic than monochrome, which is purely black / white / gray.
     */
    public data object Neutral : PaletteStyle

    /**
     * A loud theme, colorfulness is maximum for Primary palette, increased for others.
     */
    public data object Vibrant : PaletteStyle

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    public data object Expressive : PaletteStyle

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    public data object Rainbow : PaletteStyle

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    public data object FruitSalad : PaletteStyle

    /**
     * A monochrome theme, colors are purely black / white / gray.
     */
    public data object Monochrome : PaletteStyle

    /**
     * A scheme that places the source color in Scheme.primaryContainer.
     *
     * Primary Container is the source color, adjusted for color relativity. It maintains constant
     * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
     * dark mode.
     *
     * Tertiary Container is the complement to the source color, using TemperatureCache. It also
     * maintains constant appearance.
     */
    public data object Fidelity : PaletteStyle

    /**
     * A scheme that places the source color in Scheme.primaryContainer.
     *
     * Primary Container is the source color, adjusted for color relativity. It maintains constant
     * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
     * dark mode.
     *
     * Tertiary Container is an analogous color, specifically, the analog of a color wheel divided
     * into 6, and the precise analog is the one found by increasing hue. This is a scientifically
     * grounded equivalent to rotating hue clockwise by 60 degrees. It also maintains constant
     * appearance.
     */
    public data object Content : PaletteStyle

    /**
     * A theme using the 2026 spec's CMF (Color, Material, Finish) variant, which accepts two seed
     * colors.
     *
     * [tertiarySeedColor] becomes the second entry of the scheme's `sourceColorHctList`, while the
     * primary seed stays the `seedColor` passed to the theme or to `toDynamicScheme`.
     *
     * The engine reads that second seed only for the tertiary palette's hue and chroma, the
     * tertiary container tone, and the error hue. Primary, secondary, neutral and neutral variant
     * palettes always come from the first seed, which is why the parameter is named for what it
     * changes rather than for its position. Leave it `null`, or pass the primary seed again, and
     * the tertiary palette falls back to the primary seed at reduced chroma.
     *
     * [Cmf] always produces a `SPEC_2026` scheme, so a requested `specVersion` is ignored.
     *
     * @param[tertiarySeedColor] An optional second seed color, used for the tertiary palette and
     * the error hue.
     */
    @Poko
    public class Cmf(
        public val tertiarySeedColor: Color? = null,
    ) : PaletteStyle {
        /**
         * `"Cmf"` on its own when there is no [tertiarySeedColor], otherwise `Cmf:AARRGGBB`.
         *
         * The seed is written through `toArgb`, so a wide gamut color collapses to its sRGB
         * eight bit form and comes back out of [parse] in sRGB.
         */
        override fun toString(): String {
            val seed = tertiarySeedColor ?: return name
            val argb = (seed.toArgb().toLong() and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')
            return "$name:$argb"
        }
    }

    public companion object {
        /**
         * Every style that can be listed without extra input, for menus, pickers and tests.
         *
         * [PaletteStyle] is a sealed interface rather than an enum, so it has no `entries`. [Cmf]
         * carries a color, so this list holds one with no tertiary seed color. Build your own
         * [Cmf] when you want to seed the tertiary palette.
         *
         * The list is built on first use. [PaletteStyle] carries default implementations, so a
         * platform may load the interface while it is still building one of the objects, and an
         * eager list would capture that half built object.
         */
        public val KnownStyles: List<PaletteStyle> by lazy {
            listOf(
                TonalSpot,
                Neutral,
                Vibrant,
                Expressive,
                Rainbow,
                FruitSalad,
                Monochrome,
                Fidelity,
                Content,
                Cmf(),
            )
        }

        /**
         * Find the style called [name]. The match is case-sensitive.
         *
         * `"Cmf"` gives back a [Cmf] with no tertiary seed color, since a name alone cannot carry
         * one. Use [parse] when the seed has to survive the trip.
         *
         * @throws NoSuchElementException if there is no style with the given [name].
         */
        public fun fromName(name: String): PaletteStyle = KnownStyles.first { style -> style.name == name }

        /**
         * Find the style called [name], or null when the name belongs to no style. The match is case-sensitive.
         *
         * `"Cmf"` gives back a [Cmf] with no tertiary seed color, since a name alone cannot carry
         * one. Use [parse] when the seed has to survive the trip.
         */
        public fun fromNameOrNull(name: String): PaletteStyle? = KnownStyles.firstOrNull { style -> style.name == name }

        /**
         * Read back a style written by `toString()`, or throw when [value] is not one we wrote.
         *
         * `parse(style.toString()) == style` holds for every style, including a [Cmf] that carries
         * a tertiary seed color. Accepted forms are the plain style names, `"Cmf"`, and `"Cmf:"`
         * followed by exactly eight hex digits in either case.
         *
         * @throws[IllegalArgumentException] when [value] is not a style we wrote.
         */
        public fun parse(value: String): PaletteStyle =
            parseOrNull(value) ?: throw IllegalArgumentException("Unknown PaletteStyle \"$value\"")

        /**
         * Read back a style written by `toString()`, or null when [value] is not one we wrote.
         *
         * The lenient twin of [parse], for text that arrives from a URL, a stored preference or a
         * user. The plain style names match case sensitively, while the eight hex digits of a
         * seeded [Cmf] read in either case. Anything else, a short seed or a signed one included,
         * is null.
         */
        public fun parseOrNull(value: String): PaletteStyle? =
            CMF_SEED_REGEX
                .matchEntire(value)
                ?.let { match -> Cmf(Color(match.groupValues[1].toUInt(radix = 16).toInt())) }
                ?: fromNameOrNull(value)

        private val CMF_SEED_REGEX = Regex("Cmf:([0-9a-fA-F]{8})")
    }
}
