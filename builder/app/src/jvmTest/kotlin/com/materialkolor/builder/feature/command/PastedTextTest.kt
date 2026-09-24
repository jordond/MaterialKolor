package com.materialkolor.builder.feature.command

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-315
class PastedTextTest {
    private val code = ShareCodec.encode(ThemeDocument.Default, projectName = "Mine")

    @Test
    fun classify_readsEachKindOfText() {
        val table = mapOf(
            "#6750A4" to PastedText.Color(Argb(0xFF6750A4.toInt())),
            "  #6750a4\n" to PastedText.Color(Argb(0xFF6750A4.toInt())),
            "rgb(103, 80, 164)" to PastedText.Color(Argb(0xFF6750A4.toInt())),
            "https://materialkolor.com/t/$code" to PastedText.Share(code),
            "http://localhost:8765/t/$code/" to PastedText.Share(code),
            "materialkolor.com/t/$code" to PastedText.Share(code),
            "https://example.org/t/abc?utm=1" to PastedText.Share("abc"),
            code to PastedText.Share(code),
            "Vibrant" to PastedText.Style(Style.Vibrant),
            "tonal spot" to PastedText.Style(Style.TonalSpot),
            "TONAL_SPOT" to PastedText.Style(Style.TonalSpot),
            "Tonal Spot" to PastedText.Style(Style.TonalSpot),
            "" to null,
            "plain words here" to null,
            "https://materialkolor.com/?seed=6750A4" to null,
            "https://materialkolor.com/about" to null,
            "notacode" to null,
        )

        table.forEach { (text, expected) -> (text to classify(text)) shouldBe (text to expected) }
    }

    @Test
    fun classify_matchesTheShownStyleName() {
        classify("Loud", mapOf(Style.Vibrant to "Loud")) shouldBe PastedText.Style(Style.Vibrant)
    }

    @Test
    fun classify_letsAColorWinATie() {
        // "red" names a color, so it sets the seed even with a style shown under that name.
        classify("red", mapOf(Style.Vibrant to "Red")) shouldBe PastedText.Color(Argb(0xFFFF0000.toInt()))
    }
}
