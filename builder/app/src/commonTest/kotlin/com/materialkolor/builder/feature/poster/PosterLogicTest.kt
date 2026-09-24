package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.InvalidReason
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_save_failed
import com.materialkolor.builder.generated.resources.poster_saved
import com.materialkolor.builder.generated.resources.poster_saving
import com.materialkolor.builder.generated.resources.poster_source_eyedropper
import com.materialkolor.builder.generated.resources.poster_source_image
import com.materialkolor.builder.generated.resources.poster_source_image_named
import com.materialkolor.builder.generated.resources.poster_source_picked
import com.materialkolor.builder.generated.resources.poster_source_preset
import com.materialkolor.builder.generated.resources.poster_source_shuffled
import com.materialkolor.builder.generated.resources.poster_source_typed
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PosterLogicTest {
    @Test
    fun kotlinLiteralOf_aSeed_isAComposeColorWithFullAlpha() {
        kotlinLiteralOf(Argb(0x6750A4)) shouldBe "Color(0xFF6750A4)"
        kotlinLiteralOf(Argb(0x00000A)) shouldBe "Color(0xFF00000A)"
    }

    @Test
    fun hctReadout_valuesBetweenWholeNumbers_roundToTheNearest() {
        HctReadout.of(hue = 281.4, chroma = 47.6, tone = 40.5) shouldBe HctReadout(281, 48, 41)
    }

    @Test
    fun hctReadout_aHueJustUnderAFullTurn_readsZero() {
        HctReadout.of(hue = 359.6, chroma = 10.0, tone = 50.0) shouldBe HctReadout(0, 10, 50)
    }

    @Test
    fun hctReadout_white_readsToneOneHundred() {
        HctReadout.of(Argb(0xFFFFFF)).tone shouldBe 100
    }

    @Test
    fun sourceLabel_everySource_namesWhereTheSeedCameFrom() {
        sourceLabel(SeedSource.Typed) shouldBe SourceLabel(Res.string.poster_source_typed)
        sourceLabel(SeedSource.Picked) shouldBe SourceLabel(Res.string.poster_source_picked)
        sourceLabel(SeedSource.Eyedropper) shouldBe SourceLabel(Res.string.poster_source_eyedropper)
        sourceLabel(SeedSource.Shuffled) shouldBe SourceLabel(Res.string.poster_source_shuffled)
        sourceLabel(SeedSource.Preset("ocean")) shouldBe SourceLabel(Res.string.poster_source_preset)
    }

    @Test
    fun sourceLabel_imageWithAName_namesTheFile() {
        sourceLabel(SeedSource.Image("sunset.png")) shouldBe
            SourceLabel(Res.string.poster_source_image_named, "sunset.png")
    }

    @Test
    fun sourceLabel_imageWithoutAName_saysItWasAnImage() {
        sourceLabel(SeedSource.Image(" ")) shouldBe SourceLabel(Res.string.poster_source_image)
    }

    @Test
    fun isLocked_eachLock_readsItsOwnPreference() {
        val prefs = Preferences(hueLock = true, styleLock = false, seedLock = true)

        prefs.isLocked(ShuffleLock.Hue) shouldBe true
        prefs.isLocked(ShuffleLock.Style) shouldBe false
        prefs.isLocked(ShuffleLock.Seed) shouldBe true
    }

    @Test
    fun shufflesNothing_everyLockCombination_isTrueOnlyWithTheSeedAndStyleLocked() {
        for (hue in listOf(false, true)) {
            for (style in listOf(false, true)) {
                for (seed in listOf(false, true)) {
                    val prefs = Preferences(hueLock = hue, styleLock = style, seedLock = seed)
                    prefs.shufflesNothing() shouldBe (seed && style)
                }
            }
        }
    }

    @Test
    fun shufflesNothing_aFreshBrowser_stillShuffles() {
        Preferences().shufflesNothing() shouldBe false
    }

    @Test
    fun hexMessages_noteOf_picksTheWordsForEachSet() {
        val messages = HexMessages(
            errors = InvalidReason.entries.associateWith { reason -> reason.name },
            alpha = "alpha",
            clamped = "clamped",
            both = "both",
        )

        messages.noteOf(setOf(ParseNote.AlphaDropped)) shouldBe "alpha"
        messages.noteOf(setOf(ParseNote.Clamped)) shouldBe "clamped"
        messages.noteOf(setOf(ParseNote.AlphaDropped, ParseNote.Clamped)) shouldBe "both"
        messages.errorOf(InvalidReason.BadHex) shouldBe "BadHex"
    }

    @Test
    fun saveStatusLabel_eachStatus_saysWhetherTheProjectIsSaved() {
        saveStatusLabel(SaveStatus.Idle) shouldBe Res.string.poster_saved
        saveStatusLabel(SaveStatus.Pending) shouldBe Res.string.poster_saving
        saveStatusLabel(SaveStatus.Failed(StoreError.QuotaExceeded)) shouldBe Res.string.poster_save_failed
    }

    @Test
    fun infoTopic_everyTopic_hasItsOwnDocsPageAndWords() {
        val topics = InfoTopic.entries

        topics.map { topic -> topic.docsUrl }.shouldBeUnique()
        topics.map { topic -> topic.question }.shouldBeUnique()
        topics.map { topic -> topic.explanation }.shouldBeUnique()
        topics.forEach { topic -> topic.docsUrl.startsWith("https://docs.materialkolor.com/") shouldBe true }
    }
}
