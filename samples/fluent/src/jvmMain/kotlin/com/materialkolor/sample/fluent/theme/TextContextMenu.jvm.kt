package com.materialkolor.sample.fluent.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun DefaultTextContextMenu(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextContextMenu provides TextContextMenu.Default, content = content)
}
