package com.materialkolor.builder

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.platform.TimingMarks
import com.materialkolor.builder.core.resources.WholeFileResourceReader
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.image.ImageSeedModel
import com.materialkolor.builder.feature.workspace.AppModel
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.WorkspaceScreen
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.transition.SkinTransition
import com.materialkolor.builder.kit.transition.SkinTransitionHost
import com.materialkolor.builder.kit.transition.SnapshotMode
import com.materialkolor.builder.kit.transition.rememberSkinTransition
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.LocalResourceReader
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The builder, on whatever platform [platform] describes.
 *
 * This is the one public entry point. The web shell and the desktop window both call it and hand
 * over their own services. [motionFrozen] holds every animation still, for the browser tests'
 * screenshots, and is never a person's setting. [awaitIdle] waits for an idle moment, and with it
 * the builder warms up for its first switch to Fluent after the first frame. The web hands it over.
 */
@Composable
fun BuilderApp(
    platform: PlatformServices,
    motionFrozen: Boolean = false,
    awaitIdle: (suspend () -> Unit)? = null,
) {
    val graph = remember(platform) {
        createGraphFactory<AppGraph.Factory>().create(
            platform = platform,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            clock = Clock.System,
        )
    }
    CompositionLocalProvider(
        LocalMetroViewModelFactory provides graph.metroViewModelFactory,
        LocalMotionFrozen provides motionFrozen,
    ) {
        BuilderRoot(graph, awaitIdle = awaitIdle)
    }
}

/**
 * The theme result of the frame being drawn, resolved once at the root for everything below.
 */
internal val LocalThemeResult: ProvidableCompositionLocal<ThemeResult> = compositionLocalOf {
    error("No ThemeResult provided")
}

/**
 * The resolver behind [LocalThemeResult], for the few places that need a scheme the open theme does
 * not hold, such as the style chips. It belongs to the UI thread. Null outside the app root.
 */
internal val LocalThemeResolver: ProvidableCompositionLocal<ThemeResolver?> = staticCompositionLocalOf { null }

/**
 * The one theme result per frame, derived from the collected [document] as its own target sees it.
 * A setting the target turns off never reaches the chrome or the preview.
 *
 * @param[environment] Marks each resolve's start and end for the perf run. None by default.
 */
@Composable
internal fun rememberThemeResult(
    document: State<ThemeDocument>,
    resolver: ThemeResolver,
    environment: Environment? = null,
): State<ThemeResult> =
    remember(document, resolver, environment) {
        derivedStateOf {
            val stored = document.value
            environment?.mark(TimingMarks.RESOLVE_START)
            resolver
                .resolve(stored.forTarget(ExportTarget.of(stored.library, stored.expressive)))
                .also { environment?.mark(TimingMarks.RESOLVE) }
        }
    }

/**
 * Whether the builder's own controls use Material 3 Expressive, whatever library the open project
 * targets.
 *
 * A library switch changes the preview and the colors, never the controls around them, since a
 * whole new set of controls on every switch was too jarring.
 */
internal const val ShellExpressive: Boolean = true

