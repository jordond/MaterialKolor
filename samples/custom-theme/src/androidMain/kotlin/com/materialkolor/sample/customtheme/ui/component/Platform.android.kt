package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@Composable
internal actual fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier,
) = Unit

// Android dims behind a dialog with the window's own scrim, so there is nothing to pass it.
internal actual fun dialogProperties(scrim: Color): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false)
