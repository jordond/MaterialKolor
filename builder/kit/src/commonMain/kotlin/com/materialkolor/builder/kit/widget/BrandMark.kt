package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.from
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette

/**
 * The MaterialKolor mark, a K drawn in three tones of one color. The stem is the strongest tone and
 * each arm steps one tone further away from it, the way a tonal palette steps.
 *
 * It is only drawn. The wordmark or the project name beside it already says what it is.
 *
 * @param[colors] The three tones, from [MarkColors.inked] or [MarkColors.tonal].
 * @param[modifier] Applied to the mark.
 * @param[size] The width and height of the square the mark fills.
 */
@Composable
public fun BrandMark(
    colors: MarkColors,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / MarkSpan
        val stroke = MarkStroke * unit
        drawLine(colors.lower, markPoint(24f, 32f, unit), markPoint(46f, 52f, unit), stroke, StrokeCap.Round)
        drawLine(colors.upper, markPoint(24f, 32f, unit), markPoint(46f, 12f, unit), stroke, StrokeCap.Round)
        drawLine(colors.stem, markPoint(18f, 12f, unit), markPoint(18f, 52f, unit), stroke, StrokeCap.Round)
    }
}

/**
 * Where the point [x], [y] of the mark's own units lands in a mark drawn at [unit] pixels a unit.
 */
private fun markPoint(
    x: Float,
    y: Float,
    unit: Float,
): Offset = Offset((x - MarkOrigin) * unit, (y - MarkOrigin) * unit)

/**
 * The three tones of [BrandMark].
 *
 * @property[stem] The upright, the strongest tone.
 * @property[upper] The upper arm, one step away from the stem.
 * @property[lower] The lower arm, one step further.
 */
@Immutable
public data class MarkColors(
    public val stem: Color,
    public val upper: Color,
    public val lower: Color,
) {
    public companion object {
        /**
         * The mark in [ink] alone, its arms stepping toward [page]. For a surface such as the poster,
         * where the ink is the one color sure to read on any seed.
         */
        public fun inked(
            ink: Color,
            page: Color,
        ): MarkColors =
            MarkColors(
                stem = ink,
                upper = lerp(page, ink, UpperInk),
                lower = lerp(page, ink, LowerInk),
            )

        /**
         * The mark in the tones of [seed], as the logo draws it. On a light [page] the stem is tone 40
         * and the arms tones 60 and 80, and a dark page turns that around.
         *
         * @param[seed] The seed as an ARGB int.
         * @param[page] What the mark stands on, which picks the direction the tones run.
         */
        public fun tonal(
            seed: Int,
            page: Color,
        ): MarkColors {
            val ramp = TonalPalette.from(seed)
            val dark = page.luminance() < DarkPage
            return MarkColors(
                stem = ramp.toneColor(if (dark) LightTone else DarkTone),
                upper = ramp.toneColor(MiddleTone),
                lower = ramp.toneColor(if (dark) DarkTone else LightTone),
            )
        }
    }
}

// The geometry of art/materialkolor-mark.svg, whose view box starts at 6 and spans 52 units.
private const val MarkOrigin = 6f
private const val MarkSpan = 52f
private const val MarkStroke = 12f

private const val UpperInk = 0.72f
private const val LowerInk = 0.48f

private const val DarkTone = 40
private const val MiddleTone = 60
private const val LightTone = 80
private const val DarkPage = 0.5f
