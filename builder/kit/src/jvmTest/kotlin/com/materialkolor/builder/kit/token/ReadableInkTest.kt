package com.materialkolor.builder.kit.token

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ReadableInkTest {
    @Test
    fun readableInk_atToneFifty_isBlack() {
        // The darkest sRGB grey that reaches tone 50, a hair over it.
        val grey = Color(0xFF777777)
        val tone = Hct.fromInt(grey.toArgb()).tone
        tone shouldBeGreaterThanOrEqual 50.0
        tone shouldBeLessThan 50.1

        readableInk(grey) shouldBe Color.Black
    }

    @Test
    fun readableInk_justUnderToneFifty_isWhite() {
        // One step darker than the grey at tone 50.
        val grey = Color(0xFF767676)
        val tone = Hct.fromInt(grey.toArgb()).tone
        tone shouldBeLessThan 50.0
        tone shouldBeGreaterThanOrEqual 49.5

        readableInk(grey) shouldBe Color.White
    }
}
