package com.materialkolor.builder.feature.topbar

import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LibraryPickTest {
    private val plain = ThemeDocument.Default.copy(style = Style.Vibrant, spec = SpecVersion.Spec2021)

    @Test
    fun libraryChoice_of_mapsMaterial3WithTheFlagToM3ExpressiveAndIgnoresTheFlagElsewhere() {
        LibraryChoice.of(plain) shouldBe LibraryChoice.M3
        LibraryChoice.of(plain.copy(expressive = true)) shouldBe LibraryChoice.M3Expressive
        LibraryChoice.of(plain.copy(library = Library.Fluent, expressive = true)) shouldBe LibraryChoice.Fluent
        LibraryChoice.entries.forEach { choice ->
            LibraryChoice.of(choice.library, choice.expressive) shouldBe choice
        }
    }

    @Test
    fun libraryPick_entering_setsTheExpressiveStyleAndMovesA2021SpecTo2025() {
        val entered = pick(LibraryChoice.M3Expressive, plain.copy(library = Library.Fluent))

        entered.library shouldBe Library.Material3
        entered.expressive shouldBe true
        entered.style shouldBe Style.Expressive
        entered.spec shouldBe SpecVersion.Spec2025
    }

    @Test
    fun libraryPick_entering_keepsASpecOtherThan2021() {
        pick(LibraryChoice.M3Expressive, plain.copy(spec = SpecVersion.Spec2026)).spec shouldBe SpecVersion.Spec2026
    }

    @Test
    fun libraryPick_enteringOnTheExpressiveStyle_movesOnlyTheLibrary() {
        val document = plain.copy(library = Library.Unstyled, style = Style.Expressive)

        pick(LibraryChoice.M3Expressive, document) shouldBe document.copy(library = Library.Material3, expressive = true)
    }

    @Test
    fun libraryPick_leaving_restoresTheStyleAndSpecFromBeforeTheEntry() {
        val entry = enter(plain)

        LibraryChoice.entries.filter { choice -> choice != LibraryChoice.M3Expressive }.forEach { choice ->
            val left = pick(choice, entry.after, listOf(entry))

            LibraryChoice.of(left) shouldBe choice
            left.style shouldBe Style.Vibrant
            left.spec shouldBe SpecVersion.Spec2021
        }
    }

    @Test
    fun libraryPick_leavingAfterAStylePickedOnM3Expressive_keepsThatStyle() {
        val entry = enter(plain)
        val restyled = step(entry.after, DocumentChange.SetStyle(Style.Rainbow))

        val left = pick(LibraryChoice.Fluent, restyled.after, listOf(entry, restyled))

        left.style shouldBe Style.Rainbow
        left.spec shouldBe SpecVersion.Spec2025
    }

    @Test
    fun libraryPick_leavingWithNoRecord_setsTonalSpotAndKeepsTheSpec() {
        val opened = plain.copy(expressive = true, style = Style.Expressive, spec = SpecVersion.Spec2025)

        val left = pick(LibraryChoice.M3, opened)

        left.style shouldBe Style.TonalSpot
        left.spec shouldBe SpecVersion.Spec2025
    }

    @Test
    fun libraryPick_leavingAfterALinkOpenedOnM3Expressive_hasNoRecord() {
        val entry = enter(plain)
        val away = step(entry.after, libraryPick(LibraryChoice.Fluent, entry.after, listOf(entry)))
        val link = away.after.copy(library = Library.Material3, expressive = true, style = Style.Expressive)
        val pasted = step(away.after, DocumentChange.Replace(link))

        pick(LibraryChoice.M3, link, listOf(entry, away, pasted)).style shouldBe Style.TonalSpot
    }

    @Test
    fun libraryPick_betweenOtherChoices_movesOnlyTheLibrary() {
        pick(LibraryChoice.Custom, plain) shouldBe plain.copy(library = Library.Custom)
        pick(LibraryChoice.M3, plain.copy(library = Library.Fluent)) shouldBe plain
    }

    private fun enter(document: ThemeDocument): HistoryEntry =
        step(document, libraryPick(LibraryChoice.M3Expressive, document, emptyList()))

    private fun step(
        before: ThemeDocument,
        change: DocumentChange,
    ): HistoryEntry = HistoryEntry(before = before, after = change.apply(before), label = change.label)

    private fun pick(
        choice: LibraryChoice,
        document: ThemeDocument,
        steps: List<HistoryEntry> = emptyList(),
    ): ThemeDocument = libraryPick(choice, document, steps).apply(document)
}
