package com.materialkolor.builder.kit.headless

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent

// b-315d

/**
 * Hears every key pressed inside an overlay opened below it, on the key's way down and before the
 * overlay's own controls, and says whether it took the key.
 *
 * The page provides it for the shortcuts that stay the page's while a dialog, a panel or a menu is
 * open. The page never hears a key pressed inside an overlay, since the overlay host draws each one
 * beside the page, and on the desktop a dialog is a window of its own. Null, the default, leaves
 * every key to the overlay.
 */
public val LocalOverlayKeys: ProvidableCompositionLocal<((KeyEvent) -> Boolean)?> =
    staticCompositionLocalOf { null }

/**
 * Hands each key to [keys] first, read as the key arrives, so it always reaches the latest one.
 */
internal fun Modifier.overlayKeys(keys: () -> ((KeyEvent) -> Boolean)?): Modifier =
    onPreviewKeyEvent { event -> keys()?.invoke(event) ?: false }
