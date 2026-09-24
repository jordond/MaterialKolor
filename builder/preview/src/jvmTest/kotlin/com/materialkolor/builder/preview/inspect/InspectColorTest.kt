package com.materialkolor.builder.preview.inspect

import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.audit.FluentText
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Seed = Argb(0x6750A4)

class InspectColorTest {
    @Test
    fun accent_isNamedByItsAccentAndPart_andReadsTheToneItIsCutAt() {
        val result =
            resolve(ThemeDocument(seed = Seed, accents = listOf(Accent(name = "Brand", seed = Argb(0x00897B)))))
        val ref = ColorRef.OfAccent(AccentSlot(index = 0, part = AccentPart.Color))

        ref.inspectName(result.document) shouldBe "Brand color"
        val light = result.inspectColor(ref, isDark = false)
        light.name shouldBe "Brand color"
        light.argb shouldBe result.accents.families[0][AccentPart.Color, false]
        light.tone shouldBe 40
        result.inspectColor(ref, isDark = true).tone shouldBe 80
    }

    @Test
    fun accent_theDocumentNoLongerHas_isNamedByItsPartAlone_withNoColor() {
        val result = resolve(ThemeDocument(seed = Seed))
        val ref = ColorRef.OfAccent(AccentSlot(index = 2, part = AccentPart.OnContainer))

        ref.inspectName(result.document) shouldBe "onContainer"
        val color = result.inspectColor(ref, isDark = false)
        color.argb.shouldBeNull()
        color.tone.shouldBeNull()
    }

    @Test
    fun customSlot_isNamedInLowerCamelCase_andReadsTheToneItIsCutAt() {
        val result = resolve(ThemeDocument(seed = Seed, library = Library.Custom))
        val ref = ColorRef.OfSlot(CustomSlot.PrimaryPressed)

        ref.inspectName(result.document) shouldBe "primaryPressed"
        val light = result.inspectColor(ref, isDark = false)
        light.argb shouldBe result.customSlots[CustomSlot.PrimaryPressed, false]
        light.tone shouldBe 32
        result.inspectColor(ref, isDark = true).tone shouldBe 70
    }

    @Test
    fun fluentShade_isNamedInLowerCamelCase_andCutFromThePrimaryPalette() {
        val result = resolve(ThemeDocument(seed = Seed, library = Library.Fluent))
        val ref = ColorRef.OfFluentShade(FluentShade.Dark2)

        ref.inspectName(result.document) shouldBe "dark2"
        val color = result.inspectColor(ref, isDark = false)
        color.argb shouldBe Argb(result.scheme(isDark = false).primaryPalette.tone(30))
        color.tone shouldBe 30
    }

    @Test
    fun fluentText_isNamedInLowerCamelCase_andReadsAsTheAuditLaysItOverTheFill() {
        val result = resolve(ThemeDocument(seed = Seed, library = Library.Fluent))
        val ref = ColorRef.OfFluentText(FluentText.OnAccentPrimary)

        ref.inspectName(result.document) shouldBe "onAccentPrimary"
        val light = result.inspectColor(ref, isDark = false)
        light.argb shouldBe Argb(0xFFFFFF)
        light.tone shouldBe 100
        val dark = result.inspectColor(ref, isDark = true)
        dark.argb shouldBe Argb(0x000000)
        dark.tone shouldBe 0
    }

    private fun resolve(document: ThemeDocument): ThemeResult = ThemeResolver().resolve(document)
}
