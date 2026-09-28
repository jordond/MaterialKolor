package com.materialkolor.builder.kit.headless

import androidx.compose.ui.input.key.Key
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RovingTargetTest {
    /**
     * Three stops with the last one turned down, the spec choice on a style with a 2025 form.
     */
    private val firstTwo: (Int) -> Boolean = { stop -> stop < 2 }

    private fun target(
        key: Key,
        index: Int,
        rtl: Boolean = false,
        allowed: (Int) -> Boolean = firstTwo,
    ): Int? = rovingTarget(key, index, count = 3, rtl = rtl, upDown = true, homeEnd = true, allowed = allowed)

    @Test
    fun arrows_stepOverAStopThatIsTurnedDown_andWrap() {
        target(Key.DirectionRight, index = 1) shouldBe 0
        target(Key.DirectionLeft, index = 0) shouldBe 1
        target(Key.DirectionDown, index = 1) shouldBe 0
        target(Key.DirectionLeft, index = 1, rtl = true) shouldBe 0
    }

    @Test
    fun homeAndEnd_landOnTheFirstAndLastAllowedStop() {
        target(Key.MoveEnd, index = 0) shouldBe 1
        target(Key.MoveHome, index = 1, allowed = { stop -> stop > 0 }) shouldBe 1
    }

    @Test
    fun aLoneAllowedStop_keepsTheKeyWhereItIs() {
        val onlyLast: (Int) -> Boolean = { stop -> stop == 2 }

        target(Key.DirectionRight, index = 2, allowed = onlyLast) shouldBe 2
        target(Key.MoveHome, index = 2, allowed = onlyLast) shouldBe 2
    }

    @Test
    fun everyStopAllowed_movesTheWayItAlwaysHas() {
        target(Key.DirectionRight, index = 2, allowed = { true }) shouldBe 0
        target(Key.MoveEnd, index = 0, allowed = { true }) shouldBe 2
        target(Key.Tab, index = 0, allowed = { true }) shouldBe null
    }
}
