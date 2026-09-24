package com.materialkolor.sample.fluent.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.materialkolor.fluent.toFluentShades
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.from
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toHct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.sample.shared.model.TaskTag
import io.github.composefluent.Shades
import kotlin.math.roundToInt

@Immutable
internal data class Tint(
    val container: Color,
    val content: Color,
)

/**
 * The colors this sample needs that Fluent does not have, one ramp per task tag.
 *
 * @property[accentTarget] The accent ramp a seed fade is heading to. Once the fade settles it matches [accent].
 */
@Immutable
internal class SampleColors(
    private val accent: Shades,
    private val work: Shades,
    private val errand: Shades,
    private val isDark: Boolean,
    val accentTarget: Shades,
) {
    val accentTint: Tint = accent.tint(isDark)

    fun shades(tag: TaskTag): Shades =
        when (tag) {
            TaskTag.Personal -> accent
            TaskTag.Work -> work
            TaskTag.Errand -> errand
        }

    fun tint(tag: TaskTag): Tint = shades(tag).tint(isDark)
}

internal fun tagShades(
    accent: Shades,
    tag: TaskTag,
): Shades {
    // Personal uses the accent itself. Work and Errand turn its hue a third and two thirds of the way round the
    // wheel at the same chroma, so the three stay apart for every seed and move with it.
    val turn = when (tag) {
        TaskTag.Personal -> return accent
        TaskTag.Work -> FULL_TURN / 3
        TaskTag.Errand -> FULL_TURN * 2 / 3
    }
    val hct = accent.base.toHct()
    return TonalPalette.fromHueAndChroma((hct.hue + turn) % FULL_TURN, hct.chroma).toFluentShades()
}

/**
 * The tone from this color's own ramp that reads on top of it.
 *
 * It asks for the AAA ratio, so small labels stay easy to read. A mid tone that nothing in the ramp reaches that
 * ratio against gets the nearer end of the ramp instead.
 */
internal fun Color.readableOn(): Color {
    val hct = toHct()
    return TonalPalette.from(hct).onTone(
        tone = hct.tone.roundToInt(),
        threshold = ContrastThreshold.WCAG_AAA_NORMAL_TEXT,
    )
}

// Mirrors how Fluent colors accent text.
private fun Shades.tint(isDark: Boolean): Tint =
    if (isDark) {
        Tint(container = dark2, content = light3)
    } else {
        Tint(container = light3, content = dark2)
    }

private const val FULL_TURN = 360.0
