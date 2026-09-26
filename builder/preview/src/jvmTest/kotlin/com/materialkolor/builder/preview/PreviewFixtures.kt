package com.materialkolor.builder.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * A library a pane can draw in, and whether Material 3 uses its Expressive flavour.
 */
internal data class PaneLibrary(
    val library: Library,
    val expressive: Boolean = false,
)

/**
 * Every library a pane can use, Material 3 once per flavour.
 */
internal val PaneLibraries: List<PaneLibrary> =
    Library.entries.map { library -> PaneLibrary(library) } + PaneLibrary(Library.Material3, expressive = true)

/**
 * This pane drawn in [pane], from the same document otherwise.
 */
internal fun PaneSpec.on(pane: PaneLibrary): PaneSpec = on(pane.library, pane.expressive)

/**
 * Whether the chrome the builder draws around every pane is Expressive, which it is whatever the
 * pane's library.
 */
internal const val ShellExpressive: Boolean = true

/**
 * A blue document for the preview, far from the chrome's red.
 */
internal val PreviewResult: ThemeResult = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x1E88E5)))

/**
 * This pane drawn in [library], from the same document otherwise. A pane draws the library its
 * document targets, whatever the chrome around it is.
 */
internal fun PaneSpec.on(
    library: Library,
    expressive: Boolean = false,
): PaneSpec =
    PaneSpec(
        result = ThemeResolver().resolve(result.document.copy(library = library, expressive = expressive)),
        isDark = isDark,
        label = label,
        filter = filter,
    )

/**
 * A red document for the builder's own chrome.
 */
internal val ChromeResult: ThemeResult = ThemeResolver().resolve(ThemeDocument(seed = Argb(0xFF0000)))

internal val LightSpec: PaneSpec = PaneSpec(PreviewResult, isDark = false, label = "Light")

internal val DarkSpec: PaneSpec = PaneSpec(PreviewResult, isDark = true, label = "Dark")

/**
 * A desktop window with a mouse, the space the chrome sits in unless a test picks another.
 */
internal val DesktopLayout: LayoutInfo = LayoutInfo(1280.dp, 800.dp)

/**
 * The builder's chrome, Expressive when [expressive], coloured from the red chrome document and laid
 * out in [layout].
 *
 * The app always measures its workspace and provides the layout, and parts of the preview such as
 * the split handle read it, so the chrome here provides one too. A layout provided around the
 * chrome does not reach inside it, so a test that wants another passes it here.
 */
@Composable
internal fun Chrome(
    expressive: Boolean = false,
    layout: LayoutInfo = DesktopLayout,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayout provides layout) {
        BuilderTheme(expressive, ChromeResult, isDark = false, reducedMotion = true, content = content)
    }
}
