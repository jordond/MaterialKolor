package com.materialkolor.builder.feature.topbar

import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExpressiveSuggestionTest {
    @Test
    fun suggestsExpressiveStyle_onThe2021Spec_holds() {
        suggestsExpressiveStyle(on(Style.Expressive, SpecVersion.Spec2021)) shouldBe true
    }

    @Test
    fun suggestsExpressiveStyle_withAStyleOutsideThe2025Set_holds() {
        suggestsExpressiveStyle(on(Style.Rainbow, SpecVersion.Spec2025)) shouldBe true
        suggestsExpressiveStyle(on(Style.Cmf, SpecVersion.Spec2026)) shouldBe true
    }

    @Test
    fun suggestsExpressiveStyle_withA2025StyleOnThe2025Spec_staysQuiet() {
        suggestsExpressiveStyle(on(Style.Expressive, SpecVersion.Spec2025)) shouldBe false
        suggestsExpressiveStyle(on(Style.Vibrant, SpecVersion.Spec2026)) shouldBe false
    }

    @Test
    fun expressiveStyleChange_apply_setsStyleAndSpecAndNothingElse() {
        val document = ThemeDocument.Default.copy(library = Library.Material3, expressive = true, style = Style.Rainbow)

        val applied = expressiveStyleChange(document).apply(document)

        applied shouldBe document.copy(style = Style.Expressive, spec = SpecVersion.Spec2025)
        expressiveStyleChange(document).merges shouldBe false
    }

    @Test
    fun libraryChoice_change_setsLibraryAndFlagTogether() {
        LibraryChoice.Expressive.change shouldBe DocumentChange.SetLibrary(Library.Material3, expressive = true)
        val expressive = ThemeDocument.Default.copy(library = Library.Material3, expressive = true)
        LibraryChoice.of(expressive) shouldBe LibraryChoice.Expressive
        LibraryChoice.entries.forEach { choice ->
            LibraryChoice.of(choice.change.apply(ThemeDocument.Default)) shouldBe choice
        }
    }

    private fun on(
        style: Style,
        spec: SpecVersion,
    ): ThemeDocument = ThemeDocument.Default.copy(style = style, spec = spec)
}
