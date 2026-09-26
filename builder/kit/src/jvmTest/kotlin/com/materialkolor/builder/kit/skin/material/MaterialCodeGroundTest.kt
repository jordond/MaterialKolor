package com.materialkolor.builder.kit.skin.material

import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.material3.toColorScheme
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import kotlin.test.Test

/**
 * What code text has to reach on its ground, WCAG's normal text contrast.
 */
private const val TextContrast = 4.5

/**
 * The builder's default seed and a pale yellow, whose accents sit closest in lightness to its
 * neutrals.
 */
private val Seeds: List<Argb> = listOf(Argb(0xD9653B), Argb(0xE8D44D))

/**
 * The M3 skin puts the code on a dark ground in both modes, and every ink of its code palette reads
 * on it. The schemes come from the engine, as the chrome's do.
 */
class MaterialCodeGroundTest {
    @Test
    fun codePalette_onTheCodeGround_reachesTextContrast() {
        for (seed in Seeds) {
            val result = ThemeResolver().resolve(ThemeDocument(seed = seed, style = Style.TonalSpot))
            for (isDark in listOf(false, true)) {
                val tokens = result.chrome(isDark).toColorScheme().builderTokens(isDark)
                val palette = tokens.codePalette
                val inks = TokenKind.entries.associate { kind -> kind.name to palette[kind] } +
                    ("muted" to palette.muted)
                for ((name, ink) in inks) {
                    withClue("$name on the code ground, seed $seed, dark $isDark") {
                        ink.contrastRatio(tokens.codeBackground) shouldBeGreaterThanOrEqual TextContrast
                    }
                }
            }
        }
    }
}
