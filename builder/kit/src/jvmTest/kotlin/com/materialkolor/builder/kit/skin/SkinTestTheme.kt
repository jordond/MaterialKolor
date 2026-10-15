package com.materialkolor.builder.kit.skin

import androidx.compose.runtime.Composable
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.skin.custom.CustomPaneTheme

/**
 * Themes [content] in [skin] the way the builder draws it.
 *
 * Material3 is the chrome's own theme. Custom only ever shows in a Custom preview pane, so it is a
 * `CustomPaneTheme` on the document's slots inside the chrome, the path every pane takes.
 */
@Composable
internal fun SkinTestTheme(
    skin: Skin,
    result: ThemeResult,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    BuilderTheme(skin.expressive, result, isDark, reducedMotion) {
        when (skin.library) {
            SkinLibrary.Material3 -> content()
            SkinLibrary.Custom -> CustomPaneTheme(result.customSlots, isDark, reducedMotion, content)
        }
    }
}
