package com.materialkolor.builder.kit.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName

/**
 * For tests only. Turns on the web fold for [content] on the JVM, so a test outside the kit can
 * check what its controls read on the web without a browser.
 *
 * Inside it every kit control and every folded name modifier reads the way the web mirror hears it,
 * with the role word, the state and the disabled note in the name, "Dark mode, switch, off" (D37,
 * D40). A value node reads its value in its text, as [foldsValueIntoName] tells it to. The web's
 * keyboard habits stay off.
 *
 * Only the names follow the web. Overlays still open as windows of their own on the JVM, not in the
 * tree the way the web draws them, since this leaves `LocalOverlaysInTree` off.
 *
 * Product code never calls it, and cannot without opting in to [KitTestApi]. The kit turns the fold
 * on by itself where the web mirror is, and a screen that turned it on anywhere else would read its
 * state twice.
 *
 * @param[content] What the test shows.
 */
@KitTestApi
@Composable
public fun ProvideWebFoldsForTest(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFoldsStateIntoName provides true, content = content)
}

/**
 * For tests only. Turns on the web's keyboard habits for [content] on the JVM, so a test outside the
 * kit can check how its screen tabs on the web without a browser.
 *
 * Inside it a scroll area that overflows is a Tab stop unless it opts out, and Material's segmented
 * buttons stay in the order they are written, as [LocalWebKeyboard] tells them. Names read the way
 * the JVM reads them, since the web fold stays off, and [ProvideWebFoldsForTest] turns that on. Overlays
 * still open as windows of their own, since this leaves `LocalOverlaysInTree` off too.
 *
 * Product code never calls it, and cannot without opting in to [KitTestApi]. The kit keeps to the
 * web's keyboard by itself where the web mirror is.
 *
 * @param[content] What the test shows.
 */
@KitTestApi
@Composable
public fun ProvideWebKeyboardForTest(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWebKeyboard provides true, content = content)
}
