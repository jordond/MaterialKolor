package com.materialkolor.builder.di

import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionColors
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.shared_theme_name
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * The saved projects, the preferences and the session that edits the open project. The projects and
 * the session tell the time by the graph's [Clock], so a test clock holds both still.
 */
@BindingContainer
@ContributesTo(AppScope::class)
internal object SessionBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideProjectRepository(
        stores: StoreFactory,
        environment: Environment,
        clock: Clock,
    ): ProjectRepository =
        ProjectRepository(
            stores = stores,
            tabId = environment.tabId,
            now = { clock.now().toEpochMilliseconds() },
            newId = { Uuid.random().toString() },
        )

    @Provides
    @SingleIn(AppScope::class)
    fun providePreferencesRepository(
        stores: StoreFactory,
        scope: CoroutineScope,
    ): PreferencesRepository = PreferencesRepository(stores, scope)

    /**
     * The session resolves themes through [resolver] only on the thread that calls it, the UI
     * thread, never on [scope].
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideProjectSession(
        projects: ProjectRepository,
        preferences: PreferencesRepository,
        environment: Environment,
        resolver: ThemeResolver,
        scope: CoroutineScope,
        clock: Clock,
    ): ProjectSession =
        ProjectSession(
            projects = projects,
            preferences = preferences,
            environment = environment,
            // The thumbnail and the splash show the theme as its own target sees it.
            colorsOf = { document ->
                val target = ExportTarget.of(document.library, document.expressive)
                resolver.resolve(document.forTarget(target)).sessionColors(document.seed)
            },
            sharedThemeName = { getString(Res.string.shared_theme_name) },
            scope = scope,
            now = { clock.now().toEpochMilliseconds() },
        )
}

/**
 * The thumbnail quadrants in drawing order, primary, secondary, tertiary and a neutral, all from
 * the light scheme.
 */
private val PREVIEW_ROLES: List<Role> = listOf(Role.Primary, Role.Secondary, Role.Tertiary, Role.SurfaceVariant)

private fun ThemeResult.sessionColors(seed: Argb): SessionColors =
    SessionColors(
        previewColors = PREVIEW_ROLES.map { role -> roles[role, false].argb },
        splashLight = Argb(chrome(isDark = false).surface),
        splashDark = Argb(chrome(isDark = true).surface),
        splashSeed = seed,
    )
