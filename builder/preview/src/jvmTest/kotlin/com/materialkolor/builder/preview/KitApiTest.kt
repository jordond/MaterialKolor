package com.materialkolor.builder.preview

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.kit.token.CodePalette
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Preview depends on the kit and on nothing the kit depends on.
 *
 * `TokenKind` is in the signature of [CodePalette], so this only compiles while the kit hands
 * `:builder:codegen` on as `api`. Every consumer that builds or reads a palette is in the same
 * position.
 */
class KitApiTest {
    @Test
    fun codePalette_builtFromAConsumerModule_resolvesTheTokenKind() {
        val palette = CodePalette(
            colors = TokenKind.entries.associateWith { Color.Red },
            plain = Color.Black,
        )

        palette[TokenKind.Keyword] shouldBe Color.Red
    }
}
