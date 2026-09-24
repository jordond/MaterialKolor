package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

/** Desktop draws a scrollbar for [state], Android shows none. */
@Composable
internal expect fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)

/**
 * Sizes the dialog by its content, and puts [scrim] behind it where the platform lets the app pick the scrim.
 */
internal expect fun dialogProperties(scrim: Color): DialogProperties
