package com.materialkolor.builder.kit.token

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.codegen.dsl.TokenKind
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CodePaletteTest {
    @Test
    fun codePalette_coveringEveryKind_answersWithTheSkinsColour() {
        val palette = CodePalette(colors = fullPalette(), plain = Color.Black)

        palette[TokenKind.Keyword] shouldBe Color.Red
        palette[TokenKind.TomlKey] shouldBe Color.Red
        palette[TokenKind.Plain] shouldBe Color.Black
    }

    @Test
    fun codePalette_missingAKind_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            CodePalette(colors = fullPalette() - TokenKind.Keyword, plain = Color.Black)
        }
    }

    @Test
    fun codePalette_missingOnlyPlain_isAccepted() {
        val palette = CodePalette(colors = fullPalette() - TokenKind.Plain, plain = Color.Black)

        palette[TokenKind.Plain] shouldBe Color.Black
    }

    private fun fullPalette(): Map<TokenKind, Color> = TokenKind.entries.associateWith { Color.Red }
}
