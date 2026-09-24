package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

/**
 * A scrollbar for the page's scroll [state], where the platform has one. Desktop draws it, Android shows none.
 */
@Composable
internal expect fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)

/**
 * Dialog properties that size the dialog by its content and put [scrim] behind it, where the platform lets the app
 * pick the scrim.
 */
internal expect fun dialogProperties(scrim: Color): DialogProperties
