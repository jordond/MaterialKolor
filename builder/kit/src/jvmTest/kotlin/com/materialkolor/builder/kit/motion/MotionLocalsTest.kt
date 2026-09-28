package com.materialkolor.builder.kit.motion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class MotionLocalsTest {
    @Test
    fun localBuilderMotion_readOutsideASkin_fails() =
        runComposeUiTest {
            assertFailsWith<IllegalStateException> {
                setContent {
                    LocalBuilderMotion.current.durations
                }
            }
        }

    @Test
    fun localBuilderMotion_readInsideASkin_isTheMotionTheSkinProvided() =
        runComposeUiTest {
            val provided = reducedBuilderMotion()
            var seen: BuilderMotion? = null
            setContent {
                CompositionLocalProvider(LocalBuilderMotion provides provided) {
                    seen = LocalBuilderMotion.current
                }
            }

            assertNotNull(seen) shouldBe provided
        }
}
