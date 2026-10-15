package com.materialkolor.compat

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round

/**
 * The 48-bit sequence and bounded rejection sampling specified by java.util.Random.
 */
internal class JavaRandom(
    seed: Long,
) {
    private var state = (seed xor 0x5DEECE66DL) and ((1L shl 48) - 1)

    // Mirrors the java.util.Random.next(bits)
    @Suppress("SameParameterValue")
    private fun next(bits: Int): Int {
        state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        return (state ushr (48 - bits)).toInt()
    }

    internal fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        if ((bound and -bound) == bound) return ((bound.toLong() * next(31)) shr 31).toInt()
        var bits: Int
        var value: Int
        do {
            bits = next(31)
            value = bits % bound
        } while (bits - value + bound - 1 < 0)
        return value
    }
}

internal fun toRadians(degrees: Double): Double = degrees * (PI / 180.0)

internal fun toDegrees(radians: Double): Double = radians * (180.0 / PI)

/**
 * Locale-independent diagnostic formatting. Round the scaled binary value to an even tenth
 * and preserve the sign of zero. Large values already have no fractional precision. This is a
 * deliberately small policy for contrast diagnostics, not a DecimalFormat implementation.
 */
internal fun formatContrast(value: Double): String {
    require(value.isFinite()) { "contrast diagnostics require a finite value" }
    val sign = if (value.toBits() < 0) "-" else ""
    val magnitude = abs(value)
    if (magnitude < 4503599627370496.0) {
        val tenths = round(magnitude * 10).toLong()
        return "$sign${tenths / 10}.${tenths % 10}"
    }
    val text = magnitude.toString().lowercase()
    val exponent = text.substringAfter('e', "0").toInt()
    val mantissa = text.substringBefore('e')
    val digits = mantissa.replace(".", "")
    val point = mantissa.substringBefore('.').length + exponent
    return sign + digits.padEnd(point, '0').take(point) + ".0"
}
