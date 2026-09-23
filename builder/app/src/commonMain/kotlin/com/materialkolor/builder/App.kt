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
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.workspace.AppModel
import com.materialkolor.builder.feature.workspace.WorkspaceScreen
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.transition.SkinTransitionHost
import com.materialkolor.builder.kit.transition.rememberSkinTransition
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The builder, on whatever platform [platform] describes.
 *
 * This is the one public entry point. The web shell and the desktop window both call it and hand
 * over their own services.
 */
@Composable
fun BuilderApp(platform: PlatformServices) {
    val graph = remember(platform) { createGraphFactory<AppGraph.Factory>().create(platform) }
    CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory) {
        BuilderRoot(graph)
    }
}

/**
 * The theme result of the frame being drawn, resolved once at the root for everything below.
 */
internal val LocalThemeResult: ProvidableCompositionLocal<ThemeResult> = compositionLocalOf {
    error("No ThemeResult provided")
}

/**
 * The one theme result per frame, derived from the session's document as its own target sees it
 * (D35). A setting the target turns off never reaches the chrome or the preview.
 */
@Composable
internal fun rememberThemeResult(
    session: ProjectSession,
    resolver: ThemeResolver,
): State<ThemeResult> {
    val document = session.document.collectAsState()
    return remember(session, resolver) {
        derivedStateOf {
            val stored = document.value
            resolver.resolve(stored.forTarget(ExportTarget.of(stored.library, stored.expressive)))
        }
    }
}

/**
 * Dresses the workspace in the skin and colors of the open project.
 *
 * The workspace is movable content, so a library switch, which swaps the skin theme it sits in,
 * carries its state across instead of starting it over. The reveal that is playing, the toasts and
 * every scroll position survive the switch that way (F-03).
 */
@Composable
private fun BuilderRoot(
    graph: AppGraph,
    model: AppModel = metroViewModel(),
) {
    val state by model.collectAsState()
    val result by rememberThemeResult(graph.session, graph.themeResolver)
    val environment = graph.environment
    val workspace = remember {
        movableContentOf { coarsePointer: Boolean ->
            ProvideBuilderLayout(coarsePointer = coarsePointer, modifier = Modifier.fillMaxSize()) {
                val transition = rememberSkinTransition()
                SkinTransitionHost(transition = transition, modifier = Modifier.fillMaxSize()) {
                    WorkspaceScreen(transition = transition)
                }
            }
        }
    }

    LaunchedEffect(model) {
        model.boot()
        withFrameNanos {}
        environment.hideSplash()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { model.flush() }

    CompositionLocalProvider(LocalThemeResult provides result) {
        BuilderTheme(skin = state.skin, result = result, isDark = state.isDark, reducedMotion = state.reducedMotion) {
            ThemeColorEffect(environment)
            workspace(state.coarsePointer)
        }
    }
}

/** Tints the browser's own chrome with the surface the shell stands on (F-04). */
@Composable
private fun ThemeColorEffect(environment: Environment) {
    val surface = Argb(LocalBuilderTokens.current.panel.toArgb())
    LaunchedEffect(environment, surface) { environment.setThemeColor(surface) }
}
