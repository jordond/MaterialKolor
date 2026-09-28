package com.materialkolor.builder.kit.headless

import androidx.compose.ui.input.key.Key

/**
 * The stop [key] moves roving focus to, from [index] in a row of [count] stops, or null for a key
 * that does not move it. The radio group and both tab rows read their arrow keys through it.
 *
 * Left and right follow the screen, so Right always moves to the stop on its right in either direction.
 * Up and down step back and forth when [upDown] is set, and Home and End jump to either end when
 * [homeEnd] is set. Every step wraps at the ends.
 *
 * A stop that [allowed] turns down is stepped over in the direction the key moves, and Home and End
 * land on the first and last stop it allows. When it allows no other stop the key stays on [index].
 */
internal fun rovingTarget(
    key: Key,
    index: Int,
    count: Int,
    rtl: Boolean,
    upDown: Boolean,
    homeEnd: Boolean,
    allowed: (Int) -> Boolean = { true },
): Int? {
    if (count <= 0) return null
    val forward = if (rtl) -1 else 1
    val (start, step) = when {
        key == Key.DirectionRight -> index + forward to forward
        key == Key.DirectionLeft -> index - forward to -forward
        upDown && key == Key.DirectionDown -> index + 1 to 1
        upDown && key == Key.DirectionUp -> index - 1 to -1
        homeEnd && key == Key.MoveHome -> 0 to 1
        homeEnd && key == Key.MoveEnd -> count - 1 to -1
        else -> return null
    }
    return (0 until count)
        .asSequence()
        .map { offset -> (start + step * offset).mod(count) }
        .firstOrNull { stop -> stop == index || allowed(stop) }
}
