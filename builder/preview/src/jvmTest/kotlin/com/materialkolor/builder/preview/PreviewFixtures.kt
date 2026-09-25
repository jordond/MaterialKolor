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
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * Every library a pane can use, Material 3 once per flavour.
 */
internal val PaneSkins: List<Skin> =
    Library.entries.map { library -> Skin(library, expressive = false) } + Skin(Library.Material3, expressive = true)

/**
 * A blue document for the preview, far from the chrome's red.
 */
internal val PreviewResult: ThemeResult = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x1E88E5)))

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
 * The builder's chrome in [skin], coloured from the red chrome document and laid out in [layout].
 *
 * The app always measures its workspace and provides the layout, and parts of the preview such as
 * the split handle read it, so the chrome here provides one too. A layout provided around the
 * chrome does not reach inside it, so a test that wants another passes it here.
 */
@Composable
internal fun Chrome(
    skin: Skin = Skin(Library.Material3, expressive = false),
    layout: LayoutInfo = DesktopLayout,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayout provides layout) {
        BuilderTheme(skin, ChromeResult, isDark = false, reducedMotion = true, content = content)
    }
}
