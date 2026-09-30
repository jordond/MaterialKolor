package com.materialkolor.builder.engine.poster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.toDomain
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.from
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toHct
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import kotlin.math.roundToInt

/**
 * The colors of the seed poster, all cut from one ramp built on the seed.
 *
 * The page is the exact seed. Ink is the ramp's tone 10, or its tone 95 when that reads better on
 * the seed. Outline is the ramp's `onTone` at the page's rounded tone and 3 to 1, the nearest tone
 * that reads as a shape. Muted ink sits halfway between ink and outline, good for large or
 * secondary text. Each of those is then measured against the exact page, since the ramp's own
 * tone can sit a little off it, and falls back to black or white when it misses.
 *
 * On a mid tone seed ink sits close to 4.5 to 1, so a surface that stepped toward it would drop it
 * under. Both surfaces start at the page's own tone and step in the direction away from the ink,
 * raised further than sunken, and each is measured against the ink. One that misses falls back to
 * the page itself.
 *
 * The [inverse] turns the poster over for a sheet that stands on it. Its page is the poster's ink,
 * and its colors are cut from the same ramp the same way.
 *
 * One ramp per seed means a seed drag re-tints the whole poster in step with the seed.
 *
 * @property[seed] The seed the poster is cut from, the page itself unless this is the [inverse].
 * @property[background] The page, the exact seed or, on the [inverse], the poster's ink.
 * @property[ramp] The ramp every poster color is cut from, which Fluent turns into its shades.
 * @property[isLight] Whether the page is light, so ink is darker than the page.
 * @property[ink] Text and icons on the page, 4.5 to 1 or more against it.
 * @property[inkMuted] Quieter text on the page, 3 to 1 or more against it.
 * @property[raised] A surface that stands off the page, which ink still reads on.
 * @property[sunken] A surface that sits just off the page, which ink still reads on.
 * @property[outline] Borders and dividers on the page, 3 to 1 or more against it.
 */
@Immutable
public class PosterColors private constructor(
    public val seed: Argb,
    public val background: Argb,
    public val ramp: TonalPalette,
    public val isLight: Boolean,
    public val ink: Argb,
    public val inkMuted: Argb,
    public val raised: Argb,
    public val sunken: Argb,
    public val outline: Argb,
    private val turnedFrom: PosterColors?,
) {
    private val turned: PosterColors by lazy { turnedFrom ?: inverseOf(this) }

    /**
     * The poster turned over, for a sheet that stands on it.
     *
     * The page is this poster's ink, and the ink is the ramp's tone 90, or its tone 10 when that
     * reads better, measured for 4.5 to 1 on that page. Everything else is cut the way [of] cuts it.
     * The inverse of the inverse is this poster again.
     */
    public fun inverse(): PosterColors = turned

    public companion object {
        /**
         * Work out the poster for [seed].
         */
        public fun of(seed: Argb): PosterColors {
            val ramp = TonalPalette.from(seed.value)
            return paint(seed, ramp, seed.toColor(), INK_DARK_TONE, INK_LIGHT_TONE, turnedFrom = null)
        }

        private fun inverseOf(poster: PosterColors): PosterColors =
            paint(
                seed = poster.seed,
                ramp = poster.ramp,
                page = poster.ink.toColor(),
                darkTone = INK_DARK_TONE,
                lightTone = INVERSE_INK_LIGHT_TONE,
                turnedFrom = poster,
            )

        /**
         * Cuts every color for [page] from [ramp], the ink from [darkTone] or [lightTone],
         * whichever reads better on it.
         */
        private fun paint(
            seed: Argb,
            ramp: TonalPalette,
            page: Color,
            darkTone: Int,
            lightTone: Int,
            turnedFrom: PosterColors?,
        ): PosterColors {
            val pageTone = page.tone
            val tone = pageTone.roundToInt()

            val inkCandidate = page.better(ramp.toneColor(darkTone), ramp.toneColor(lightTone))
            val ink = page.readable(inkCandidate, TEXT_RATIO)
            val outline = page.readable(ramp.onTone(tone, ContrastThreshold.WcagAaLargeText), SHAPE_RATIO)
            val mutedTone = (ink.tone.roundToInt() + outline.tone.roundToInt()) / 2
            val inkMuted = page.readable(ramp.toneColor(mutedTone), SHAPE_RATIO)

            val isLight = ink.tone < pageTone
            val away = if (isLight) 1 else -1
            val raised = page.surface(ramp.toneColor(step(tone, RAISED_STEP * away)), ink)
            val sunken = page.surface(ramp.toneColor(step(tone, SUNKEN_STEP * away)), ink)

            return PosterColors(
                seed = seed,
                background = page.toDomain(),
                ramp = ramp,
                isLight = isLight,
                ink = ink.toDomain(),
                inkMuted = inkMuted.toDomain(),
                raised = raised.toDomain(),
                sunken = sunken.toDomain(),
                outline = outline.toDomain(),
                turnedFrom = turnedFrom,
            )
        }

        /**
         * [candidate] when it reaches [ratio] on this page, or black or white, whichever reads better.
         */
        private fun Color.readable(
            candidate: Color,
            ratio: Double,
        ): Color = if (candidate.contrastRatio(this) >= ratio) candidate else better(Color.Black, Color.White)

        /**
         * Whichever of [dark] and [light] reads better on this page, [dark] on a tie.
         */
        private fun Color.better(
            dark: Color,
            light: Color,
        ): Color = if (dark.contrastRatio(this) >= light.contrastRatio(this)) dark else light

        /**
         * [candidate] when [ink] still reads on it, or this page.
         */
        private fun Color.surface(
            candidate: Color,
            ink: Color,
        ): Color = if (ink.contrastRatio(candidate) >= TEXT_RATIO) candidate else this

        private fun step(
            tone: Int,
            by: Int,
        ): Int = (tone + by).coerceIn(MIN_TONE, MAX_TONE)

        private val Color.tone: Double
            get() = toHct().tone
    }
}

private const val INK_DARK_TONE = 10
private const val INK_LIGHT_TONE = 95
private const val INVERSE_INK_LIGHT_TONE = 90
private const val TEXT_RATIO = 4.5
private const val SHAPE_RATIO = 3.0
private const val RAISED_STEP = 8
private const val SUNKEN_STEP = 4
private const val MIN_TONE = 0
private const val MAX_TONE = 100
