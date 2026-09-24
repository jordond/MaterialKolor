package com.materialkolor.builder.feature.command

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.DecodeResult
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
            // A code that reads counts at any length.
            SHORT_CODE to PastedText.Share(SHORT_CODE),
            // One from a newer builder counts only when it looks like a real code.
            NEWER_CODE to PastedText.Share(NEWER_CODE),
            "zzzzzzzzzzzzzzzzzz" to null,
            "zzzz9zzz" to null,
        )

        (ShareCodec.decode(SHORT_CODE) is DecodeResult.Ok) shouldBe true
        ShareCodec.decode(NEWER_CODE) shouldBe DecodeResult.UnknownVersion
        table.forEach { (text, expected) -> (text to classify(text)) shouldBe (text to expected) }
    }

    @Test
    fun classify_letsAStyleNameWinOverACodeFromANewerBuilder() {
        val name = "Z9StyleNameOfSixteen"
        ShareCodec.decode(name) shouldBe DecodeResult.UnknownVersion

        classify(name, mapOf(Style.Vibrant to name)) shouldBe PastedText.Style(Style.Vibrant)
        classify(name) shouldBe PastedText.Share(name)
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

/** A real code of twelve characters. */
private const val SHORT_CODE = "AdllOwAAAAAT"

/** Starts with a version byte past the one this builder writes. */
private const val NEWER_CODE = "_wAAAAAAAAAAAAAAAAAA"
