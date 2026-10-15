package com.materialkolor.builder.domain.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ColorInputTest {
    @Test
    fun parse_everyAcceptedForm_readsTheColor() {
        val cases =
            listOf(
                "#abc" to ok("#AABBCC"),
                "abc" to ok("#AABBCC"),
                "#6750A4" to ok("#6750A4"),
                "6750a4" to ok("#6750A4"),
                "  #6750a4\t" to ok("#6750A4"),
                "#FF6750A4" to ok("#6750A4"),
                "FF6750A4" to ok("#6750A4"),
                "#806750A4" to ok("#6750A4", ParseNote.AlphaDropped),
                "0xFF6750A4" to ok("#6750A4"),
                "0X806750A4" to ok("#6750A4", ParseNote.AlphaDropped),
                "0x6750A4" to ok("#6750A4"),
                "Color(0xFF6750A4)" to ok("#6750A4"),
                "Color(0x6750A4)" to ok("#6750A4"),
                "  color( 0x806750a4 )  " to ok("#6750A4", ParseNote.AlphaDropped),
                "rgb(103, 80, 164)" to ok("#6750A4"),
                "rgb(103 80 164)" to ok("#6750A4"),
                "RGB( 103 ,80,164 )" to ok("#6750A4"),
                "rgba(103, 80, 164)" to ok("#6750A4"),
                "rgba(103, 80, 164, 1)" to ok("#6750A4"),
                "rgba(103, 80, 164, 0.5)" to ok("#6750A4", ParseNote.AlphaDropped),
                "rgb(103, 80, 164, 50%)" to ok("#6750A4", ParseNote.AlphaDropped),
                "rgb(103 80 164 / 50%)" to ok("#6750A4", ParseNote.AlphaDropped),
                "rgb(103 80 164/1)" to ok("#6750A4"),
                "rgb(40.4%, 31.4%, 64.3%)" to ok("#6750A4"),
                "rgb(100% 0% 50%)" to ok("#FF0080"),
                "rgb(1e2, 0, 0)" to ok("#640000"),
                "rgb(300, -20, 128)" to ok("#FF0080", ParseNote.Clamped),
                "rgb(110% 0% 50%)" to ok("#FF0080", ParseNote.Clamped),
                "rgba(300, 0, 128, 0)" to ok("#FF0080", ParseNote.AlphaDropped, ParseNote.Clamped),
                "hsl(0, 100%, 50%)" to ok("#FF0000"),
                "hsl(120deg 100% 25%)" to ok("#008000"),
                "hsl(120deg, 100%, 25%)" to ok("#008000"),
                "hsl(-120 100% 50%)" to ok("#0000FF"),
                "hsl(0.5turn 100% 50%)" to ok("#00FFFF"),
                "hsl(200grad 100% 50%)" to ok("#00FFFF"),
                "hsl(3.14159265rad 100% 50%)" to ok("#00FFFF"),
                "hsl(0 100 50)" to ok("#FF0000"),
                "hsla(240, 100%, 50%, 0.3)" to ok("#0000FF", ParseNote.AlphaDropped),
                "hsl(0 150% 50%)" to ok("#FF0000", ParseNote.Clamped),
                "oklch(0.5 0 0)" to ok("#636363"),
                "oklch(50% 0 0)" to ok("#636363"),
                "oklch(0.7 0.1 150)" to ok("#6FB07D"),
                "oklch(70% 25% 150deg)" to ok("#6FB07D"),
                "oklch(0.7 0.1 150 / 0.5)" to ok("#6FB07D", ParseNote.AlphaDropped),
                "oklch(1.2 0 0)" to ok("#FFFFFF", ParseNote.Clamped),
                "rebeccapurple" to ok("#663399"),
                "RebeccaPurple" to ok("#663399"),
                "  Light Goldenrod Yellow " to ok("#FAFAD2"),
                "dark-slate-grey" to ok("#2F4F4F"),
            )

        cases.forEach { (text, expected) -> assertEquals(expected, ColorInput.parse(text), text) }
    }

    @Test
    fun parse_textThatIsNotAColor_isInvalidWithAReason() {
        val cases =
            listOf(
                "" to InvalidReason.Empty,
                "   " to InvalidReason.Empty,
                "#" to InvalidReason.BadHex,
                "#12" to InvalidReason.BadHex,
                "#1234" to InvalidReason.BadHex,
                "#12345" to InvalidReason.BadHex,
                "#ggg" to InvalidReason.BadHex,
                "0x123" to InvalidReason.BadHex,
                "beef" to InvalidReason.BadHex,
                "Color(0xZZ)" to InvalidReason.BadHex,
                "Color(" to InvalidReason.BadArguments,
                "Color(6750A4)" to InvalidReason.BadArguments,
                "rgb(1,2)" to InvalidReason.BadArguments,
                "rgb()" to InvalidReason.BadArguments,
                "rgb(1,2,3" to InvalidReason.BadArguments,
                "rgb(1,2,3))" to InvalidReason.BadArguments,
                "rgb(1, 2 3)" to InvalidReason.BadArguments,
                "rgb(1, 2, 3 / 0.5)" to InvalidReason.BadArguments,
                "rgb(1 2 3 / 4 / 5)" to InvalidReason.BadArguments,
                "rgb(1deg, 2, 3)" to InvalidReason.BadArguments,
                "rgb(1e999, 0, 0)" to InvalidReason.BadArguments,
                "rgb(x, 0, 0)" to InvalidReason.BadArguments,
                "hsl(10px, 5%, 5%)" to InvalidReason.BadArguments,
                "hsl(10%, 5%, 5%)" to InvalidReason.BadArguments,
                "oklch(0.5 0.1)" to InvalidReason.BadArguments,
                "hwb(0 0% 0%)" to InvalidReason.UnknownFunction,
                "hwb(" to InvalidReason.UnknownFunction,
                "notacolor" to InvalidReason.UnknownName,
                "transparent" to InvalidReason.UnknownName,
                "12 34" to InvalidReason.Unrecognized,
                "()" to InvalidReason.Unrecognized,
                "#6750A4!" to InvalidReason.BadHex,
            )

        cases.forEach { (text, reason) -> assertEquals(ParseResult.Invalid(reason), ColorInput.parse(text), text) }
    }

    @Test
    fun parse_oklchOutsideSrgb_keepsLightnessAndHueAndGivesUpChroma() {
        val result = assertIs<ParseResult.Ok>(ColorInput.parse("oklch(0.7 0.4 150)"))
        val landed = result.argb.toOklab().toOklch()

        assertEquals(setOf(ParseNote.Clamped), result.notes)
        assertEquals(0.7, landed.l, absoluteTolerance = 0.01)
        assertEquals(150.0, landed.h, absoluteTolerance = 2.0)
        assertTrue(landed.c in 0.15..0.2, "chroma ${landed.c}")
    }

    @Test
    fun parse_tenThousandRandomStrings_neverThrowAndOnlyGiveOpaqueColors() {
        val random = Random(seed = 103)

        repeat(10_000) {
            val text = random.nextColorishText()
            val result = ColorInput.parse(text)
            if (result is ParseResult.Ok) assertEquals(0xFF, result.argb.value ushr 24, text)
        }
    }

    private fun ok(
        hex: String,
        vararg notes: ParseNote,
    ): ParseResult = ParseResult.Ok(Argb.fromHex(hex), notes.toSet())

    private fun Random.nextColorishText(): String {
        val random = this
        return buildString {
            append(RandomPrefixes.random(random))
            repeat(random.nextInt(until = 24)) {
                val anyChar = random.nextInt(until = 8) == 0
                append(if (anyChar) Char(random.nextInt(until = 0x10000)) else RandomAlphabet.random(random))
            }
            if (random.nextBoolean()) append(')')
        }
    }

    private companion object {
        val RandomPrefixes =
            listOf(
                "",
                "#",
                "0x",
                "rgb(",
                "rgba(",
                "hsl(",
                "hsla(",
                "oklch(",
                "Color(",
                "Color(0x",
                "rgb(1 2 ",
                "oklch(0.7 ",
            )

        const val RandomAlphabet = "0123456789abcdefABCDEFxX#(),/%.+-eE dgrtunhslokc\t"
    }
}
