package com.materialkolor.sample.material3.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)
