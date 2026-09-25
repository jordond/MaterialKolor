package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.Selawik_Regular
import com.materialkolor.builder.kit.generated.resources.Selawik_Semibold
import com.materialkolor.builder.kit.shell.PosterSurface
import com.materialkolor.builder.kit.skin.Skin
import io.github.composefluent.FluentTheme
import io.github.composefluent.Typography
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FluentTypeTest {
    @Test
    fun inFace_setsEveryStyleInTheFace_andKeepsTheScale() =
        runComposeUiTest {
            var scale: Typography? = null
            setContent { scale = FluentTheme.typography }
            waitForIdle()
            val base = checkNotNull(scale)
            val face = FontFamily.Serif

            val set = base.inFace(face)

            set.styles().map { style -> style.fontFamily }.distinct() shouldBe listOf(face)
            set.styles().map { style -> style.copy(fontFamily = null) } shouldBe base.styles()
        }

    /**
     * The chrome and the poster both hand Fluent's components Selawik, the same face in each.
     */
    @Test
    fun fluentSkin_setsFluentsOwnTextInSelawik_onTheChromeAndThePoster() =
        runComposeUiTest {
            var face: FontFamily? = null
            var chrome: Typography? = null
            var poster: Typography? = null
            setContent {
                ControlsHarness(Skin(Library.Fluent, expressive = false)) {
                    face = fluentFontFamily()
                    chrome = FluentTheme.typography
                    PosterSurface(PosterColors.of(Argb(0xD9653B))) { poster = FluentTheme.typography }
                }
            }
            waitForIdle()

            for ((where, scale) in listOf("chrome" to chrome, "poster" to poster)) {
                withClue(where) {
                    checkNotNull(scale).styles().filter { style -> style.fontFamily != face }.shouldBeEmpty()
                }
            }
        }

    @Test
    fun preloadFluentFace_readsBothWeights_andCanBeAskedAgain() =
        runTest {
            preloadFluentFace()
            preloadFluentFace()

            val environment = getSystemResourceEnvironment()
            for (face in listOf(Res.font.Selawik_Regular, Res.font.Selawik_Semibold)) {
                getFontResourceBytes(environment, face).size shouldBeGreaterThan 0
            }
        }
}

private fun Typography.styles(): List<TextStyle> =
    listOf(caption, body, bodyStrong, bodyLarge, subtitle, title, titleLarge, display)