/**
 * Themes the workspace in Material 3 Expressive, per [ShellExpressive], with the colors of the open
 * project.
 *
 * The workspace state is collected once, here, and the theme result comes from its own document. So
 * no frame pairs a new document with the old colors, and the workspace draws the state the colors
 * were resolved from.
 *
 * @param[awaitIdle] Waits for an idle moment. With it the builder warms up the reveal a library
 * switch plays once the first frame is up. None by default, and no warm-up then.
 * @param[probe] Drawn over the workspace with the state it was handed, for tests. Nothing by default.
 * @param[stringsWait] The longest the splash waits on the first screen's strings, so a read that
 * never answers still lets the builder show.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun BuilderRoot(
    graph: AppGraph,
    model: AppModel = metroViewModel(),
    workspaceModel: WorkspaceModel = metroViewModel(),
    awaitIdle: (suspend () -> Unit)? = null,
    stringsWait: Duration = 2.seconds,
    probe: @Composable (state: WorkspaceModel.State) -> Unit = {},
) {
    val state by model.collectAsState()
    val workspace = workspaceModel.collectAsState()
    val document = remember(workspace) { derivedStateOf { workspace.value.document } }
    val environment = graph.environment
    val result by rememberThemeResult(document, graph.themeResolver, environment)
    val announcer = remember(environment) { Announcer { message -> environment.announce(message) } }
    val firstFrame = remember { CompletableDeferred<Unit>() }
    val baseReader = LocalResourceReader.current
    val resourceReader = remember(baseReader) { WholeFileResourceReader(baseReader) }

    LaunchedEffect(model) {
        model.boot()
        withFrameNanos {}
        withTimeoutOrNull(stringsWait) { resourceReader.awaitFirstScreen() }
        withFrameNanos {}
        environment.hideSplash()
        environment.mark(TimingMarks.FIRST_FRAME)
        firstFrame.complete(Unit)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { model.flush() }
    ImageMarkEffects(environment)

    CompositionLocalProvider(
        LocalResourceReader provides resourceReader,
        LocalThemeResult provides result,
        LocalThemeResolver provides graph.themeResolver,
        LocalAnnouncer provides announcer,
    ) {
        BuilderTheme(
            expressive = ShellExpressive,
            result = result,
            isDark = state.isDark,
            reducedMotion = state.reducedMotion,
        ) {
            ThemeColorEffect(environment)
            ProvideBuilderLayout(coarsePointer = state.coarsePointer, modifier = Modifier.fillMaxSize()) {
                val transition = rememberSkinTransition(SnapshotMode.Bitmap)
                if (awaitIdle != null) RevealWarmUpEffect(transition, firstFrame, awaitIdle)
                SkinTransitionHost(transition = transition, modifier = Modifier.fillMaxSize()) {
                    val shown = workspace.value
                    WorkspaceScreen(state = shown, transition = transition, model = workspaceModel)
                    probe(shown)
                }
            }
        }
    }
}

/**
 * Waits until every string the first screen asks for is in, frame after frame, since a string that
 * arrives can compose more that ask for their own.
 */
private suspend fun WholeFileResourceReader.awaitFirstScreen() {
    do {
        val seen = partsStarted
        awaitReads()
        withFrameNanos {}
    } while (partsStarted != seen)
}

/**
 * Tints the browser's own chrome with the surface the shell stands on.
 */
@Composable
private fun ThemeColorEffect(environment: Environment) {
    val surface = Argb(LocalBuilderTokens.current.panel.toArgb())
    LaunchedEffect(environment, surface) { environment.setThemeColor(surface) }
}

/**
 * Leaves the perf run's two timing marks for each image the user brings in, when its thumbnail is in
 * and when its candidates are. The image model is the one the workspace reads, since each owner keeps
 * one of a kind.
 */
@Composable
private fun ImageMarkEffects(
    environment: Environment,
    images: ImageSeedModel = metroViewModel(),
) {
    LaunchedEffect(environment, images) {
        images.state
            .map { state -> state.arriving?.thumbnail }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { environment.mark(TimingMarks.THUMBNAIL) }
    }
    LaunchedEffect(environment, images) {
        images.state
            .map { state -> state.newest }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { environment.mark(TimingMarks.EXTRACT) }
    }
}

/**
 * Warms up the reveal a library switch plays, once, after [firstFrame] and in idle time, so the
 * browser has the circle's and the crossfade's GPU programs ready before the first switch.
 */
@Composable
private fun RevealWarmUpEffect(
    transition: SkinTransition,
    firstFrame: Deferred<Unit>,
    awaitIdle: suspend () -> Unit,
) {
    LaunchedEffect(transition) {
        firstFrame.await()
        transition.warmUp(sample = null, pause = awaitIdle)
    }
}
