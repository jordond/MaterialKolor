package com.materialkolor.builder.kit.token

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.codegen.dsl.TokenKind
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CodePaletteTest {
    @Test
    fun codePalette_coveringEveryKind_answersWithTheSkinsColour() {
        val palette = CodePalette(colors = fullPalette(), plain = Color.Black, muted = Color.Gray)

        palette[TokenKind.Keyword] shouldBe Color.Red
        palette[TokenKind.TomlKey] shouldBe Color.Red
        palette[TokenKind.Plain] shouldBe Color.Black
        palette.muted shouldBe Color.Gray
    }

    @Test
    fun codePalette_differingOnlyInMuted_isNotEqual() {
        val palette = CodePalette(colors = fullPalette(), plain = Color.Black, muted = Color.Gray)
        val louder = CodePalette(colors = fullPalette(), plain = Color.Black, muted = Color.White)

        palette shouldNotBe louder
    }

    @Test
    fun codePalette_missingAKind_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            CodePalette(colors = fullPalette() - TokenKind.Keyword, plain = Color.Black, muted = Color.Gray)
        }
    }

    @Test
    fun codePalette_missingOnlyPlain_isAccepted() {
        val palette = CodePalette(colors = fullPalette() - TokenKind.Plain, plain = Color.Black, muted = Color.Gray)

        palette[TokenKind.Plain] shouldBe Color.Black
    }

    private fun fullPalette(): Map<TokenKind, Color> = TokenKind.entries.associateWith { Color.Red }
}
