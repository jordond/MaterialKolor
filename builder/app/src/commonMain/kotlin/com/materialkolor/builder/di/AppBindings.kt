package com.materialkolor.builder.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The app wide coroutine scope and the computation dispatcher.
 */
@BindingContainer
@ContributesTo(AppScope::class)
internal object AppBindings {
    /**
     * Work that outlives any one screen, autosave for example.
     *
     * It runs off the UI thread on the JVM, so nothing launched here may touch the theme resolver.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Computation
    fun provideComputationDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
