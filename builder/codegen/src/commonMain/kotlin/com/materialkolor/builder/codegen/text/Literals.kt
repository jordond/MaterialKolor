package com.materialkolor.builder.codegen.text

import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.codegen.dsl.raw
import com.materialkolor.builder.codegen.symbol.Symbols
import kotlin.math.abs

/**
 * Every number, colour and string that reaches generated source is formatted here.
 *
 * The formatting is done by hand rather than through `Double.toString` or `Int.toString(radix)`,
 * because those disagree between the JVM and wasm and the goldens have to match on both.
 */
public object Literals {
    private const val HEX_DIGITS = "0123456789ABCDEF"
    private const val HUNDRED = 100

    /**
     * An ARGB colour as `Color(0xFF6750A4)`, with the swatch carried on the literal token.
     */
    public fun colorLiteral(argb: Int): Expression =
        raw(
            tokens = listOf(
                Token(TokenKind.Function, "Color"),
                Token(TokenKind.Punctuation, "("),
                Token(TokenKind.ColorLiteral, hexText(argb), color = argb),
                Token(TokenKind.Punctuation, ")"),
            ),
            symbols = listOf(Symbols.Color),
        )

    /**
     * An ARGB colour as the bare `0xFF6750A4` text, in upper case and always eight digits.
     */
    public fun hexText(argb: Int): String =
        buildString {
            append("0x")
            for (shift in 28 downTo 0 step 4) {
                append(HEX_DIGITS[(argb ushr shift) and 0xF])
            }
        }

    /**
     * A number held in hundredths, written as the `Double` literal it stands for.
     */
    public fun decimal(hundredths: Int): Expression =
        raw(listOf(Token(TokenKind.NumberLiteral, decimalText(hundredths))))

    /**
     * The text of a number held in hundredths.
     *
     * Contrast levels and the like are stored as whole hundredths, so `50` reads as `0.5` and `-100`
     * as `-1.0`. There is always at least one digit after the point, which is what makes the value a
     * `Double` to the Kotlin compiler.
     */
    public fun decimalText(hundredths: Int): String {
        val sign = if (hundredths < 0) "-" else ""
        val magnitude = abs(hundredths.toLong())
        val whole = magnitude / HUNDRED
        val fraction = (magnitude % HUNDRED).toInt()

        return when {
            fraction == 0 -> "$sign$whole.0"
            fraction % 10 == 0 -> "$sign$whole.${fraction / 10}"
            fraction < 10 -> "$sign$whole.0$fraction"
            else -> "$sign$whole.$fraction"
        }
    }

    /**
     * A whole number.
     */
    public fun int(value: Int): Expression = raw(listOf(Token(TokenKind.NumberLiteral, value.toString())))

    /**
     * `true` or `false`.
     */
    public fun boolean(value: Boolean): Expression = raw(listOf(Token(TokenKind.Keyword, value.toString())))

    /**
     * A quoted string, escaped so it survives a Kotlin parser.
     */
    public fun string(value: String): Expression = raw(listOf(Token(TokenKind.StringLiteral, "\"${escape(value)}\"")))

    /**
     * The body of a Kotlin string literal, without the quotes.
     *
     * Anything a reader would not be able to type back in, including control characters, comes out
     * as an escape.
     */
    public fun escape(value: String): String =
        buildString {
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\$")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    else -> if (character.code < 0x20 || character.code == 0x7F) {
                        append("\\u")
                        for (shift in 12 downTo 0 step 4) {
                            append(HEX_DIGITS[(character.code ushr shift) and 0xF])
                        }
                    } else {
                        append(character)
                    }
                }
            }
        }
}
