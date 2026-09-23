package com.materialkolor.builder.di

import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.engine.resolve.ThemeResolver
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * The one graph.
 *
 * Platform variance comes in through [Factory.create], so the graph never sees a web or desktop
 * class. Each property of [PlatformServices] is a binding of its own.
 */
@DependencyGraph(AppScope::class)
internal interface AppGraph : ViewModelGraph {
    /** The resolver the root theme reads on the UI thread. */
    val themeResolver: ThemeResolver

    /** Dark mode and reduced motion for the root, straight from the platform. */
    val environment: Environment

    // b-216

    /** The open project, for the root's theme result. */
    val session: ProjectSession

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes platform: PlatformServices,
        ): AppGraph
    }
}
