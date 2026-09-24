package com.materialkolor.builder.feature.projects

import com.materialkolor.builder.di.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlin.time.Clock

/**
 * The clock the drawer tells each project's age by, so a test can hold time still.
 */
@BindingContainer
@ContributesTo(AppScope::class)
internal object ProjectsBindings {
    @Provides
    fun provideClock(): Clock = Clock.System
}
