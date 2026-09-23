package com.materialkolor.builder.preview

import androidx.compose.runtime.Composable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.split.PaneSpec

/** Every library a pane can wear, Material 3 once per flavour. */
internal val PaneSkins: List<Skin> =
    Library.entries.map { library -> Skin(library, expressive = false) } + Skin(Library.Material3, expressive = true)

/** A blue document for the preview, far from the chrome's red. */
internal val PreviewResult: ThemeResult = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x1E88E5)))

/** A red document for the builder's own chrome. */
internal val ChromeResult: ThemeResult = ThemeResolver().resolve(ThemeDocument(seed = Argb(0xFF0000)))

internal val LightSpec: PaneSpec = PaneSpec(PreviewResult, isDark = false, label = "Light")

internal val DarkSpec: PaneSpec = PaneSpec(PreviewResult, isDark = true, label = "Dark")

/** The builder's chrome in [skin], coloured from the red chrome document. */
@Composable
internal fun Chrome(
    skin: Skin = Skin(Library.Material3, expressive = false),
    content: @Composable () -> Unit,
) {
    BuilderTheme(skin, ChromeResult, isDark = false, reducedMotion = true, content = content)
}
