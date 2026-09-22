package com.materialkolor.sample.customtheme.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The seeds the golden table is pinned to.
 *
 * Two are enough. The table exists to catch an accidental change to a tone choice, not to cover the
 * seed space, which the contrast and symmetry tests already do.
 */
internal val GoldenSeeds: List<Color> = listOf(
    Color(0xFF6750A4),
    Color(0xFF8DB600),
)

/**
 * Render every slot of every golden theme as text, in the format the table is stored in.
 */
internal fun renderAppThemeGoldens(): String =
    GoldenSeeds
        .flatMap { seed ->
            listOf(false, true).map { isDark -> renderTheme(seed = seed, isDark = isDark) }
        }.joinToString(separator = "\n\n")

private fun renderTheme(
    seed: Color,
    isDark: Boolean,
): String {
    val mode = if (isDark) "dark" else "light"
    val header = "${seed.hex()}|$mode"
    val slots = appColors(seed = seed, isDark = isDark)
        .slots
        .entries
        .joinToString(separator = "\n") { entry -> "${entry.key}=${entry.value.hex()}" }

    return "$header\n$slots"
}

private fun Color.hex(): String = toArgb().toUInt().toString(radix = 16).padStart(length = 8, padChar = '0')
