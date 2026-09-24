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
 * Product code never calls it. The kit turns the fold on by itself where the web mirror is, and a
 * screen that turned it on anywhere else would read its state twice.
 *
 * @param[content] What the test shows.
 */
@Composable
public fun ProvideWebFoldsForTest(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFoldsStateIntoName provides true, content = content)
}
