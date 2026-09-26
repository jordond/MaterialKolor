package com.materialkolor.builder.kit.skin

import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.tweenBuilderMotion
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Every skin the builder can use, Material3 once per flavour.
 */
private val Skins: List<Skin> =
    SkinLibrary.entries.map { library -> Skin(library, expressive = false) } + Skin(SkinLibrary.Material3, expressive = true)

@OptIn(ExperimentalTestApi::class)
class SkinMotionTest {
    @Test
    fun material3_bothFlavours_keepTheBuilderTweensForTheFencedTransitions() =
        runComposeUiTest {
            val seen = mutableMapOf<Boolean, BuilderMotion>()
            setContent {
                val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
                for (expressive in listOf(false, true)) {
                    BuilderTheme(Skin(SkinLibrary.Material3, expressive), result, isDark = false, reducedMotion = false) {
                        seen[expressive] = LocalBuilderMotion.current
                    }
                }
            }

            waitForIdle()
            val tweens = tweenBuilderMotion()
            seen.keys shouldBe setOf(false, true)
            for ((expressive, motion) in seen) {
                withClue("expressive = $expressive") {
                    motion.reveal<Float>() shouldBe tweens.reveal<Float>()
                    motion.crossfade<Float>() shouldBe tweens.crossfade<Float>()
                    motion.panelEnter<Float>() shouldBe tweens.panelEnter<Float>()
                    motion.panelExit<Float>() shouldBe tweens.panelExit<Float>()
                }
            }
        }

    @Test
    fun everySkin_reducedMotion_pressesWithoutShrinking() =
        runComposeUiTest {
            val seen = mutableMapOf<Skin, Float>()
            setContent {
                val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
                for (skin in Skins) {
                    BuilderTheme(skin, result, isDark = false, reducedMotion = true) {
                        seen[skin] = LocalBuilderMotion.current.pressScale
                    }
                }
            }

            waitForIdle()
            seen shouldBe Skins.associateWith { 1f }
        }
}
