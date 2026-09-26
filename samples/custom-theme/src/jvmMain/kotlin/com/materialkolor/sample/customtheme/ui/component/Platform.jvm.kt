package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal actual fun PageScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    val colors = LocalAppColors.current

    VerticalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(state),
        style = ScrollbarStyle(
            minimalHeight = 32.dp,
            thickness = 8.dp,
            shape = CircleShape,
            hoverDurationMillis = 300,
            unhoverColor = colors.textMuted.copy(alpha = 0.35f),
            hoverColor = colors.textMuted.copy(alpha = 0.7f),
        ),
    )
}

internal actual fun dialogProperties(scrim: Color): DialogProperties =
    DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false,
        usePlatformInsets = true,
        useSoftwareKeyboardInset = true,
        scrimColor = scrim,
    )
