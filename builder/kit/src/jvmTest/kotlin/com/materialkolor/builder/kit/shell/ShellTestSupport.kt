package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin

internal const val ShellPosterTag = "shell-poster"
internal const val ShellCanvasTag = "shell-canvas"
internal const val ShellTopBarTag = "shell-top-bar"
internal const val ShellDockTag = "shell-dock"
internal const val ShellRootTag = "shell-root"

/** The poster's name, as `strings_shell.xml` has it. */
internal const val ShellPosterLabel = "Seed and theme controls"

/** The design reference's seed, a mid tone orange that sits close to the ink floor. */
internal val ShellPosterColors: PosterColors = PosterColors.of(Argb(0xD9653B))

/** Every skin the shell is drawn in, named for the screenshot files. */
internal val ShellSkins: List<Pair<String, Skin>> = listOf(
    "material3" to Skin(Library.Material3, expressive = false),
    "expressive" to Skin(Library.Material3, expressive = true),
    "unstyled" to Skin(Library.Unstyled, expressive = false),
    "custom" to Skin(Library.Custom, expressive = false),
    "fluent" to Skin(Library.Fluent, expressive = false),
)

/** A skin over a resolved document, a measured layout for a mouse or a finger, and frozen motion. */
@Composable
internal fun ShellHarness(
    skin: Skin,
    reducedMotion: Boolean = false,
    coarsePointer: Boolean = false,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        BuilderTheme(skin, result, isDark = false, reducedMotion = reducedMotion) {
            ProvideBuilderLayout(coarsePointer, Modifier.fillMaxSize().testTag(ShellRootTag)) { content() }
        }
    }
}

/** A slot filled edge to edge by a tagged box, so its bounds are the space the shell gave it. */
@Composable
internal fun ShellSlot(tag: String) {
    Box(Modifier.fillMaxSize().testTag(tag))
}

/** Where the node tagged [tag] sits, in dp from the top start of the window. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.shellBounds(tag: String): DpRect =
    onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()
