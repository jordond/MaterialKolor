package com.materialkolor.builder.feature.about

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/**
 * What About needs beyond the workspace. Everything it changes goes through the workspace's
 * dispatcher, so all it reads here is what a problem report names, the [browser] and the
 * [siteOrigin] its theme link opens on.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class AboutModel(
    environment: Environment,
) : ViewModel() {
    /**
     * The browser and the system it runs on.
     */
    val browser: String = environment.browser

    /**
     * Where the theme link in a report opens.
     */
    val siteOrigin: String = environment.siteOrigin
}
