package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import dev.stateholder.dispatcher.Dispatcher

/**
 * A window wide enough to put the light and dark columns side by side.
 */
internal const val TABS_WIDE = 1280

/**
 * A phone held upright, a Compact window.
 */
internal const val TABS_PHONE = 390

/**
 * Tall enough for the top of every data tab.
 */
internal const val TABS_HEIGHT = 900

/**
 * [document] resolved the way the app resolves it, for the target it exports to.
 */
internal fun resolvedFor(document: ThemeDocument): ThemeResult =
    ThemeResolver().resolve(document.forTarget(ExportTarget.of(document.library, document.expressive)))

/**
 * Every action a data tab sent, oldest first.
 */
internal class TabActions {
    val sent = mutableListOf<WorkspaceAction>()
    val dispatcher = Dispatcher<WorkspaceAction> { action -> sent += action }
}

/**
 * [content] in [skin] over [result], laid out for the window, with motion frozen.
 */
@Composable
internal fun DataTabTheme(
    result: ThemeResult,
    skin: Skin = Skin(SkinLibrary.Material3, expressive = false),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMotionFrozen provides true,
        LocalThemeResult provides result,
    ) {
        BuilderTheme(
            skin = skin,
            result = result,
            isDark = false,
            reducedMotion = false,
        ) {
            ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
