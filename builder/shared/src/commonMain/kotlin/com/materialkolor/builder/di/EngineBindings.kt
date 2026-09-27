package com.materialkolor.builder.di

import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.core.platform.LibraryVersionSource
import com.materialkolor.builder.core.versions.liveExportVersions
import com.materialkolor.builder.engine.resolve.ThemeResolver
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Engine and codegen types carry no DI annotations, so they enter the graph here.
 */
@BindingContainer
@ContributesTo(AppScope::class)
internal object EngineBindings {
    /**
     * The one resolver, shared so every screen hits the same caches.
     *
     * It belongs to the thread that first asks for it, which is the UI thread since only
     * composition and the models it creates reach it. Never call it from the app scope.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideThemeResolver(): ThemeResolver = ThemeResolver()

    /**
     * The versions an export names. They start as the build config's and turn live once the site has
     * said what each library has published since, asked once per run. The build config's are the
     * floor, so a live version is never older than what this build was made with.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideExportVersions(
        source: LibraryVersionSource,
        scope: CoroutineScope,
    ): StateFlow<ExportVersions> = scope.liveExportVersions(bakedExportVersions(), source)
}

/**
 * The versions the build config names, baked in from the version catalog.
 */
private fun bakedExportVersions(): ExportVersions =
    ExportVersions(
        builder = BuildKonfig.BUILDER_VERSION,
        materialKolor = BuildKonfig.MATERIAL_KOLOR_VERSION,
        fluent = BuildKonfig.FLUENT_VERSION,
        composeUnstyled = BuildKonfig.COMPOSE_UNSTYLED_VERSION,
        composeMaterial3 = BuildKonfig.COMPOSE_MATERIAL3_VERSION,
        androidxMaterial3 = BuildKonfig.ANDROIDX_MATERIAL3_VERSION,
        fluentModuleAvailable = BuildKonfig.FLUENT_MODULE,
    )
