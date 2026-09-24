package com.materialkolor.builder.core.platform

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.persist.Appearance
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-501b
// boot.js reads this text before any app code runs, so its shape is held here to the letter.
class BootSplashTest {
    @Test
    fun toJson_writesEachColorAsSignedArgb() {
        val splash = BootSplash(WHITE, BLACK, DEFAULT_SEED, Appearance.Dark)

        splash.toJson() shouldBe """{"light":-1,"dark":-16777216,"seed":-2529989,"appearance":"dark"}"""
    }

    @Test
    fun toJson_namesEachAppearanceInLowerCase() {
        val names = Appearance.entries.map { appearance ->
            BootSplash(WHITE, BLACK, DEFAULT_SEED, appearance).toJson().substringAfter("\"appearance\":")
        }

        names shouldBe listOf("\"system\"}", "\"light\"}", "\"dark\"}")
    }

    private companion object {
        val WHITE = Argb(0xFFFFFFFF.toInt())
        val BLACK = Argb(0xFF000000.toInt())
    }
}
