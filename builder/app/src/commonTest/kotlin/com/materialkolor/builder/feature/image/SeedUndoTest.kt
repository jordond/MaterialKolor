package com.materialkolor.builder.feature.image

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-311c

/**
 * The Undo on an image seed's toast ends as soon as the document is somewhere the seed did not put
 * it, even when no frame ever showed the seed itself (R-B-311a).
 */
class SeedUndoTest {
    private val before = ThemeDocument(seed = Argb(0xFF6750A4.toInt()))
    private val made = before.copy(
        seed = Argb(0xFF0000FF.toInt()),
        seedSource = SeedSource.Image("photo.png", listOf(Argb(0xFF0000FF.toInt()))),
    )
    private val moved = made.copy(contrast = ContrastLevel.High)
    private val undo = SeedUndo(before, made, project = 0)
    private var withdrawn = false

    init {
        undo.shown { withdrawn = true }
    }

    @Test
    fun seedUndo_whileTheSeedIsOnItsWayAndOnceItLands_holds() {
        undo.follow(before, project = 0)
        undo.follow(made, project = 0)

        withdrawn shouldBe false
        undo.holds(made, project = 0) shouldBe true
    }

    @Test
    fun seedUndo_editInTheSameFrameAsTheSeed_endsIt() {
        undo.follow(before, project = 0)

        undo.follow(moved, project = 0)

        withdrawn shouldBe true
        undo.holds(made, project = 0) shouldBe false
    }

    @Test
    fun seedUndo_laterPassBackThroughTheSeed_neverBringsItBack() {
        undo.follow(moved, project = 0)

        undo.follow(made, project = 0)

        withdrawn shouldBe true
        undo.holds(made, project = 0) shouldBe false
    }
}
