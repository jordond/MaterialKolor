package com.materialkolor.builder.codegen.text

import com.materialkolor.builder.codegen.dsl.TokenKind
import kotlin.test.Test
import kotlin.test.assertEquals

class LiteralsTest {
    @Test
    fun hexText_argbValues_writesEightUpperCaseDigits() {
        val table = listOf(
            -0x98AF5C to "0xFF6750A4",
            0x00000000 to "0x00000000",
            -0x1 to "0xFFFFFFFF",
            -0x1000000 to "0xFF000000",
            0x7FFFFFFF to "0x7FFFFFFF",
            0x0A0B0C0D to "0x0A0B0C0D",
        )

        table.forEach { (argb, expected) ->
            assertEquals(expected, Literals.hexText(argb), "argb $expected")
        }
    }

    @Test
    fun decimalText_hundredths_writesTheDoubleLiteral() {
        val table = listOf(
            50 to "0.5",
            -100 to "-1.0",
            100 to "1.0",
            5 to "0.05",
            0 to "0.0",
            -50 to "-0.5",
            -5 to "-0.05",
            125 to "1.25",
            110 to "1.1",
            1000 to "10.0",
            -1 to "-0.01",
        )

        table.forEach { (hundredths, expected) ->
            assertEquals(expected, Literals.decimalText(hundredths), "hundredths $hundredths")
        }
    }

    @Test
    fun escape_awkwardCharacters_writesKotlinEscapes() {
        val table = listOf(
            "plain" to "plain",
            "a\"b" to "a\\\"b",
            "a\\b" to "a\\\\b",
            "a\$b" to "a\\\$b",
            "a\nb" to "a\\nb",
            "a\tb" to "a\\tb",
            "a\rb" to "a\\rb",
            "a\u0000b" to "a\\u0000b",
            "a\u001Fb" to "a\\u001Fb",
        )

        table.forEach { (value, expected) ->
            assertEquals(expected, Literals.escape(value), "escape of $expected")
        }
    }

    @Test
    fun colorLiteral_argb_tokenisesAsAColorCarryingTheValue() {
        val expression = Literals.colorLiteral(-0x98AF5C)
        val tokens = expression.tokens

        assertEquals("Color(0xFF6750A4)", tokens.joinToString("") { it.text })

        val colorToken = tokens.single { it.kind == TokenKind.ColorLiteral }
        assertEquals(-0x98AF5C, colorToken.color)
    }

    @Test
    fun string_value_isQuotedAndEscaped() {
        val tokens = Literals.string("say \"hi\"").tokens

        assertEquals("\"say \\\"hi\\\"\"", tokens.joinToString("") { it.text })
        assertEquals(TokenKind.StringLiteral, tokens.single().kind)
    }

    @Test
    fun boolean_values_writeKeywords() {
        assertEquals("true", Literals.boolean(true).tokens.joinToString("") { it.text })
        assertEquals("false", Literals.boolean(false).tokens.joinToString("") { it.text })
    }
}
