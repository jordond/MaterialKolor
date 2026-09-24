package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.hct.Hct
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Seed = Argb(0x6750A4)

private val Plain = ThemeDocument(seed = Seed)

private fun accent(name: String): Accent = Accent(name = name, seed = Argb(0x00897B))

class AccentNamesTest {
    @Test
    fun problems_fineName_isNone() {
        accentNameProblems(Plain.copy(accents = listOf(accent("brand"))), index = 0, name = "sale") shouldBe emptyList()
    }

    @Test
    fun problems_nameOfALaterColor_landsOnTheOneBeingRenamed() {
        val document = Plain.copy(accents = listOf(accent("brand"), accent("status")))

        accentNameProblems(document, index = 0, name = "status") shouldBe listOf(AccentNameProblem.Duplicate)
        accentNameProblems(document, index = 0, name = "Status") shouldBe listOf(AccentNameProblem.CaseClash)
    }

    @Test
    fun problems_oneForEachError() {
        val document = Plain.copy(accents = listOf(accent("brand")))

        accentNameProblems(document, index = 0, name = "1st") shouldBe listOf(AccentNameProblem.Invalid)
        accentNameProblems(document, index = 0, name = "class") shouldBe listOf(AccentNameProblem.Keyword)
        accentNameProblems(document, index = 0, name = "a".repeat(25)) shouldBe listOf(AccentNameProblem.TooLong)
        accentNameProblems(document, index = 0, name = "Primary") shouldBe listOf(AccentNameProblem.Role)
        accentNameProblems(document, index = 0, name = "remember") shouldBe listOf(AccentNameProblem.Taken)
    }

    @Test
    fun problems_theColorsOwnName_isFine() {
        val document = Plain.copy(accents = listOf(accent("brand"), accent("status")))

        accentNameProblems(document, index = 1, name = "status") shouldBe emptyList()
    }

    @Test
    fun newAccent_emptyTheme_isAccent1TurnedOffTheSeed() {
        val added = newAccent(Plain)

        added.name shouldBe "accent1"
        val seed = Hct.fromInt(Seed.value)
        val turned = Hct.fromInt(added.seed.value)
        turned.hue shouldBe ((seed.hue + 40.0) % 360.0 plusOrMinus 2.0)
        turned.tone shouldBe (seed.tone plusOrMinus 1.0)
    }

    @Test
    fun newAccent_takenNames_takesTheFirstFreeOne() {
        val document = Plain.copy(accents = listOf(accent("accent1"), accent("Accent2")))

        newAccent(document).name shouldBe "accent3"
    }

    @Test
    fun messages_joinOnePerProblem() {
        val messages = AccentNameMessages(AccentNameProblem.entries.associateWith { problem -> problem.name })

        messages.messageFor(emptyList()) shouldBe null
        messages.messageFor(listOf(AccentNameProblem.Keyword, AccentNameProblem.Taken)) shouldBe "Keyword Taken"
    }
}
