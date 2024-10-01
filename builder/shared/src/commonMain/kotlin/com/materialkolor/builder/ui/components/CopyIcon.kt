package com.materialkolor.builder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CopyIcon(
    visble: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visble,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy color code",
        )
    }
}
