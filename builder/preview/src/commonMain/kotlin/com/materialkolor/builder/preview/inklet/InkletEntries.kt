package com.materialkolor.builder.preview.inklet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec

// Inklet ships for jvm and wasmJs but not js, so the real Inklet screens live in inkletMain and the
// js fallback draws an Inklet document as plain Material 3.

/**
 * Inklet's pen settings and boil clock around [content], inside the pane's Material theme.
 *
 * The clock holds still whenever the builder's own motion does, so tests, screenshots, reduced
 * motion and a hidden tab all see one steady frame.
 */
@Composable
internal expect fun InkletPaneTheme(content: @Composable () -> Unit)

/**
 * The Inklet components gallery.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
internal expect fun InkletGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
)

/**
 * The Inklet sample app, the Trips travel app sketched in pen.
 *
 * @param[spec] The pane the app is drawn in.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The width the dock frames the app at.
 * @param[modifier] Applied to the app.
 */
@Composable
internal expect fun InkletAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
)
