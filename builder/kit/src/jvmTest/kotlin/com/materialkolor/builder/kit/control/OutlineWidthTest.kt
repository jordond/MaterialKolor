package com.materialkolor.builder.kit.control

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OutlineWidthTest {
    @Test
    fun outlineWidth_reprovidedWider_everySkin_widensTheFieldBorder() =
        forEachSkin { _, skin ->
            var width: Dp? = null
            setContent {
                ControlsHarness(skin) {
                    val tokens = LocalBuilderTokens.current
                    CompositionLocalProvider(LocalBuilderTokens provides tokens.copy(outlineWidth = 3.dp)) {
                        width = overlayStyle(LocalSkin.current.library).fieldBorder.width
                    }
                }
            }
            waitForIdle()

            width shouldBe 3.dp
        }
}
