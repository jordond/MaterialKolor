package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@Composable
internal expect fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)

internal expect fun dialogProperties(scrim: Color): DialogProperties
