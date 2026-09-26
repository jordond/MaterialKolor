package com.materialkolor.sample.fluent.theme

import androidx.compose.runtime.Composable

/**
 * Uses the platform's default text context menu for [content].
 *
 * Fluent v0.1.0 provides its own text context menu on desktop, and it crashes on Compose 1.12. Drop this once
 * Fluent ships a release built for Compose 1.12.
 */
@Composable
internal expect fun DefaultTextContextMenu(content: @Composable () -> Unit)
