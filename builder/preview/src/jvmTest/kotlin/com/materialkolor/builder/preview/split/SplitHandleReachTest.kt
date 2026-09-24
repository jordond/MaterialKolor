package com.materialkolor.builder.preview.split

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-406

/** The line and grip the handle draws, the kit's section spacing, whatever it reaches. */
private val Drawn: Dp = 32.dp

@OptIn(ExperimentalTestApi::class)
class SplitHandleReachTest {
    @Test
    fun reach_onAPhone_isThumbSized() =
        runComposeUiTest {
            showHandle(LayoutInfo(390.dp, 844.dp))

            handleWidth() shouldBe (48f plusOrMinus 0.5f)
        }

    @Test
    fun reach_withAFingerOnAWideWindow_isTheTouchTarget() =
        runComposeUiTest {
            showHandle(LayoutInfo(1280.dp, 800.dp, coarsePointer = true))

            handleWidth() shouldBe (44f.coerceAtLeast(Drawn.value) plusOrMinus 0.5f)
        }

    @Test
    fun reach_withAMouse_isTheDrawnHandle() =
        runComposeUiTest {
            showHandle(LayoutInfo(1280.dp, 800.dp))

            handleWidth() shouldBe (Drawn.value plusOrMinus 0.5f)
        }

    @Test
    fun reach_onAPhone_staysCentredOnTheSplit() =
        runComposeUiTest {
            showHandle(LayoutInfo(390.dp, 844.dp))

            val bounds = onNodeWithContentDescription("Split").fetchSemanticsNode().boundsInRoot
            (bounds.center.x / density.density) shouldBe (200f plusOrMinus 1f)
        }

    private fun ComposeUiTest.showHandle(layout: LayoutInfo) {
        setContent {
            CompositionLocalProvider(LocalLayout provides layout) {
                Chrome {
                    SplitPreview(LightSpec, DarkSpec, SplitState(), Modifier.size(400.dp, 300.dp)) { }
                }
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.handleWidth(): Float =
        onNodeWithContentDescription("Split").fetchSemanticsNode().size.width / density.density
}
