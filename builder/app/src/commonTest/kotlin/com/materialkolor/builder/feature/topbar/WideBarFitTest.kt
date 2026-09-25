package com.materialkolor.builder.feature.topbar

import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-512

class WideBarFitTest {
    @Test
    fun refit_rowShortOfRoom_goesCompactAndOpensOutOnlyOnceTheRowFitsBesideTheWideForms() {
        val fit = WideBarFit()

        fit.refit(needed = 575, room = 545, shownCompact = false)
        fit.compact shouldBe true

        // The compact bar frees 150 px, so the row fits, 120 px to spare.
        fit.refit(needed = 575, room = 695, shownCompact = true)
        fit.compact shouldBe true

        // Opening out takes the 150 px back, so a row with less to spare than that stays put.
        fit.refit(needed = 575, room = 724, shownCompact = true)
        fit.compact shouldBe true
        fit.refit(needed = 575, room = 725, shownCompact = true)
        fit.compact shouldBe false
    }

    @Test
    fun refit_fromABarThatHasNotCaughtUp_changesNothing() {
        val fit = WideBarFit()

        fit.refit(needed = 575, room = 545, shownCompact = false)
        fit.refit(needed = 575, room = 545, shownCompact = false)

        fit.compact shouldBe true
    }
}
