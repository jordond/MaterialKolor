package com.materialkolor.builder.kit.widget

import androidx.compose.ui.graphics.ImageBitmap
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KeptPicturesTest {
    @BeforeTest
    fun setUp() {
        KeptPictures.clear()
    }

    @AfterTest
    fun tearDown() {
        KeptPictures.clear()
    }

    @Test
    fun keep_pastKeptHues_forgetsTheOldestHue() {
        val pictures = (0..KeptHues).map { hue -> picture(hue) }
        pictures.forEach { picture -> KeptPictures.keep(picture) }

        KeptPictures.take(0).shouldBeNull()
        for (hue in 1..KeptHues) {
            withClue("hue $hue") { KeptPictures.take(hue) shouldBeSameInstanceAs pictures[hue] }
        }
    }

    @Test
    fun take_thenKeepPastKeptHues_forgetsTheLeastRecentInstead() {
        val pictures = (0 until KeptHues).map { hue -> picture(hue) }
        pictures.forEach { picture -> KeptPictures.keep(picture) }

        KeptPictures.take(0) shouldBeSameInstanceAs pictures[0]
        KeptPictures.keep(picture(KeptHues))

        KeptPictures.take(1).shouldBeNull()
        KeptPictures.take(0) shouldBeSameInstanceAs pictures[0]
    }

    @Test
    fun clear_afterKeep_forgetsEveryHue() {
        KeptPictures.keep(picture(7))

        KeptPictures.clear()

        KeptPictures.take(7).shouldBeNull()
    }

    private fun picture(hue: Int): PlanePicture =
        PlanePicture(hue = hue, span = 0.0, edges = DoubleArray(0), image = ImageBitmap(1, 1))
}
