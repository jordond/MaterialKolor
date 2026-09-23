package com.materialkolor.builder.di

import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionColors
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * The saved projects, the preferences and the session that edits the open project.
 */
@BindingContainer
@ContributesTo(AppScope::class)
internal object SessionBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideProjectRepository(
        stores: StoreFactory,
        environment: Environment,
    ): ProjectRepository =
        ProjectRepository(
            stores = stores,
            tabId = environment.tabId,
            now = ::epochMillis,
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
    ): ProjectSession =
        ProjectSession(
            projects = projects,
            preferences = preferences,
            environment = environment,
            colorsOf = { document -> resolver.resolve(document).sessionColors() },
            scope = scope,
            now = ::epochMillis,
        )
}

/**
 * The thumbnail quadrants in drawing order, primary, secondary, tertiary and a neutral, all from
 * the light scheme.
 */
private val PREVIEW_ROLES: List<Role> = listOf(Role.Primary, Role.Secondary, Role.Tertiary, Role.SurfaceVariant)

private fun ThemeResult.sessionColors(): SessionColors =
    SessionColors(
        previewColors = PREVIEW_ROLES.map { role -> roles[role, false].argb },
        splashLight = Argb(chrome(isDark = false).surface),
        splashDark = Argb(chrome(isDark = true).surface),
    )

private fun epochMillis(): Long = Clock.System.now().toEpochMilliseconds()
