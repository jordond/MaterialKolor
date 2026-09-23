package com.materialkolor.builder.di

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.PlaceholderModel
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test

/**
 * S10 in the browser. Runs in headless Chrome through `wasmJsBrowserTest`, on the wasm Compose
 * runtime and the view model store the Compose scene provides, the same one `ComposeViewport` uses.
 */
@OptIn(ExperimentalTestApi::class)
class AppGraphBrowserTest {
    @Test
    fun metroViewModel_acrossRecompositionsInTheBrowser_keepsOneModel() =
        runComposeUiTest {
            val graph = createGraphFactory<AppGraph.Factory>().create(TestPlatform)
            val tick = mutableIntStateOf(0)
            val seen = mutableListOf<PlaceholderModel>()
            setContent {
                CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory) {
                    val model = metroViewModel<PlaceholderModel>()
                    seen += model
                    BasicText("tick ${tick.intValue}")
                }
            }

            repeat(2) {
                tick.intValue++
                awaitIdle()
            }

            onNodeWithText("tick 2").assertExists()
            seen shouldHaveAtLeastSize 3
            seen.forEach { model -> model shouldBeSameInstanceAs seen.first() }
        }
}

/** Services that do nothing, apart from stores held in memory. */
private object TestPlatform : PlatformServices {
    override val router: Router = TestRouter
    override val stores: StoreFactory = InMemoryStoreFactory()
    override val clipboard: Clipboard = TestClipboard
    override val files: FileSaver = TestFileSaver
    override val images: ImageInput = TestImageInput
    override val pastes: PasteInput = TestPasteInput
    override val environment: Environment = TestEnvironment
}

private object TestRouter : Router {
    override val initial: Route = Route.Home
    override val overlayPops: Flow<Unit> = emptyFlow()

    override fun replaceHome() = Unit

    override fun pushOverlay(id: String) = Unit

    override fun popOverlay() = Unit
}

private object TestClipboard : Clipboard {
    override suspend fun writeText(text: String): Result<Unit> = Result.success(Unit)
}

private object TestFileSaver : FileSaver {
    override val canShareFiles: Boolean = false

    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> = Result.success(Unit)
}

private object TestImageInput : ImageInput {
    override val drops: Flow<ImageHandle> = emptyFlow()

    override suspend fun pick(): ImageHandle? = null

    override suspend fun decode(handle: ImageHandle): DecodedImage? = null
}

private object TestPasteInput : PasteInput {
    override val pastes: Flow<Paste> = emptyFlow()
}

private object TestEnvironment : Environment {
    override val prefersDark: StateFlow<Boolean> = MutableStateFlow(false)
    override val reducedMotion: StateFlow<Boolean> = MutableStateFlow(false)
    override val coarsePointer: StateFlow<Boolean> = MutableStateFlow(false)
    override val eyeDropperAvailable: Boolean = false
    override val tabId: String = "test"
    override val storageAvailable: Boolean = false

    override suspend fun pickScreenColor(): Argb? = null

    override fun hideSplash() = Unit

    override fun setThemeColor(argb: Argb) = Unit

    override fun writeSplashColors(
        light: Argb,
        dark: Argb,
    ) = Unit

    override suspend fun requestPersist(): Boolean = false

    // b-215a
    override fun readTabProject(): String? = null

    override fun writeTabProject(id: String?) = Unit
}
