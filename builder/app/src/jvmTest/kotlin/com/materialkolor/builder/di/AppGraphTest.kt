package com.materialkolor.builder.di

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderApp
import com.materialkolor.builder.PlaceholderModel
import com.materialkolor.builder.desktop.DesktopPlatform
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppGraphTest {
    @Test
    fun themeResolver_readTwice_isOneInstance() {
        val graph = createGraphFactory<AppGraph.Factory>().create(DesktopPlatform)

        graph.themeResolver shouldBeSameInstanceAs graph.themeResolver
    }

    @Test
    fun environment_fromTheIncludedPlatform_isThePlatformsOwn() {
        val graph = createGraphFactory<AppGraph.Factory>().create(DesktopPlatform)

        graph.environment shouldBeSameInstanceAs DesktopPlatform.environment
    }

    @Test
    fun metroViewModel_acrossRecompositions_keepsOneModel() =
        runComposeUiTest {
            val graph = createGraphFactory<AppGraph.Factory>().create(DesktopPlatform)
            val owner = TestOwner()
            val tick = mutableIntStateOf(0)
            val seen = mutableListOf<PlaceholderModel>()
            setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides owner,
                    LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                ) {
                    val model = metroViewModel<PlaceholderModel>()
                    seen += model
                    BasicText("tick ${tick.intValue}")
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
    fun builderApp_onDesktop_showsThePlaceholder() =
        runComposeUiTest {
            val owner = TestOwner()
            setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    BuilderApp(DesktopPlatform)
                }
            }

            onNodeWithText("MaterialKolor Builder").assertExists()
        }

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }
}
