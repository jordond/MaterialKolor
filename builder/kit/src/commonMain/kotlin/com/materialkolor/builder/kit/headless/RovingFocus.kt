package com.materialkolor.builder.kit.headless

import androidx.compose.ui.input.key.Key

/**
 * The stop [key] moves roving focus to, from [index] in a row of [count] stops, or null for a key
 * that does not move it. The radio group and both tab rows read their arrow keys through it.
 *
 * Left and right follow the reading direction, so Right moves toward the end of the row either way.
 * Up and down step back and forth when [upDown] is set, and Home and End jump to either end when
 * [homeEnd] is set. Every step wraps at the ends.
 */
internal fun rovingTarget(
    key: Key,
    index: Int,
    count: Int,
    rtl: Boolean,
    upDown: Boolean,
    homeEnd: Boolean,
): Int? {
    if (count <= 0) return null
    val forward = if (rtl) -1 else 1
    val target = when {
        key == Key.DirectionRight -> index + forward
        key == Key.DirectionLeft -> index - forward
        upDown && key == Key.DirectionDown -> index + 1
        upDown && key == Key.DirectionUp -> index - 1
        homeEnd && key == Key.MoveHome -> 0
        homeEnd && key == Key.MoveEnd -> count - 1
        else -> return null
    }
    return target.mod(count)
}
