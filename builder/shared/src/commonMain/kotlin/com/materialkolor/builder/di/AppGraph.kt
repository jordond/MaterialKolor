package com.materialkolor.builder.di

import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.engine.resolve.ThemeResolver
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock

/**
 * The one graph.
 *
 * Platform variance comes in through [Factory.create], so the graph never sees a web or desktop
 * class. Each property of [PlatformServices] is a binding of its own.
 */
@DependencyGraph(AppScope::class)
internal interface AppGraph : ViewModelGraph {
    /**
     * The resolver the root theme reads on the UI thread.
     */
    val themeResolver: ThemeResolver

    /**
     * Dark mode and reduced motion for the root, straight from the platform.
     */
    val environment: Environment

    /**
     * The open project, for the root's theme result.
     */
    val session: ProjectSession

    @DependencyGraph.Factory
    fun interface Factory {
        /**
         * The graph over [platform].
         *
         * @param[scope] The app scope, for work that outlives any one screen, autosave for example.
         * It runs off the UI thread on the JVM, so nothing launched on it may touch the theme
         * resolver. A test hands in one on its own clock.
         * @param[clock] The time everything saved is stamped with and each project's age is told by,
         * so one test clock holds all of them still.
         */
        fun create(
            @Includes platform: PlatformServices,
            @Provides scope: CoroutineScope,
            @Provides clock: Clock,
        ): AppGraph
    }
}
