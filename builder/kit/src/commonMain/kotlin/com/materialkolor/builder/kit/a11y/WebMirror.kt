package com.materialkolor.builder.kit.a11y

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether assistive technology reads the page through the web mirror of CMP 1.12.1 rather than the
 * Compose semantics themselves (D37, D40).
 *
 * The kit works around the mirror in two ways that end at different times, so each has a local of
 * its own that starts from this. The state fold in the accessible name goes once CMP mirrors state.
 * The keyboard habits in [LocalWebKeyboard] stay after that.
 */
internal expect val onWebMirror: Boolean

/**
 * Whether the kit keeps to the web's keyboard and reading order. A scroll area that overflows is a
 * Tab stop unless it opts out, and Material's segmented buttons stay in the order they are written.
 *
 * It starts from [onWebMirror], and it is a local so a test can turn it on for a whole tree on the
 * JVM.
 */
internal val LocalWebKeyboard: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { onWebMirror }
