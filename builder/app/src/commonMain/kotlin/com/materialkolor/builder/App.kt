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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.platform.TimingMarks
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.canvas.CanvasArea
import com.materialkolor.builder.feature.canvas.CanvasDock
import com.materialkolor.builder.feature.image.ImageSeedModel
import com.materialkolor.builder.feature.image.ProvideImageSeeds
import com.materialkolor.builder.feature.poster.LocalPosterSheetState
import com.materialkolor.builder.feature.poster.PosterFocus
import com.materialkolor.builder.feature.poster.PosterPanel
import com.materialkolor.builder.feature.topbar.TopBarContent
import com.materialkolor.builder.feature.topbar.rememberTopBarFocus
import com.materialkolor.builder.feature.workspace.AppModel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.WorkspaceScreen
import com.materialkolor.builder.feature.workspace.skinOf
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.rememberBottomSheetState
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.shell.WorkspaceShell
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.fluent.FluentWarmUpTheme
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.transition.SkinTransition
import com.materialkolor.builder.kit.transition.SkinTransitionHost
import com.materialkolor.builder.kit.transition.rememberSkinTransition
import dev.stateholder.dispatcher.rememberDispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

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
    val graph = remember(platform) { createGraphFactory<AppGraph.Factory>().create(platform) }
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
 * The skin of the collected [document]. It reads the same state as [rememberThemeResult], so a
 * library switch or its undo never draws a frame of new colors in the old skin.
 */
@Composable
internal fun rememberSkin(document: State<ThemeDocument>): State<Skin> =
    remember(document) { derivedStateOf { skinOf(document.value) } }

/**
 * Themes the workspace with the skin and colors of the open project.
 *
 * The workspace state is collected once, here, and the theme result and the skin come from its own
 * document. So no frame pairs a new document with the old colors or the old skin, and the workspace
 * draws the state the colors were resolved from. The kit keeps the content movable across a library
 * switch, so the reveal that is playing, the toasts and every scroll position survive it.
 *
 * @param[awaitIdle] Waits for an idle moment. With it the builder warms up for its first switch to
 * Fluent once the first frame is up. None by default, and no warm-up then.
 * @param[probe] Drawn over the workspace with the state it was handed, for tests. Nothing by default.
 */
@Composable
internal fun BuilderRoot(
    graph: AppGraph,
    model: AppModel = metroViewModel(),
    workspaceModel: WorkspaceModel = metroViewModel(),
    awaitIdle: (suspend () -> Unit)? = null,
    probe: @Composable (state: WorkspaceModel.State) -> Unit = {},
) {
    val state by model.collectAsState()
    val workspace = workspaceModel.collectAsState()
    val document = remember(workspace) { derivedStateOf { workspace.value.document } }
    val environment = graph.environment
    val result by rememberThemeResult(document, graph.themeResolver, environment)
    val skin by rememberSkin(document)
    val announcer = remember(environment) { Announcer { message -> environment.announce(message) } }
    val firstFrame = remember { CompletableDeferred<Unit>() }

    LaunchedEffect(model) {
        model.boot()
        withFrameNanos {}
        environment.hideSplash()
        environment.mark(TimingMarks.FIRST_FRAME)
        firstFrame.complete(Unit)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { model.flush() }
    ImageMarkEffects(environment)

    CompositionLocalProvider(
        LocalThemeResult provides result,
        LocalThemeResolver provides graph.themeResolver,
        LocalAnnouncer provides announcer,
    ) {
        BuilderTheme(skin = skin, result = result, isDark = state.isDark, reducedMotion = state.reducedMotion) {
            ThemeColorEffect(environment)
            ProvideBuilderLayout(coarsePointer = state.coarsePointer, modifier = Modifier.fillMaxSize()) {
                val transition = rememberSkinTransition()
                if (awaitIdle != null) FluentWarmUpEffect(transition, firstFrame, awaitIdle, workspace) { state.isDark }
                SkinTransitionHost(transition = transition, modifier = Modifier.fillMaxSize()) {
                    // Read in here, so the content the kit moves sees the same state as the colors
                    // provided above it. Read in the root, a move can pair it with the old colors.
                    val shown = workspace.value
                    WorkspaceScreen(state = shown, transition = transition, model = workspaceModel)
                    probe(shown)
                }
            }
        }
    }
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
 * An announcer that says nothing, so a warm-up never reads out what the page already did.
 */
private val SilentAnnouncer = Announcer { }

/**
 * Warms the page up for the first switch to Fluent, once, after [firstFrame] and in idle time.
 *
 * The first switch compiled some forty GPU programs in one frame, which took a second on a loaded
 * machine. This composes the workspace in Fluent off screen and has [transition] draw it under the
 * live frame a step at a time, waiting on [awaitIdle] before each stage. When the page already
 * shows Fluent only the reveal's own clips over the live frame are warmed.
 *
 * @param[isDark] Which mode the chrome shows, read when the sample composes.
 */
@Composable
private fun FluentWarmUpEffect(
    transition: SkinTransition,
    firstFrame: Deferred<Unit>,
    awaitIdle: suspend () -> Unit,
    workspace: State<WorkspaceModel.State>,
    isDark: () -> Boolean,
) {
    val skin by rememberUpdatedState(LocalSkin.current)
    LaunchedEffect(transition) {
        firstFrame.await()
        val sample: (@Composable () -> Unit)? = if (skin.library == Library.Fluent) {
            null
        } else {
            // No ramp highlight, since the palettes tab asks for focus when it shows one.
            { FluentWorkspaceSample(workspace.value.copy(rampHighlight = null), isDark()) }
        }
        transition.warmUp(sample, awaitIdle)
    }
}

/**
 * The workspace as the first switch to Fluent draws it, the poster, the top bar, the canvas and the
 * dock, without the panels and toasts over them.
 *
 * Every action goes to a dispatcher that drops it and every announcement to one that says nothing,
 * and the focus holders are its own, so nothing here reaches the live workspace. The caller hands it
 * a state with no ramp highlight, because the palettes tab moves focus to a highlighted ramp.
 */
@Composable
private fun FluentWorkspaceSample(
    state: WorkspaceModel.State,
    isDark: Boolean,
) {
    val dispatcher = rememberDispatcher<WorkspaceAction> { }
    val result = LocalThemeResult.current
    FluentWarmUpTheme(result, isDark) {
        CompositionLocalProvider(LocalAnnouncer provides SilentAnnouncer) {
            val focus = rememberTopBarFocus()
            val posterFocus = remember { PosterFocus() }
            val sheetState = rememberBottomSheetState()
            WorkspaceShell(
                posterColors = result.poster,
                posterCollapsed = state.posterCollapsed(LocalLayout.current.posterMode),
                poster = { rail ->
                    CompositionLocalProvider(LocalPosterSheetState provides sheetState) {
                        ProvideImageSeeds(state) { PosterPanel(state, rail, dispatcher, focus = posterFocus) }
                    }
                },
                topBar = { TopBarContent(state, dispatcher, focus = focus) },
                canvas = { contentPadding -> CanvasArea(state, contentPadding, dispatcher) },
                dock = { CanvasDock(state, dispatcher) },
                sheetState = sheetState,
                fullscreen = state.fullscreen,
            )
        }
    }
}
