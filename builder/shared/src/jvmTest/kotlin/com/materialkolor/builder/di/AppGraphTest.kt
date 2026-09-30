package com.materialkolor.builder.di

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderApp
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.fakes.RouterCall
import com.materialkolor.builder.feature.workspace.AppModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppGraphTest {
    private val app = AppHarness()

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun metroViewModel_acrossRecompositions_keepsOneModel() =
        runComposeUiTest {
            val tick = mutableIntStateOf(0)
            val seen = mutableListOf<AppModel>()
            with(app) {
                createGraph()
                setContent {
                    Provide {
                        val model = metroViewModel<AppModel>()
                        seen += model
                        BasicText("tick ${tick.intValue}")
                    }
                }
            }

            repeat(2) {
                tick.intValue++
                waitForIdle()
            }

            seen shouldHaveAtLeastSize 3
            seen.distinct().size shouldBe 1
        }

    @Test
    fun builderApp_onFirstFrame_bootsHidesTheSplashAndShowsTheWorkspace() =
        runComposeUiTest {
            val platform = app.platform
            setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides app.owner) {
                    BuilderApp(platform)
                }
            }

            waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
            // More options stays in the bar at every width, where Undo may have moved into it.
            onNodeWithContentDescription("More options").assertExists()
            platform.router.calls shouldBe listOf(RouterCall.ReplaceHome)
        }
}
