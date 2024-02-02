package com.materialkolor.ktx

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.temperature.TemperatureCache

/**
 * Design utilities using color temperature theory.
 *
 * Analogous colors, complementary color, and cache to efficiently, lazily, generate data for
 * calculations when needed.
 *
 * Create a cache that allows calculation of ex. complementary and analogous colors.
 *
 * @param[input] Color to find complement/analogous colors of. Any colors will have the same tone,
 * and chroma as the input color, modulo any restrictions due to the other hues having lower
 * limits on chroma.
 */
public fun TemperatureCache(input: Color): TemperatureCache {
    return TemperatureCache(input.toHct())
}

/**
 * Value representing cool-warm factor of a color.
 *
 * Values below 0 are considered cool, above, warm.
 *
 * Color science has researched emotion and harmony, which art uses to select colors. Warm-cool
 * is the foundation of analogous and complementary colors. See: - Li-Chen Ou's Chapter 19 in
 * Handbook of Color Psychology (2015). - Josef Albers' Interaction of Color chapters 19 and 21.
 *
 * Implementation of Ou, Woodcock and Wright's algorithm, which uses Lab/LCH color space.
 * Return value has these properties:
 *     - Values below 0 are cool, above 0 are warm.<br></br>
 *     - Lower bound: -9.66. Chroma is infinite. Assuming max of Lab chroma 130.
 *     - Upper bound: 8.61. Chroma is infinite. Assuming max of Lab chroma 130.
 *
 * @param[color] The color to calculate the cool-warm factor of.
 * @return below 0 is cool, above 0 is warm.
 */
public fun TemperatureCache.Companion.temperature(color: Color): Double {
    return rawTemperature(color.toHct())
}

/**
 * Determine a given color is warm.
 */
@Stable
public fun Color.isWarm(): Boolean {
    return TemperatureCache.temperature(this) > 0
}

/**
 * Determine a given color is cool.
 */
@Stable
public fun Color.isCool(): Boolean {
    return TemperatureCache.temperature(this) < 0
}