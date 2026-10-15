package com.materialkolor.builder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.widget.forgetPlanePictures
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * How long a test waits in wall time for a condition before it fails. The condition itself moves on
 * the test's clock, so this only runs out on a slow or busy machine.
 */
internal const val WAIT_MILLIS: Long = 10_000L

/**
 * The width of a desktop window, wide enough for the docked poster.
 */
internal const val WIDTH: Int = 1280

/**
 * The height of a desktop window.
 */
internal const val HEIGHT: Int = 800

/**
 * The app graph on fakes for one test, on the Compose test's own clock.
 *
 * The app scope and `Dispatchers.Main` run on the scheduler behind `mainClock`, so autosave, the
 * splash and every view model move only as the test's clock does. Each test class that uses one
 * calls [close] from its `@AfterTest`.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class, KitTestApi::class)
internal open class AppHarness(
    val platform: FakePlatform = FakePlatform(),
) {
    /**
     * The graph [createGraph] built last.
     */
    lateinit var graph: AppGraph
        private set

    private val scopes = mutableListOf<CoroutineScope>()
    private val stores = mutableListOf<ViewModelStore>()
    private var lastClock: Clock? = null

    /**
     * The view models of the graph built last, or of none yet.
     */
    var owner: ViewModelStoreOwner = newOwner()
        private set

    /**
     * Builds the graph over [platform] on this test's clock and makes it [graph].
     */
    fun ComposeUiTest.createGraph(platform: FakePlatform = this@AppHarness.platform): AppGraph {
        val dispatcher = mainOnTestClock()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scopes += scope
        val start = lastClock?.now() ?: START
        val clock = TestClock(start, mainClock)
        lastClock = clock
        owner = newOwner()
        graph = createGraphFactory<AppGraph.Factory>().create(platform, scope, clock)
        return graph
    }

    /**
     * [content] with the view models of [graph].
     */
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides owner,
            LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            content = content,
        )
    }

    /**
     * Builds the graph over [platform], shows [root] with its view models and waits until the
     * builder has booted and hidden the splash.
     */
    fun ComposeUiTest.bootRoot(
        platform: FakePlatform = this@AppHarness.platform,
        root: @Composable (graph: AppGraph) -> Unit = { graph -> BuilderRoot(graph) },
    ): AppGraph {
        val graph = createGraph(platform)
        setContent { Provide { root(graph) } }
        waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    /**
     * Cancels every app scope, clears every view model and hands `Dispatchers.Main` back.
     */
    fun close() {
        scopes.forEach { scope -> scope.cancel() }
        stores.forEach { store -> store.clear() }
        if (scopes.isNotEmpty()) Dispatchers.resetMain()
        scopes.clear()
        stores.clear()
        forgetPlanePictures()
    }

    private fun newOwner(): ViewModelStoreOwner {
        val store = ViewModelStore()
        stores += store
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    /**
     * The time from [start] on, as far as [clock] has moved.
     */
    private class TestClock(
        private val start: Instant,
        private val clock: MainTestClock,
    ) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + clock.currentTime)
    }

    private companion object {
        val START: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}

/**
 * Puts `Dispatchers.Main` on this test's clock, for view models a test builds by hand, and returns
 * the dispatcher it put there. A test that calls it resets Main in its `@AfterTest`.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal fun ComposeUiTest.mainOnTestClock(): TestDispatcher {
    val dispatcher = UnconfinedTestDispatcher(mainClock.scheduler)
    Dispatchers.setMain(dispatcher)
    return dispatcher
}
