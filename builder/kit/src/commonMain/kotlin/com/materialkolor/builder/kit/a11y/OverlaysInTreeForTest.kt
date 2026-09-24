package com.materialkolor.builder.kit.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree

// b-406g

/**
 * For tests only. Draws the overlays in [content] inside the page on the JVM, the way the web draws
 * them (D40), so a test outside the kit can tap and key through a menu or a select without a desktop
 * window of its own.
 *
 * Every kit overlay then renders into the `OverlayHost` that `BuilderTheme` puts at the root, so put
 * this around the theme, not inside it. Names still read the way the JVM reads them, and
 * [ProvideWebFoldsForTest] turns the web's names on.
 *
 * Product code never calls it, and cannot without opting in to [KitTestApi]. The kit keeps overlays
 * in the page by itself where the web mirror is.
 *
 * @param[content] What the test shows.
 */
@KitTestApi
@Composable
public fun ProvideOverlaysInTreeForTest(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverlaysInTree provides true, content = content)
}
