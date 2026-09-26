package com.materialkolor.builder.feature.picker

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/**
 * What the color picker needs beyond the workspace. Edits go through the workspace's dispatcher,
 * so the only thing here is the [environment] the eyedropper button calls straight from its click.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class PickerModel(
    val environment: Environment,
) : ViewModel()
