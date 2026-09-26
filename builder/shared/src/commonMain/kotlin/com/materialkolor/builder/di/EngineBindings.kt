package com.materialkolor.builder.di

import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.engine.resolve.ThemeResolver
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

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
     * The versions an export names, from the build config.
     */
    @Provides
    fun provideExportVersions(): ExportVersions =
        ExportVersions(
            builder = BuildKonfig.BUILDER_VERSION,
            materialKolor = BuildKonfig.MATERIAL_KOLOR_VERSION,
            fluent = BuildKonfig.FLUENT_VERSION,
            composeUnstyled = BuildKonfig.COMPOSE_UNSTYLED_VERSION,
            fluentModuleAvailable = BuildKonfig.FLUENT_MODULE,
        )
}
