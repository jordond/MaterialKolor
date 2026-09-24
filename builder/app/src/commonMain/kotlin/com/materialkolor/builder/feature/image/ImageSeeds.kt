package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * What the image seed model holds for the open project, the image on its way and the newest one.
 * The poster reads it here, so the poster itself needs no model and its tests no graph. Nothing
 * provided reads as no image at all.
 */
internal val LocalImageSeeds: ProvidableCompositionLocal<ImageSeedModel.State> =
    compositionLocalOf { ImageSeedModel.State() }

/** Hands [content] what [model] holds for the project [state] shows, through [LocalImageSeeds]. */
@Composable
internal fun ProvideImageSeeds(
    state: WorkspaceModel.State,
    model: ImageSeedModel = metroViewModel(),
    content: @Composable () -> Unit,
) {
    val seeds by model.collectAsState()
    CompositionLocalProvider(LocalImageSeeds provides seeds.forProject(state.projectGeneration), content = content)
}
