package com.materialkolor.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.reducedBuilderMotion
import com.materialkolor.builder.kit.motion.tweenBuilderMotion
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.token.rememberBuilderType
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The builder, on whatever platform [platform] describes.
 *
 * This is the one public entry point. The web shell and the desktop window both call it and hand
 * over their own services.
 */
@Composable
fun BuilderApp(platform: PlatformServices) {
    val graph = remember(platform) { createGraphFactory<AppGraph.Factory>().create(platform) }
    val reducedMotion by graph.environment.reducedMotion.collectAsState()
    val prefersDark by graph.environment.prefersDark.collectAsState()
    val motion = remember(reducedMotion) { if (reducedMotion) reducedBuilderMotion() else tweenBuilderMotion() }

    CompositionLocalProvider(
        LocalMetroViewModelFactory provides graph.metroViewModelFactory,
        LocalReducedMotion provides reducedMotion,
        LocalBuilderMotion provides motion,
    ) {
        PlaceholderRoot(resolver = graph.themeResolver, isDark = prefersDark)
    }
}

/**
 * Stands in for the workspace until the real shell lands (B-216). It wears the Material3 skin in
 * the colors of the default seed.
 */
@Composable
private fun PlaceholderRoot(
    resolver: ThemeResolver,
    isDark: Boolean,
    model: PlaceholderModel = metroViewModel(),
) {
    val roles = remember(resolver) { resolver.resolve(ThemeDocument.Default).roles }
    val surface = Color(roles[Role.Surface, isDark].argb.value)
    val onSurface = Color(roles[Role.OnSurface, isDark].argb.value)
    val primary = Color(roles[Role.Primary, isDark].argb.value)
    val type = rememberBuilderType()

    CompositionLocalProvider(
        LocalSkin provides Skin(library = Library.Material3, expressive = false),
        LocalBuilderType provides type,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(surface),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(text = "MaterialKolor Builder", style = type.title.copy(color = primary))
            BasicText(text = model.versionLine, style = type.body.copy(color = onSurface))
        }
    }
}

/**
 * The smallest model there is, here to prove a graph built model survives recomposition (S10).
 * B-216 replaces it with the real app model.
 */
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class PlaceholderModel(
    versions: ExportVersions,
) : ViewModel() {
    val versionLine: String = "Builder ${versions.builder}, MaterialKolor ${versions.materialKolor}"
}
